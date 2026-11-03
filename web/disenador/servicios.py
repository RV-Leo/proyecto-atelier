"""Proveedores de propuestas de diseño y conexión al puente de IA de Atelier."""

import base64
import hashlib
import json
import random
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen

from django.conf import settings

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


class ProveedorGateway(ProveedorDiseno):
    """Solicita texto al backend, que mantiene las credenciales del proveedor."""

    nombre = 'backend'

    def conversar(self, mensajes):
        return self._consultar_backend(mensajes)

    def generar(self, resumen, opciones):
        tipo = opciones.get('tipo_prenda', 'prenda')
        estilo = opciones.get('estilo', 'minimalista')
        temporada = opciones.get('temporada', 'sin especificar')
        calidad = opciones.get('calidad', 'sin especificar')
        paleta = opciones.get('paleta', 'neutros')
        instruccion = (
            'Propón un diseño textil en español, en un párrafo conciso. '
            'Describe la silueta, los detalles y cómo aplicar la paleta. '
            'No afirmes que hay materiales disponibles ni que la prenda es viable; '
            'el inventario se verifica por separado.\n\n'
            f'Descripción: {resumen}\n'
            f'Tipo: {tipo}\nEstilo: {estilo}\nTemporada: {temporada}\n'
            f'Nivel de calidad: {calidad}\nPaleta: {paleta}'
        )
        texto = self._consultar_backend([{'role': 'user', 'content': instruccion}])

        id_textura = hashlib.sha256(instruccion.encode('utf-8')).hexdigest()[:12]
        nombre = f'propuesta_{tipo}_{estilo}_{id_textura}'
        textura = generar_textura_tela(
            f'disenador/{nombre}',
            color_inicio='#8AA1B4',
            color_fin='#37474F',
            semilla=sum(map(ord, instruccion)) % 9999 or 1,
        )
        return {
            'propuestas': [{
                'titulo': f'{tipo.title()} {estilo.title()} · propuesta IA',
                'resumen': texto,
                'paleta': paleta,
                'materiales': 'Por verificar en el inventario',
                'viabilidad': 'Pendiente de consulta al inventario',
                'nota': 'La imagen es una textura ilustrativa, no fue generada por IA.',
                'imagen': textura,
            }],
            'intro': (
                'El modelo conectado al backend generó la descripción. '
                'La imagen es ilustrativa y la viabilidad debe verificarse por separado.'
            ),
            'demostracion': True,
        }

    def _consultar_backend(self, mensajes):
        url_base = getattr(settings, 'AI_BACKEND_URL', '').rstrip('/')
        if not url_base:
            raise RuntimeError('Configura ATELIER_BACKEND_URL para conectar el puente de IA.')

        cuerpo = json.dumps({'messages': mensajes}).encode('utf-8')
        solicitud = Request(
            f'{url_base}/api/v1/ia/chat',
            data=cuerpo,
            headers=self._cabeceras(),
            method='POST',
        )
        try:
            with urlopen(solicitud, timeout=settings.AI_BACKEND_TIMEOUT) as respuesta_http:
                respuesta = json.loads(respuesta_http.read().decode('utf-8'))
        except HTTPError as error:
            try:
                cuerpo_error = json.loads(error.read().decode('utf-8'))
            except (UnicodeDecodeError, json.JSONDecodeError):
                cuerpo_error = None
            mensaje = (
                cuerpo_error.get('mensaje')
                if isinstance(cuerpo_error, dict)
                else None
            )
            if isinstance(mensaje, str) and mensaje.strip():
                raise RuntimeError(mensaje.strip()) from error
            raise RuntimeError(
                f'El puente de IA respondió con estado HTTP {error.code}.'
            ) from error
        except (URLError, TimeoutError) as error:
            raise RuntimeError('No se pudo conectar con el puente de IA de Atelier.') from error
        except (UnicodeDecodeError, json.JSONDecodeError) as error:
            raise RuntimeError('El puente de IA devolvió una respuesta inválida.') from error

        texto = respuesta.get('respuesta') if isinstance(respuesta, dict) else None
        if not isinstance(texto, str) or not texto.strip():
            raise RuntimeError('El puente de IA no devolvió una respuesta de texto.')
        return texto.strip()

    @staticmethod
    def _cabeceras():
        cabeceras = {'Content-Type': 'application/json'}
        usuario = getattr(settings, 'AI_BACKEND_USER', '')
        clave = getattr(settings, 'AI_BACKEND_PASSWORD', '')
        if usuario and clave:
            credenciales = base64.b64encode(f'{usuario}:{clave}'.encode('utf-8')).decode('ascii')
            cabeceras['Authorization'] = f'Basic {credenciales}'
        return cabeceras


def obtener_proveedor():
    """Devuelve el proveedor configurado en settings.AI_PROVEEDOR."""
    nombre = getattr(settings, 'AI_PROVEEDOR', 'demo')
    if nombre in {'backend', 'qwen'}:
        return ProveedorGateway()
    return ProveedorDemo()