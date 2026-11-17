"""Casos de uso del asistente de diseño."""

import logging
from urllib.parse import urlparse
from uuid import uuid4

from django.core.files.base import ContentFile
from .ia_gateway import ClientePuenteIA, PuenteIAError, descargar_imagen_backend
from .models import Mensaje
from .repositories import DisenadorRepository
from .servicios import ProveedorDemo, obtener_proveedor
from .servicios import contexto_catalogo

log = logging.getLogger(__name__)


class DisenoService:
    """Orquesta generación, persistencia y consulta de propuestas."""

    def __init__(self, repository=None, proveedor=None, puente_ia=None):
        self.repository = repository or DisenadorRepository()
        self.proveedor = proveedor or obtener_proveedor
        self.puente_ia = puente_ia or ClientePuenteIA()

    def conversar(self, usuario, texto, conversacion_id=None):
        """Envía el turno al puente de IA y guarda la conversación del usuario."""
        conversacion = (
            self.repository.conversacion_usuario(usuario, conversacion_id)
            if conversacion_id else None
        )
        historial = []
        if conversacion:
            historial = [
                {
                    'role': 'user' if mensaje.rol == Mensaje.Rol.USUARIO else 'assistant',
                    'content': mensaje.texto[:4000],
                }
                for mensaje in self.repository.mensajes_recientes(conversacion)
            ]

        mensajes = (historial + [{'role': 'user', 'content': texto}])[-20:]
        respuesta = self.puente_ia.conversar(mensajes)

        if not conversacion:
            titulo = texto[:60] + ('…' if len(texto) > 60 else '')
            conversacion = self.repository.crear_conversacion(usuario, titulo)
        self.repository.crear_mensaje(conversacion, Mensaje.Rol.USUARIO, texto)
        self.repository.crear_mensaje(conversacion, Mensaje.Rol.ASISTENTE, respuesta.texto)
        self._guardar_imagenes_chat(usuario, conversacion, texto, respuesta)
        return conversacion

    def procesar_turno(self, usuario, texto, request, conversacion_id=None):
        """Resuelve consultas de catálogo, creación y edición desde un solo chat."""
        conversacion = (
            self.repository.conversacion_usuario(usuario, conversacion_id)
            if conversacion_id else None
        )
        if not conversacion:
            conversacion = self.repository.crear_conversacion(usuario, texto[:60])

        historial = []
        for mensaje in self.repository.mensajes_recientes(conversacion):
            historial.append({
                'role': 'user' if mensaje.rol == Mensaje.Rol.USUARIO else 'assistant',
                'content': mensaje.texto[:4000],
            })
        mensajes = (historial + [{'role': 'user', 'content': texto}])[-20:]
        propuesta = conversacion.propuestas.order_by('-creada').first()
        lower = texto.lower()
        quiere_editar = bool(propuesta and any(k in lower for k in (
            'cambia', 'cambiar', 'cámbia', 'cambialo', 'modifica', 'modificar', 'edita', 'editar',
            'ajusta', 'ajustar', 'hazlo', 'ponle', 'ponlo', 'quítale', 'quitale', 'añade', 'anade',
            'más ', 'mas ', 'quiero que', 'haz que', 'que sea', 'que tenga', 'usa ', 'en color', 'menos ',
        )))
        quiere_crear = any(k in lower for k in (
            'diseña', 'disena', 'diseñar', 'disenar', 'genera', 'generar', 'crea una imagen',
            'crear una imagen', 'crea un diseño', 'crear un diseño', 'crea un ', 'crea una ',
            'diseño de', 'diseño:',
            'imagina una prenda', 'quiero un diseño', 'quiero diseñar', 'quiero disenar',
            'hazme un diseño', 'hazme un diseno', 'hazme una imagen',
        ))
        self.repository.crear_mensaje(conversacion, Mensaje.Rol.USUARIO, texto)

        if quiere_editar:
            proveedor = self.proveedor() if callable(self.proveedor) else self.proveedor
            if not hasattr(proveedor, 'editar'):
                raise PuenteIAError('La edición de imágenes requiere el proveedor de IA conectado.')
            bytes_imagen, nombre = proveedor.editar(propuesta, texto)
            nueva = self.repository.crear_propuesta(
                usuario=usuario, conversacion=conversacion, prompt=texto,
                tipo_prenda=propuesta.tipo_prenda, estilo=propuesta.estilo,
                temporada=propuesta.temporada, calidad=propuesta.calidad,
                paleta=propuesta.paleta, resultado_resumen=f'Edición: {texto}',
                imagen=ContentFile(bytes_imagen, name=nombre), es_favorita=False,
                es_demo=False,
            )
            self.repository.crear_mensaje(
                conversacion, Mensaje.Rol.ASISTENTE,
                'He actualizado el diseño con el cambio que pediste. La nueva versión aparece aquí; la anterior queda guardada en la conversación.',
            )
            return conversacion

        if quiere_crear:
            proveedor = self.proveedor() if callable(self.proveedor) else self.proveedor
            if getattr(proveedor, 'nombre', '') == 'backend':
                resultado = proveedor.generar(texto, {
                    'tipo_prenda': 'diseño personalizado', 'estilo': 'según tu descripción',
                    'temporada': '', 'calidad': 'alta', 'paleta': '',
                })
                self._guardar_propuestas(usuario, conversacion, texto, resultado)
                self.repository.crear_mensaje(
                    conversacion, Mensaje.Rol.ASISTENTE,
                    'Generé una imagen para tu pedido. Puedes pedirme cambios aquí mismo.',
                )
                return conversacion
            raise PuenteIAError('La generación de imágenes requiere el proveedor de IA conectado.')

        contexto = contexto_catalogo(texto, request)
        respuesta = self.puente_ia.conversar(mensajes, contexto=contexto)
        self.repository.crear_mensaje(conversacion, Mensaje.Rol.ASISTENTE, respuesta.texto)
        self._guardar_imagenes_chat(usuario, conversacion, texto, respuesta)
        return conversacion

    def _guardar_imagenes_chat(self, usuario, conversacion, prompt, respuesta):
        """Descarga y guarda en la web las imágenes que generó el asistente.

        El backend guarda la propuesta ligada a su prenda; la web necesita su
        propia copia para pintarla en el hilo y en el historial. Si una imagen
        no se puede descargar, la conversación sigue y solo se pierde esa vista.
        """
        for indice, ruta in enumerate(respuesta.imagenes, start=1):
            try:
                contenido = descargar_imagen_backend(ruta)
            except PuenteIAError as error:
                log.warning('No se pudo descargar la imagen %s: %s', ruta, error)
                continue
            extension = urlparse(ruta).path.rsplit('.', 1)[-1].lower()
            if extension not in {'png', 'jpg', 'jpeg', 'webp'}:
                extension = 'png'
            self.repository.crear_propuesta(
                usuario=usuario, conversacion=conversacion, prompt=prompt,
                tipo_prenda='Diseño personalizado', estilo='', temporada='',
                calidad='', paleta='', resultado_resumen=respuesta.texto,
                imagen=ContentFile(
                    contenido,
                    name=f'propuesta-chat-{indice}-{uuid4().hex[:8]}.{extension}',
                ),
                es_favorita=False, es_demo=False,
            )

    def _guardar_propuestas(self, usuario, conversacion, prompt, resultado):
        for propuesta in resultado['propuestas'][:1]:
            imagen = propuesta.get('imagen')
            if propuesta.get('imagen_bytes'):
                imagen = ContentFile(propuesta['imagen_bytes'], name=propuesta.get('imagen_nombre', 'diseno-ia.png'))
            self.repository.crear_propuesta(
                usuario=usuario, conversacion=conversacion, prompt=prompt,
                tipo_prenda='Diseño personalizado', estilo='', temporada='',
                calidad='alta', paleta='', resultado_resumen=propuesta.get('resumen', prompt),
                imagen=imagen, es_favorita=False,
                es_demo=resultado.get('demostracion', False),
            )

    def estado_asistente(self, usuario, conversacion_id=None, propuestas_demo=None):
        conversacion = None
        propuestas = []
        if conversacion_id and usuario.is_authenticated:
            conversacion = self.repository.conversacion_usuario(usuario, conversacion_id)
            propuestas = self.repository.propuestas_conversacion(conversacion)
        elif propuestas_demo:
            propuestas = propuestas_demo
        if usuario.is_authenticated and not conversacion:
            conversacion = self.repository.ultima_conversacion(usuario)
        timeline = []
        if conversacion:
            timeline.extend({'tipo': 'mensaje', 'objeto': m, 'fecha': m.creada}
                            for m in conversacion.mensajes.all())
            timeline.extend({'tipo': 'imagen', 'objeto': p, 'fecha': p.creada}
                            for p in conversacion.propuestas.all())
            timeline.sort(key=lambda item: item['fecha'])
            propuestas = list(conversacion.propuestas.all())
        return conversacion, propuestas, timeline

    def generar(self, usuario, resumen, opciones):
        proveedor = self.proveedor() if callable(self.proveedor) else self.proveedor
        # Mantiene la generación de demostración para visitantes, cuya sesión
        # no debe guardar imágenes pesadas ni necesita crear una cuenta en la API.
        if not usuario.is_authenticated and getattr(proveedor, 'nombre', '') == 'backend':
            proveedor = ProveedorDemo()
        resultado = proveedor.generar(resumen, opciones)
        propuestas = resultado['propuestas']
        conversacion = None
        if usuario.is_authenticated:
            titulo = resumen[:60] + ('…' if len(resumen) > 60 else '')
            conversacion = self.repository.crear_conversacion(usuario, titulo)
            self.repository.crear_mensaje(conversacion, Mensaje.Rol.USUARIO, resumen)
            lista = '\n'.join(
                f'{indice + 1}. {propuesta["titulo"]}: {propuesta["resumen"]}'
                for indice, propuesta in enumerate(propuestas)
            )
            self.repository.crear_mensaje(
                conversacion, Mensaje.Rol.ASISTENTE, f'{resultado["intro"]}\n{lista}'
            )
            for propuesta in propuestas:
                imagen = propuesta.get('imagen')
                if propuesta.get('imagen_bytes'):
                    imagen = ContentFile(
                        propuesta['imagen_bytes'],
                        name=propuesta.get('imagen_nombre', 'propuesta-ia.png'),
                    )
                self.repository.crear_propuesta(
                    usuario=usuario,
                    conversacion=conversacion,
                    prompt=resumen,
                    tipo_prenda=opciones['tipo_prenda'],
                    estilo=opciones['estilo'],
                    temporada=opciones['temporada'],
                    calidad=opciones['calidad'],
                    paleta=opciones['paleta'],
                    resultado_resumen=propuesta['resumen'],
                    imagen=imagen,
                    es_favorita=False,
                    es_demo=resultado.get('demostracion', True),
                )
        else:
            for propuesta in propuestas:
                propuesta['resultado_resumen'] = propuesta.get('resumen', '')
        return {'resultado': resultado, 'conversacion': conversacion, 'propuestas': propuestas}

    def alternar_favorita(self, usuario, pk):
        propuesta = self.repository.propuesta_usuario(usuario, pk)
        nueva_estado = not propuesta.es_favorita
        self.repository.actualizar_estado_favorita(propuesta, nueva_estado)
        return nueva_estado

    def historial(self, usuario, filtro):
        propuestas = self.repository.propuestas_usuario(usuario)
        if filtro == 'favoritas':
            propuestas = propuestas.filter(es_favorita=True)
        return {'propuestas': propuestas, 'filtro': filtro, 'total': propuestas.count()}
