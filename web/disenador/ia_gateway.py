"""Cliente HTTP del puente de conversación con IA de Spring Boot."""

import base64
import json
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen

from django.conf import settings


class PuenteIAError(Exception):
    """Error legible al comunicarse con el backend o el proveedor de IA."""


class ClientePuenteIA:
    """Envía el historial del chat al endpoint común de Atelier."""

    def conversar(self, mensajes):
        url_base = settings.AI_BACKEND_URL.rstrip('/')
        if not url_base:
            raise PuenteIAError('Configura ATELIER_BACKEND_URL para conectar el chat.')

        cuerpo = json.dumps({'messages': mensajes}).encode('utf-8')
        cabeceras = {'Content-Type': 'application/json', 'Accept': 'application/json'}
        usuario = settings.AI_BACKEND_USER
        clave = settings.AI_BACKEND_PASSWORD
        if usuario and clave:
            token = base64.b64encode(f'{usuario}:{clave}'.encode('utf-8')).decode('ascii')
            cabeceras['Authorization'] = f'Basic {token}'

        solicitud = Request(
            f'{url_base}/api/v1/ia/chat',
            data=cuerpo,
            headers=cabeceras,
            method='POST',
        )
        try:
            with urlopen(solicitud, timeout=settings.AI_BACKEND_TIMEOUT) as respuesta_http:
                respuesta = json.loads(respuesta_http.read().decode('utf-8'))
        except HTTPError as error:
            if error.code in (401, 403):
                raise PuenteIAError(
                    'El backend rechazó el acceso. Revisa las credenciales de la API.'
                ) from error
            if error.code == 503:
                raise PuenteIAError(
                    'El backend está activo, pero el proveedor de IA no está configurado o disponible.'
                ) from error
            raise PuenteIAError(
                f'El backend respondió con el estado {error.code} al procesar el mensaje.'
            ) from error
        except (URLError, TimeoutError) as error:
            raise PuenteIAError(
                'No se pudo conectar con el backend de IA en '
                f'{settings.AI_BACKEND_URL}. Inícialo y revisa su configuración.'
            ) from error
        except (UnicodeDecodeError, json.JSONDecodeError) as error:
            raise PuenteIAError('El backend de IA devolvió una respuesta no válida.') from error

        texto = respuesta.get('respuesta') if isinstance(respuesta, dict) else None
        if not isinstance(texto, str) or not texto.strip():
            raise PuenteIAError('El backend de IA no devolvió una respuesta de texto.')
        return texto.strip()
