"""Pruebas del asistente de diseño con IA (modo demostración)."""

import shutil
import tempfile
from unittest.mock import patch

from django.contrib.auth import get_user_model
from django.test import RequestFactory, TestCase, override_settings
from django.urls import reverse

from catalogo.models import Material

from .ia_gateway import PuenteIAError, RespuestaChat
from .models import Conversacion, Mensaje, PropuestaDiseno
from .services import DisenoService
from .servicios import ProveedorDemo, obtener_proveedor

Usuario = get_user_model()
MEDIA_TEMPORAL = tempfile.mkdtemp()
MEDIA_CHAT = tempfile.mkdtemp()


@override_settings(MEDIA_ROOT=MEDIA_TEMPORAL, AI_PROVEEDOR='demo')
class ServicioTests(TestCase):
    def setUp(self):
        Material.objects.create(nombre='Lino', color='beige', categoria='Telas', cantidad=40)

    @classmethod
    def tearDownClass(cls):
        super().tearDownClass()
        shutil.rmtree(MEDIA_TEMPORAL, ignore_errors=True)

    def test_proveedor_demo_genera_tres_propuestas(self):
        resultado = ProveedorDemo().generar('vestido de lino', {
            'tipo_prenda': 'vestido', 'estilo': 'boho',
            'temporada': 'Primavera-Verano', 'calidad': 'editorial', 'paleta': 'beige',
        })
        self.assertTrue(resultado['demostracion'])
        self.assertEqual(len(resultado['propuestas']), 3)
        self.assertIn('simuladas', resultado['intro'])

    def test_obtener_proveedor_por_defecto_es_demo(self):
        # No depende de web/.env: aquí AI_PROVEEDOR está forzado a 'demo'.
        self.assertIsInstance(obtener_proveedor(), ProveedorDemo)

    @override_settings(AI_PROVEEDOR='backend')
    def test_obtener_proveedor_backend(self):
        from .servicios import ProveedorBackend

        self.assertIsInstance(obtener_proveedor(), ProveedorBackend)


@override_settings(MEDIA_ROOT=MEDIA_TEMPORAL, AI_PROVEEDOR='demo')
class AsistenteVistasTests(TestCase):
    def setUp(self):
        Material.objects.create(nombre='Lino', color='beige', categoria='Telas', cantidad=40)
        self.datos = {
            'descripcion': 'Vestido de lino para verano',
            'tipo_prenda': 'vestido',
            'estilo': 'boho',
            'temporada': 'Primavera-Verano',
            'calidad': 'editorial',
            'paleta': ['beige', 'blanco'],
        }

    def test_get_asistente_responde(self):
        respuesta = self.client.get(reverse('disenador:asistente'))
        self.assertEqual(respuesta.status_code, 200)
        self.assertContains(respuesta, 'Diseñador IA')
        self.assertContains(respuesta, 'Chat con Atelier IA')

    def test_post_anonimo_no_guarda_y_avisa(self):
        respuesta = self.client.post(
            reverse('disenador:conversar'), {'mensaje': 'Hola, ¿qué telas hay?'}
        )
        self.assertEqual(respuesta.status_code, 302)
        self.assertIn(reverse('cuentas:acceder'), respuesta.url)
        self.assertFalse(PropuestaDiseno.objects.exists())

    def test_post_autenticado_guarda_conversacion_y_propuestas(self):
        Usuario.objects.create_user('disenadora', password='clave-segura-123')
        self.client.login(username='disenadora', password='clave-segura-123')
        from . import views

        puente = PuenteFalso(RespuestaChat('Hola, soy Atelier IA.'))
        with patch.object(views.servicio_diseno, 'puente_ia', puente):
            respuesta = self.client.post(
                reverse('disenador:conversar'), {'mensaje': 'Hola, ¿qué telas hay?'}
            )
        conversacion = Conversacion.objects.get()
        self.assertRedirects(
            respuesta, reverse('disenador:asistente') + f'?c={conversacion.pk}#chat',
            fetch_redirect_response=False,
        )
        self.assertEqual(conversacion.mensajes.count(), 2)
        self.assertEqual(
            conversacion.mensajes.get(rol=Mensaje.Rol.ASISTENTE).texto,
            'Hola, soy Atelier IA.',
        )

    def test_favorita_requiere_sesion(self):
        respuesta = self.client.post(reverse('disenador:alternar_favorita', args=[1]))
        self.assertEqual(respuesta.status_code, 302)
        self.assertIn(reverse('cuentas:acceder'), respuesta.url)

    def test_favorita_solo_del_propietario(self):
        propietaria = Usuario.objects.create_user('duena', password='clave-segura-123')
        otra = Usuario.objects.create_user('otra', password='clave-segura-123')
        propuesta = PropuestaDiseno.objects.create(usuario=propietaria, prompt='x')
        self.client.login(username='otra', password='clave-segura-123')
        respuesta = self.client.post(reverse('disenador:alternar_favorita', args=[propuesta.pk]))
        self.assertEqual(respuesta.status_code, 404)

    def test_favorita_alterna(self):
        propietaria = Usuario.objects.create_user('duena2', password='clave-segura-123')
        propuesta = PropuestaDiseno.objects.create(usuario=propietaria, prompt='x')
        self.client.login(username='duena2', password='clave-segura-123')
        self.client.post(reverse('disenador:alternar_favorita', args=[propuesta.pk]))
        propuesta.refresh_from_db()
        self.assertTrue(propuesta.es_favorita)
        self.client.post(reverse('disenador:alternar_favorita', args=[propuesta.pk]))
        propuesta.refresh_from_db()
        self.assertFalse(propuesta.es_favorita)

    def test_historial_requiere_sesion(self):
        respuesta = self.client.get(reverse('disenador:historial'))
        self.assertEqual(respuesta.status_code, 302)


