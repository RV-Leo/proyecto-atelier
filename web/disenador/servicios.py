"""Servicio de generación de propuestas de diseño.

Arquitectura preparada para conectar un modelo real (familia Qwen, p. ej.
Qwen-Image) en el futuro. Por defecto se usa el proveedor de demostración,
que genera propuestas simuladas usando el inventario real del proyecto.
Las propuestas de demostración se identifican claramente en la interfaz.
"""

import random

import requests
from django.conf import settings
from django.core.files.base import ContentFile

from catalogo.models import Material
from core.placeholder import generar_textura_tela

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
        materiales = list(Material.objects.filter(cantidad__gt=0))
        if not materiales:
            return [], False, False
        relacionados = []
        for m in materiales:
            if any(p in m.nombre.lower() or p in m.color.lower() for p in paleta.split(',')):
                relacionados.append(m)
        stock = len(materiales)
        hay_stock_bajo = any(m.stock_bajo for m in materiales)
        hay_agotados = Material.objects.filter(cantidad__lte=0).count() > 0
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
    """Proveedor real: crea la prenda en el backend y genera imágenes con la IA.

    Flujo:
      1. POST /api/v1/prendas      → crea la prenda y devuelve su id
      2. POST /prendas/{id}/diseno/propuestas → genera imágenes (Pollinations)
      3. Descarga cada imagen y la devuelve como bytes para guardarla en disco.

    Nunca envía la clave de Google al proveedor de imágenes: el backend ya se
    encarga de separar texto (Gemini) de imágenes (Pollinations).
    """

    nombre = 'backend'

    def __init__(self, url_base=None, usuario=None, contrasena=None, timeout=None):
        self.url_base = (url_base or getattr(settings, 'API_BASE_URL', 'http://localhost:8080/api/v1')).rstrip('/')
        # Las imágenes se sirven bajo /imagenes/ (fuera del prefijo /api/v1).
        self.host_base = self.url_base[: -len('/api/v1')] if self.url_base.endswith('/api/v1') else self.url_base
        self.usuario = usuario or getattr(settings, 'API_USER', 'atelier')
        self.contrasena = contrasena or getattr(settings, 'API_PASSWORD', 'atelier-dev')
        self.timeout = int(timeout or getattr(settings, 'API_TIMEOUT', 30))

    def _crear_prenda(self, resumen, opciones):
        payload = {
            "nombre": opciones.get("nombre", resumen[:120] or "Diseño sin nombre"),
            "tipoPrenda": opciones.get("tipo_prenda", "prenda"),
            "estilo": opciones.get("estilo", "minimalista"),
            "colores": opciones.get("paleta", ""),
            "tela": "",
            "descripcion": resumen,
        }
        resp = requests.post(
            f"{self.url_base}/prendas",
            json=payload,
            auth=(self.usuario, self.contrasena),
            timeout=self.timeout,
        )
        resp.raise_for_status()
        return resp.json()["id"]

    def _generar_propuestas(self, prenda_id, cantidad):
        resp = requests.post(
            f"{self.url_base}/prendas/{prenda_id}/diseno/propuestas",
            json={"cantidad": cantidad},
            auth=(self.usuario, self.contrasena),
            timeout=self.timeout,
        )
        resp.raise_for_status()
        return resp.json()

    def _descargar_imagen(self, imagen_url):
        resp = requests.get(
            f"{self.host_base}{imagen_url}",
            auth=(self.usuario, self.contrasena),
            timeout=self.timeout,
        )
        resp.raise_for_status()
        return resp.content

    def generar(self, resumen, opciones):
        esfuerzo = int(opciones.get("esfuerzo", 3))
        prenda_id = self._crear_prenda(resumen, opciones)
        datos = self._generar_propuestas(prenda_id, esfuerzo)

        propuestas = []
        for i, item in enumerate(datos):
            nombre_archivo = f"propuesta_{prenda_id}_{i + 1}.jpg"
            imagen_bytes = self._descargar_imagen(item["imagenUrl"])
            propuestas.append({
                "titulo": f"Propuesta {i + 1} · {opciones.get('tipo_prenda', 'prenda').title()} {opciones.get('estilo', '').title()}",
                "resumen": item.get("promptUsado", ""),
                "imagen_bytes": imagen_bytes,
                "imagen_nombre": nombre_archivo,
                "imagen_url_backend": item["imagenUrl"],
            })

        return {
            "propuestas": propuestas,
            "intro": "Imágenes generadas por la IA del backend (Pollinations, gratuito).",
            "demostracion": False,
        }


def obtener_proveedor():
    """Devuelve el proveedor configurado en settings.AI_PROVEEDOR."""
    nombre = getattr(settings, 'AI_PROVEEDOR', 'demo')
    if nombre == 'qwen':
        return ProveedorQwen(getattr(settings, 'AI_URL_BASE', ''))
    if nombre == 'backend':
        return ProveedorBackend()
    return ProveedorDemo()