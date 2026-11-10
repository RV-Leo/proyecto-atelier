"""Cliente HTTP de la API del backend de ATELIER.

La web no guarda datos de negocio: prendas y materiales viven en PostgreSQL y
los expone Spring Boot. Este módulo es el único punto de la web que habla con
esa API.

Las respuestas se convierten a objetos con la misma interfaz que usaban las
plantillas (``producto.categoria``, ``producto.get_absolute_url``,
``material.cantidad``, etc.) para no tener que reescribir todo el HTML.
"""

from __future__ import annotations

from dataclasses import dataclass
from types import SimpleNamespace
from typing import Any

import requests
from django.conf import settings
from django.urls import reverse
from django.utils.text import slugify


class ApiNoDisponible(RuntimeError):
    """La API no respondió o devolvió un error.

    Las vistas la capturan para mostrar un aviso en lugar de un error 500.
    """


# --- Adaptadores ---------------------------------------------------------


@dataclass(frozen=True)
class Prenda:
    """Una prenda de la API con la interfaz que esperan las plantillas."""

    id: int
    nombre: str
    slug: str
    tipo: str
    estilo: str
    colores: str
    tela: str
    descripcion: str
    estado: str

    # Estados del ciclo de vida que devuelve el backend.
    ESTADOS = {
        'BORRADOR': 'Borrador',
        'GENERANDO': 'En generación',
        'GENERADA': 'Generada',
        'ANALIZADA': 'Analizada',
    }

    @property
    def pk(self) -> int:
        return self.id

    @property
    def categoria(self) -> SimpleNamespace:
        """La API no tiene categorías: se usa el tipo de prenda."""
        tipo = (self.tipo or 'prenda').strip()
        return SimpleNamespace(nombre=tipo.capitalize(), slug=slugify(tipo))

    def get_tipo_display(self) -> str:
        return (self.tipo or 'Prenda').capitalize()

    def get_estado_display(self) -> str:
        return self.ESTADOS.get((self.estado or '').upper(), self.estado or '—')

    # Campos que la plantilla muestra y que la API no expone todavía.
    @property
    def color(self) -> str:
        return self.colores

    @property
    def composicion(self) -> str:
        return self.tela

    @property
    def precio(self) -> None:
        return None

    @property
    def unidades(self) -> None:
        return None

    @property
    def imagen(self) -> None:
        return None

    @property
    def estado_visual(self) -> None:
        """Insignia del catálogo (tendencia/nuevo/agotado).

        La API todavía no expone stock ni destacados, así que no se inventa
        una etiqueta.
        """
        return None

    @property
    def temporada(self) -> str:
        return ''

    def get_temporada_display(self) -> str:
        return '—'

    def get_absolute_url(self) -> str:
        return reverse('catalogo:detalle', kwargs={'slug': self.slug})

    @classmethod
    def desde_api(cls, datos: dict[str, Any]) -> 'Prenda':
        return cls(
            id=datos.get('id'),
            nombre=datos.get('nombre') or '',
            slug=datos.get('slug') or '',
            tipo=datos.get('tipoPrenda') or '',
            estilo=datos.get('estilo') or '',
            colores=datos.get('colores') or '',
            tela=datos.get('tela') or '',
            descripcion=datos.get('descripcion') or '',
            estado=datos.get('estado') or '',
        )


@dataclass(frozen=True)
class Material:
    """Un material del inventario con la interfaz que espera la plantilla."""

    id: int
    nombre: str
    tipo: str = ''
    color: str = ''
    unidad: str = ''
    stock: float = 0.0
    stock_minimo: float = 0.0
    estado: str = ''
    proveedor: str = ''
    precio_unitario: float = 0.0
    activo: bool = True
    actualizado: str = ''

    UNIDADES = {
        'METRO': 'Metro',
        'UNIDAD': 'Unidad',
        'KILOGRAMO': 'Kilogramo',
        'ROLLO': 'Rollo',
    }

    @property
    def categoria(self) -> str:
        return self.tipo or 'Sin categoría'

    @property
    def tipo_tela(self) -> str:
        return self.tipo

    @property
    def composicion(self) -> str:
        # La API todavía no expone composición; se muestra el proveedor.
        return ''

    @property
    def cantidad(self) -> float:
        return self.stock

    def get_unidad_medida_display(self) -> str:
        return self.UNIDADES.get((self.unidad or '').upper(), self.unidad or '')

    @property
    def agotado(self) -> bool:
        return (self.estado or '').upper() == 'AGOTADO'

    @property
    def stock_bajo(self) -> bool:
        return (self.estado or '').upper() == 'BAJO'

    @property
    def disponible(self) -> bool:
        return (self.estado or '').upper() == 'DISPONIBLE'

    @classmethod
    def desde_api(cls, datos: dict[str, Any]) -> 'Material':
        return cls(
            id=datos.get('id'),
            nombre=datos.get('nombre') or '',
            tipo=datos.get('tipo') or '',
            color=datos.get('color') or '',
            unidad=datos.get('unidad') or '',
            stock=_numero(datos.get('stock')),
            stock_minimo=_numero(datos.get('stockMinimo')),
            estado=datos.get('estado') or '',
            proveedor=datos.get('proveedor') or '',
            precio_unitario=_numero(datos.get('precioUnitario')),
            activo=bool(datos.get('activo', True)),
            actualizado=datos.get('actualizadoEn') or '',
        )


