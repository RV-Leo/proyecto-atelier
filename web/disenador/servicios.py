"""Servicio de generación de propuestas de diseño.

Arquitectura preparada para conectar un modelo real (familia Qwen, p. ej.
Qwen-Image) en el futuro. Por defecto se usa el proveedor de demostración,
que genera propuestas simuladas usando el inventario real del proyecto.
Las propuestas de demostración se identifican claramente en la interfaz.
"""

import random
import re
import base64
from urllib.parse import urlparse
from django.db.models import Q
from catalogo.models import Producto

from django.conf import settings

from catalogo.repositories import CatalogoRepository
from core.placeholder import generar_textura_tela
from .ia_gateway import ClientePuenteIA, PuenteIAError, descargar_imagen_backend, solicitar_backend

SUFIJOS_DESCRIPCION = [
    'con acabados limpios y silueta relajada',
    'con caída fluida y líneas depuradas',
    'de corte contemporáneo con detalles sutiles',
    'con acentos oversize y lenguaje editorial',
]


class ProveedorDiseno:
    """Interfaz común para los proveedores de propuestas."""

    nombre = 'base'

    def generar(self, resumen, opciones):
        raise NotImplementedError


class ProveedorDemo(ProveedorDiseno):
    """Proveedor de demostración: combina la descripción con el inventario real.

    No genera imágenes reales: se trata de una simulación funcional, marcada
    como tal, que prepara el camino para el modelo de IA definitivo.
    """

    nombre = 'demo'

    def __init__(self, esfuerzo=3):
        self.esfuerzo = int(esfuerzo)

    def _materiales_disponibles(self, paleta):
        catalogo = CatalogoRepository()
        materiales = list(catalogo.materiales_disponibles())
        if not materiales:
            return [], False, False
        relacionados = []
        for m in materiales:
            if any(p in m.nombre.lower() or p in m.color.lower() for p in paleta.split(',')):
                relacionados.append(m)
        stock = len(materiales)
        hay_stock_bajo = any(m.stock_bajo for m in materiales)
        hay_agotados = catalogo.existen_materiales_agotados()
        return (relacionados or materiales[:3]), hay_stock_bajo, hay_agotados

    def generar(self, resumen, opciones):
        rng = random.Random(repr({**opciones, 'texto': resumen}))
        paleta = opciones.get('paleta', 'neutros')
        tipo = opciones.get('tipo_prenda', 'prenda')
        estilo = opciones.get('estilo', 'minimalista')

        materiales, hay_stock_bajo, hay_agotados = self._materiales_disponibles(paleta)

        lista_materiales = ', '.join(m.nombre for m in materiales) or 'sin materiales registrados'
        viabilidad = 'OK'
        nota = ''
        if hay_agotados:
            viabilidad = 'con ajustes'
            nota = ' Algunos materiales están agotados; se revisó la disponibilidad real.'
        elif hay_stock_bajo:
            viabilidad = 'con ajustes'
            nota = ' Varios materiales tienen stock bajo; se recomienda reponer antes de producir.'
        elif not materiales:
            viabilidad = 'sin inventario'

        propuestas = []
        for i in range(self.esfuerzo):
            sufijo = rng.choice(SUFIJOS_DESCRIPCION)
            nombre = f'propuesta_{tipo}_{estilo}_{i + 1}'
            imagen = generar_textura_tela(
                f'disenador/{nombre}',
                color_inicio=rng.choice(['#2E4A66', '#8AA1B4', '#5D4037', '#37474F']),
                color_fin=rng.choice(['#102238', '#B8CBD5', '#8D6E63', '#607D8B']),
                semilla=rng.randint(1, 9999),
            )
            propuestas.append({
                'titulo': f'Propuesta {i + 1} · {tipo.title()} {estilo.title()}',
                'resumen': f'{resumen.capitalize().rstrip(".")}. {sufijo.capitalize()}.',
                'paleta': paleta,
                'materiales': lista_materiales,
                'viabilidad': viabilidad,
                'nota': nota,
                'imagen': imagen,
            })
        return {
            'propuestas': propuestas,
            'intro': 'Estas propuestas son simuladas. '
                     'Reflejan tu descripción y el inventario real de ATELIER.',
            'demostracion': True,
        }


class ProveedorQwen(ProveedorDiseno):
    """Integración futura con modelos Qwen (Qwen-Image / Qwen-Image-Edit).

    Aún no está conectada: al seleccionarla devuelve un error claro para no
    presentar resultados simulados como si fueran reales.
    """

    nombre = 'qwen'

    def __init__(self, url_base):
        self.url_base = url_base

    def generar(self, resumen, opciones):
        raise NotImplementedError(
            'El proveedor Qwen-Image aún no está conectado. '
            'Usa el proveedor "demo" mientras se implementa la integración.'
        )


