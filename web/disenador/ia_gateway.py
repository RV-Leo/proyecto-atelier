"""Cliente HTTP del puente de conversación con IA de Spring Boot."""

import base64
import json
from dataclasses import dataclass
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen
from urllib.parse import urljoin, urlparse

from django.conf import settings


class PuenteIAError(Exception):
    """Error legible al comunicarse con el backend o el proveedor de IA."""


@dataclass(frozen=True)
class RespuestaChat:
    """Texto del asistente y rutas de las imágenes que generó en el turno."""

    texto: str
    imagenes: tuple[str, ...] = ()


def solicitar_backend(ruta, metodo='POST', cuerpo=None, bytes_respuesta=False, timeout=None):
    """Llama a una ruta del backend Atelier con la autenticación local configurada."""
    url_base = settings.AI_BACKEND_URL.rstrip('/')
    if not url_base:
        raise PuenteIAError('Configura ATELIER_BACKEND_URL para conectar Atelier IA.')

    datos = json.dumps(cuerpo).encode('utf-8') if cuerpo is not None else None
    cabeceras = {'Accept': 'image/*,*/*' if bytes_respuesta else 'application/json'}
    if datos is not None:
        cabeceras['Content-Type'] = 'application/json'
    usuario = settings.AI_BACKEND_USER
    clave = settings.AI_BACKEND_PASSWORD
    if usuario and clave:
        token = base64.b64encode(f'{usuario}:{clave}'.encode('utf-8')).decode('ascii')
        cabeceras['Authorization'] = f'Basic {token}'

    solicitud = Request(
        urljoin(url_base + '/', ruta.lstrip('/')),
        data=datos,
        headers=cabeceras,
        method=metodo,
    )
    try:
        with urlopen(solicitud, timeout=timeout or settings.AI_BACKEND_TIMEOUT) as respuesta_http:
            contenido = respuesta_http.read()
    except HTTPError as error:
        try:
            detalle = json.loads(error.read().decode('utf-8'))
        except (UnicodeDecodeError, json.JSONDecodeError):
            detalle = None
        mensaje = detalle.get('mensaje') if isinstance(detalle, dict) else None
        if isinstance(mensaje, str) and mensaje.strip():
            raise PuenteIAError(mensaje.strip()) from error
        if error.code in (401, 403):
            raise PuenteIAError(
                'El backend rechazó el acceso. Revisa las credenciales de la API.'
            ) from error
        if error.code == 503:
            raise PuenteIAError(
                'El backend está activo, pero falta configurar el proveedor de IA o no está disponible.'
            ) from error
        raise PuenteIAError(
            f'El backend respondió con el estado {error.code} al procesar la solicitud.'
        ) from error
    except (URLError, TimeoutError) as error:
        raise PuenteIAError(
            'No se pudo conectar con el backend de IA en '
            f'{settings.AI_BACKEND_URL}. Inícialo y revisa su configuración.'
        ) from error

    if bytes_respuesta:
        return contenido
    try:
        return json.loads(contenido.decode('utf-8'))
    except (UnicodeDecodeError, json.JSONDecodeError) as error:
        raise PuenteIAError('El backend de IA devolvió una respuesta no válida.') from error


def descargar_imagen_backend(ruta):
    """Descarga una imagen del mismo backend que devolvió su ruta."""
    url_base = settings.AI_BACKEND_URL.rstrip('/')
    url = urljoin(url_base + '/', ruta.lstrip('/'))
    origen_base = urlparse(url_base)
    origen_imagen = urlparse(url)
    if (origen_base.scheme, origen_base.netloc) != (origen_imagen.scheme, origen_imagen.netloc):
        raise PuenteIAError('El backend devolvió una dirección de imagen no válida.')
    return solicitar_backend(url, metodo='GET', bytes_respuesta=True)


class ClientePuenteIA:
    """Envía el historial del chat al endpoint común de Atelier."""

    def conversar(self, mensajes, contexto=''):
        respuesta = solicitar_backend(
            '/api/v1/ia/chat', cuerpo={'messages': mensajes, 'contexto': contexto}
        )
        texto = respuesta.get('respuesta') if isinstance(respuesta, dict) else None
        if not isinstance(texto, str) or not texto.strip():
            raise PuenteIAError('El backend de IA no devolvió una respuesta de texto.')
        rutas = respuesta.get('imagenes') if isinstance(respuesta, dict) else None
        imagenes = tuple(
            ruta.strip() for ruta in rutas
            if isinstance(ruta, str) and ruta.strip()
        ) if isinstance(rutas, list) else ()
        return RespuestaChat(texto.strip(), imagenes)

    def editar_imagen(self, prompt, imagen_base64, content_type='image/png'):
        respuesta = solicitar_backend(
            '/api/v1/ia/images/edits',
            cuerpo={'prompt': prompt, 'imageBase64': imagen_base64, 'contentType': content_type},
            timeout=getattr(settings, 'AI_BACKEND_IMAGE_TIMEOUT', 240),
        )
        ruta = respuesta.get('imagenUrl') if isinstance(respuesta, dict) else None
        if not ruta:
            raise PuenteIAError('El backend no devolvió la imagen editada.')
        return descargar_imagen_backend(ruta), ruta