@dataclass(frozen=True)
class Paginador:
    """Mínimo de ``django.core.paginator.Paginator`` que usan las plantillas."""

    count: int
    num_pages: int

    @property
    def page_range(self) -> range:
        return range(1, self.num_pages + 1)


@dataclass(frozen=True)
class Pagina:
    """Mínimo de ``django.core.paginator.Page`` que usan las plantillas."""

    object_list: list
    number: int
    tamano: int
    paginator: Paginador
    primera: bool
    ultima: bool

    def has_other_pages(self) -> bool:
        return self.paginator.num_pages > 1

    def has_previous(self) -> bool:
        return not self.primera

    def has_next(self) -> bool:
        return not self.ultima

    def previous_page_number(self) -> int:
        return self.number - 1

    def next_page_number(self) -> int:
        return self.number + 1

    @classmethod
    def vacia(cls) -> 'Pagina':
        return cls([], 1, 0, Paginador(0, 1), True, True)


# --- Llamadas a la API ---------------------------------------------------


def _numero(valor: Any) -> float:
    try:
        return float(valor)
    except (TypeError, ValueError):
        return 0.0


def _url(path: str) -> str:
    return '%s/%s' % (settings.API_BASE_URL.rstrip('/'), path.lstrip('/'))


def _get(path: str, params: dict[str, Any] | None = None) -> Any:
    """GET autenticado. Devuelve ``None`` si el recurso no existe (404)."""
    try:
        respuesta = requests.get(
            _url(path),
            params=params,
            auth=(settings.API_USER, settings.API_PASSWORD),
            timeout=settings.API_TIMEOUT,
        )
    except requests.RequestException as exc:
        raise ApiNoDisponible(
            'No se pudo conectar con la API (%s).' % settings.API_BASE_URL
        ) from exc

    if respuesta.status_code == 404:
        return None
    if respuesta.status_code >= 400:
        raise ApiNoDisponible(
            'La API respondió %s al pedir %s.' % (respuesta.status_code, path)
        )

    try:
        return respuesta.json()
    except ValueError as exc:
        raise ApiNoDisponible('La API devolvió una respuesta que no es JSON.') from exc


def _parametros(**filtros: Any) -> dict[str, Any]:
    return {clave: valor for clave, valor in filtros.items() if valor not in (None, '')}


def listar_prendas(
    pagina: int = 1,
    tamano: int = 12,
    q: str | None = None,
    tipo: str | None = None,
    estilo: str | None = None,
    estado: str | None = None,
) -> Pagina:
    """Lista paginada de prendas, con los filtros que acepta la API."""
    datos = _get(
        'prendas',
        _parametros(pagina=pagina, tamano=tamano, q=q, tipo=tipo, estilo=estilo, estado=estado),
    ) or {'items': []}

    items = [Prenda.desde_api(item) for item in datos.get('items', [])]
    return Pagina(
        object_list=items,
        number=int(datos.get('pagina', pagina)),
        tamano=int(datos.get('tamano', tamano)),
        paginator=Paginador(
            count=int(datos.get('total', len(items))),
            num_pages=int(datos.get('totalPaginas', 1)),
        ),
        primera=bool(datos.get('primera', True)),
        ultima=bool(datos.get('ultima', True)),
    )


def obtener_por_slug(slug: str) -> Prenda | None:
    """Detalle de una prenda. ``None`` si el slug no existe (404)."""
    datos = _get('prendas/slug/%s' % slug)
    return Prenda.desde_api(datos) if datos else None


def opciones_catalogo() -> tuple[list[str], list[str]]:
    """Tipos y estilos distintos para los filtros del catálogo."""
    pagina = listar_prendas(pagina=1, tamano=100)
    tipos = sorted({p.tipo for p in pagina.object_list if p.tipo})
    estilos = sorted({p.estilo for p in pagina.object_list if p.estilo})
    return tipos, estilos


def total_prendas() -> int:
    """Total de prendas, para los informes."""
    return listar_prendas(tamano=1).paginator.count


def listar_materiales(q: str | None = None, estado: str | None = None) -> list[Material]:
    """Inventario completo (la API devuelve todo si no se pagina)."""
    estados = {'disponible': 'DISPONIBLE', 'bajo': 'BAJO', 'agotado': 'AGOTADO'}
    estado_api = estados.get((estado or '').strip().lower(), estado)
    datos = _get('materiales', _parametros(q=q, estado=estado_api)) or {'items': []}
    return [Material.desde_api(item) for item in datos.get('items', [])]