class ProveedorBackend(ProveedorDiseno):
    """Genera imágenes y guarda el resultado del backend de Atelier."""

    nombre = 'backend'

    def generar(self, resumen, opciones):
        tipo = opciones.get('tipo_prenda', 'prenda')
        estilo = opciones.get('estilo', 'minimalista')
        solicitud = {
            'nombre': resumen[:120] or f'{tipo} {estilo}',
            'tipoPrenda': tipo,
            'estilo': estilo,
            'colores': opciones.get('paleta', ''),
            'tela': opciones.get('tela', ''),
            'descripcion': resumen,
        }
        prenda = solicitar_backend('/api/v1/prendas', cuerpo=solicitud)
        prenda_id = prenda.get('id') if isinstance(prenda, dict) else None
        if not prenda_id:
            raise PuenteIAError('El backend no devolvió la prenda solicitada.')

        imagenes = solicitar_backend(
            f'/api/v1/prendas/{prenda_id}/diseno/propuestas',
            cuerpo={'cantidad': 1},
            timeout=getattr(settings, 'AI_BACKEND_IMAGE_TIMEOUT', 240),
        )
        if not isinstance(imagenes, list) or not imagenes:
            raise PuenteIAError('El proveedor de IA no generó imágenes para esta idea.')

        propuestas = []
        for indice, imagen in enumerate(imagenes, start=1):
            url = imagen.get('imagenUrl') if isinstance(imagen, dict) else None
            if not url:
                continue
            contenido = descargar_imagen_backend(url)
            sufijo = re.sub(r'[^a-zA-Z0-9]+', '-', f'{tipo}-{estilo}').strip('-').lower()
            extension = urlparse(url).path.rsplit('.', 1)[-1].lower()
            if extension not in {'png', 'jpg', 'jpeg', 'webp'}:
                extension = 'png'
            propuestas.append({
                'titulo': f'Propuesta {indice} · {tipo.title()} {estilo.title()}',
                'resumen': imagen.get('promptUsado') or resumen,
                'imagen_bytes': contenido,
                'imagen_nombre': f'propuesta-{prenda_id}-{indice}-{sufijo}.{extension}',
                'materiales': 'Por verificar en el inventario',
                'viabilidad': 'Pendiente de consulta al inventario',
            })
        if not propuestas:
            raise PuenteIAError('El backend respondió, pero no devolvió imágenes utilizables.')
        return {
            'propuestas': propuestas,
            'intro': 'Imágenes generadas por IA y guardadas en ATELIER.',
            'demostracion': False,
        }

    def editar(self, propuesta, instruccion):
        if not propuesta.imagen:
            raise PuenteIAError('No se encontró una imagen anterior para editar.')
        propuesta.imagen.open('rb')
        try:
            contenido = propuesta.imagen.read()
        finally:
            propuesta.imagen.close()
        encoded = base64.b64encode(contenido).decode('ascii')
        nombre = propuesta.imagen.name.lower()
        tipo = ('image/jpeg' if nombre.endswith(('.jpg', '.jpeg')) else
                'image/webp' if nombre.endswith('.webp') else 'image/png')
        bytes_imagen, ruta = ClientePuenteIA().editar_imagen(instruccion, encoded, tipo)
        extension = ruta.rsplit('.', 1)[-1].split('?', 1)[0]
        if extension not in {'png', 'jpg', 'jpeg', 'webp'}:
            extension = 'png'
        return bytes_imagen, f'edicion-{propuesta.pk}.{extension}'


def obtener_proveedor():
    """Devuelve el proveedor configurado en settings.AI_PROVEEDOR."""
    nombre = getattr(settings, 'AI_PROVEEDOR', 'backend')
    if nombre in {'backend', 'qwen'}:
        return ProveedorBackend()
    return ProveedorDemo()


def contexto_catalogo(texto, request=None):
    """Busca productos públicos del catálogo y forma contexto verificable para la IA."""
    ignorar = {'que', 'qué', 'como', 'cómo', 'hay', 'tienen', 'tiene', 'quiero', 'ver',
               'para', 'con', 'por', 'una', 'uno', 'los', 'las', 'del', 'de', 'un', 'en',
               'me', 'puedes', 'puedo', 'recomiendas', 'recomendar', 'dime', 'hola', 'favor'}
    palabras = [p for p in re.findall(r'[\wáéíóúñ]+', texto.lower())
                if len(p) > 2 and p not in ignorar]
    consulta = Producto.objects.all()
    no_hubo_coincidencia = False
    if palabras:
        filtro = Q()
        for palabra in palabras[:8]:
            filtro |= Q(nombre__icontains=palabra) | Q(descripcion__icontains=palabra) | Q(
                composicion__icontains=palabra
            ) | Q(color__icontains=palabra) | Q(tipo__icontains=palabra)
        productos = list(consulta.filter(filtro).distinct()[:5])
        no_hubo_coincidencia = not productos
    else:
        productos = list(consulta.order_by('-es_destacado', '-actualizado')[:5])
    if no_hubo_coincidencia:
        return ('No se encontraron productos que coincidan con la búsqueda de la persona en el catálogo actual. '
                'Dile con claridad que no aparece un resultado para esa consulta y pregúntale si quiere probar '
                'con otro tipo, color o material. No sugieras nombres, precios ni stock que no estén confirmados.')
    lineas = []
    for p in productos:
        disponibilidad = 'disponible' if p.disponible else 'agotado'
        enlace = request.build_absolute_uri(p.get_absolute_url()) if request else p.get_absolute_url()
        lineas.append(
            f'- {p.nombre} ({p.get_tipo_display()}): S/ {p.precio}; {disponibilidad}; '
            f'stock {p.unidades}; color {p.color or "no indicado"}; '
            f'composición {p.composicion or "no indicada"}; {p.descripcion[:220]}; enlace {enlace}'
        )
    return ('Catálogo real de ATELIER (usa solo estos datos para responder sobre productos, '
            'precios y existencias; si no aparece el artículo, dilo claramente):\n' + '\n'.join(lineas))