class PuenteFalso:
    """Sustituye al cliente HTTP para probar el turno sin backend real."""

    def __init__(self, respuesta):
        self.respuesta = respuesta
        self.llamada = None

    def conversar(self, mensajes, contexto=''):
        self.llamada = (mensajes, contexto)
        return self.respuesta


@override_settings(MEDIA_ROOT=MEDIA_CHAT)
class ChatConHerramientasTests(TestCase):
    """El chat guarda las imágenes que devuelve el asistente del backend."""

    @classmethod
    def tearDownClass(cls):
        super().tearDownClass()
        shutil.rmtree(MEDIA_CHAT, ignore_errors=True)

    def setUp(self):
        self.usuario = Usuario.objects.create_user('chat-ia', password='clave-segura-123')
        self.request = RequestFactory().get('/disenador/')
        self.request.user = self.usuario

    @patch('disenador.services.descargar_imagen_backend')
    def test_guarda_la_imagen_generada_en_el_turno(self, descargar):
        descargar.return_value = b'\xff\xd8\xff\xe0imagen'
        puente = PuenteFalso(RespuestaChat(
            'Generé una propuesta para tu prenda.',
            ('/imagenes/2026-11-17/propuesta.jpg',),
        ))
        servicio = DisenoService(puente_ia=puente)

        conversacion = servicio.procesar_turno(
            self.usuario, '¿Me ayudas con una idea para el verano?', self.request
        )

        self.assertEqual(conversacion.mensajes.filter(rol=Mensaje.Rol.ASISTENTE).count(), 1)
        propuesta = conversacion.propuestas.get()
        self.assertFalse(propuesta.es_demo)
        self.assertTrue(propuesta.imagen.name.endswith('.jpg'))
        self.assertTrue(propuesta.imagen.storage.exists(propuesta.imagen.name))
        descargar.assert_called_once_with('/imagenes/2026-11-17/propuesta.jpg')

    @patch('disenador.services.descargar_imagen_backend')
    def test_si_la_descarga_falla_igual_responde(self, descargar):
        descargar.side_effect = PuenteIAError('sin imagen')
        puente = PuenteFalso(RespuestaChat(
            'No pude mostrar la imagen.',
            ('/imagenes/no-existe.jpg',),
        ))
        servicio = DisenoService(puente_ia=puente)

        conversacion = servicio.procesar_turno(
            self.usuario, 'Muéstrame la propuesta de la prenda.', self.request
        )

        self.assertEqual(conversacion.propuestas.count(), 0)
        mensaje = conversacion.mensajes.get(rol=Mensaje.Rol.ASISTENTE)
        self.assertIn('No pude mostrar', mensaje.texto)