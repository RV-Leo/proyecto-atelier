"""Pruebas del asistente de diseño con IA (modo demostración)."""

import json
import shutil
import tempfile
from io import BytesIO
from urllib.error import HTTPError
from unittest.mock import Mock, patch

from django.contrib.auth import get_user_model
from django.test import TestCase, override_settings
from django.urls import reverse

from .servicios import ProveedorDemo, ProveedorGateway, obtener_proveedor
from catalogo.models import Material

from .models import Conversacion, Mensaje, PropuestaDiseno

Usuario = get_user_model()
MEDIA_TEMPORAL = tempfile.mkdtemp()


@override_settings(MEDIA_ROOT=MEDIA_TEMPORAL)
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
        self.assertIsInstance(obtener_proveedor(), ProveedorDemo)

    @override_settings(
        AI_BACKEND_URL='https://backend.test',
        AI_BACKEND_USER='web',
        AI_BACKEND_PASSWORD='clave-local',
        AI_BACKEND_TIMEOUT=10,
    )
    @patch('disenador.servicios.urlopen')
    def test_gateway_envia_la_solicitud_al_backend(self, urlopen):
        respuesta_http = Mock()
        respuesta_http.read.return_value = json.dumps({
            'respuesta': 'Vestido amplio de lino con costuras visibles.',
        }).encode('utf-8')
        respuesta_http.__enter__ = Mock(return_value=respuesta_http)
        respuesta_http.__exit__ = Mock(return_value=False)
        urlopen.return_value = respuesta_http

        resultado = ProveedorGateway().generar('vestido de lino', {
            'tipo_prenda': 'vestido',
            'estilo': 'boho',
            'temporada': 'Primavera-Verano',
            'calidad': 'editorial',
            'paleta': 'beige',
        })

        solicitud = urlopen.call_args.args[0]
        self.assertEqual(solicitud.full_url, 'https://backend.test/api/v1/ia/chat')
        self.assertEqual(solicitud.get_header('Authorization'), 'Basic d2ViOmNsYXZlLWxvY2Fs')
        self.assertEqual(
            json.loads(solicitud.data)['messages'][0]['role'],
            'user',
        )
        self.assertEqual(
            resultado['propuestas'][0]['resumen'],
            'Vestido amplio de lino con costuras visibles.',
        )
        self.assertTrue(resultado['demostracion'])
        self.assertIn('ilustrativa', resultado['propuestas'][0]['nota'])

    @override_settings(AI_PROVEEDOR='backend')
    def test_obtener_proveedor_backend(self):
        self.assertIsInstance(obtener_proveedor(), ProveedorGateway)

    @override_settings(
        AI_BACKEND_URL='https://backend.test',
        AI_BACKEND_USER='web',
        AI_BACKEND_PASSWORD='clave-local',
        AI_BACKEND_TIMEOUT=10,
    )
    @patch('disenador.servicios.urlopen')
    def test_gateway_muestra_el_mensaje_de_error_del_backend(self, urlopen):
        detalle = 'El modelo solicitado no está disponible. Revisa IA_MODELO_TEXTO.'
        urlopen.side_effect = HTTPError(
            'https://backend.test/api/v1/ia/chat',
            503,
            'Servicio no disponible',
            None,
            BytesIO(json.dumps({'mensaje': detalle}).encode('utf-8')),
        )

        with self.assertRaisesRegex(RuntimeError, 'IA_MODELO_TEXTO'):
            ProveedorGateway().conversar([{'role': 'user', 'content': 'Prueba'}])

    @override_settings(
        AI_BACKEND_URL='https://backend.test',
        AI_BACKEND_USER='web',
        AI_BACKEND_PASSWORD='clave-local',
        AI_BACKEND_TIMEOUT=10,
    )
    @patch('disenador.servicios.urlopen')
    def test_gateway_envia_historial_de_chat(self, urlopen):
        respuesta_http = Mock()
        respuesta_http.read.return_value = json.dumps({
            'respuesta': 'El lino es fresco y transpirable.',
        }).encode('utf-8')
        respuesta_http.__enter__ = Mock(return_value=respuesta_http)
        respuesta_http.__exit__ = Mock(return_value=False)
        urlopen.return_value = respuesta_http

        respuesta = ProveedorGateway().conversar([
            {'role': 'user', 'content': '¿Qué tela es fresca?'},
            {'role': 'assistant', 'content': '¿Para qué prenda?'},
            {'role': 'user', 'content': 'Para un vestido de verano.'},
        ])

        solicitud = urlopen.call_args.args[0]
        self.assertEqual(respuesta, 'El lino es fresco y transpirable.')
        self.assertEqual(len(json.loads(solicitud.data)['messages']), 3)


@override_settings(MEDIA_ROOT=MEDIA_TEMPORAL)
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
        self.assertContains(respuesta, 'modo demostración')

    @override_settings(AI_PROVEEDOR='backend')
    def test_chat_se_muestra_al_usuario_autenticado(self):
        Usuario.objects.create_user('cliente_chat', password='clave-segura-123')
        self.client.login(username='cliente_chat', password='clave-segura-123')

        respuesta = self.client.get(reverse('disenador:asistente'))

        self.assertContains(respuesta, 'Asesoría de estilo con IA')
        self.assertContains(respuesta, 'name="mensaje"')
        self.assertContains(respuesta, 'aria-label="Enviar mensaje"')
        self.assertContains(respuesta, 'placeholder="Escribe un mensaje..."')
        self.assertNotContains(respuesta, 'Escribe tu mensaje')
        self.assertNotContains(respuesta, 'Pregunta para la asesora de estilo')
        self.assertNotContains(respuesta, '>Preguntar</button>')

    def test_post_anonimo_no_guarda_y_avisa(self):
        respuesta = self.client.post(reverse('disenador:asistente'), self.datos)
        self.assertEqual(respuesta.status_code, 302)
        self.assertFalse(PropuestaDiseno.objects.exists())
        self.assertEqual(len(self.client.session.get('propuestas_demo', [])), 3)
        # El GET muestra las propuestas y limpia la sesión.
        pagina = self.client.get(reverse('disenador:asistente'))
        self.assertContains(pagina, 'Propuesta 1')
        self.assertEqual(self.client.session.get('propuestas_demo', []), [])

    @override_settings(AI_PROVEEDOR='backend')
    @patch('disenador.servicios.urlopen')
    def test_post_anonimo_no_consulta_el_modelo(self, urlopen):
        respuesta = self.client.post(reverse('disenador:asistente'), self.datos)
        self.assertEqual(respuesta.status_code, 200)
        self.assertContains(respuesta, 'Inicia sesión para consultar el modelo de IA.')
        urlopen.assert_not_called()

    @override_settings(AI_PROVEEDOR='backend')
    @patch(
        'disenador.servicios.ProveedorGateway.conversar',
        return_value='El lino es ligero para verano.',
    )
    def test_chat_autenticado_guarda_y_envia_historial(self, conversar):
        usuario = Usuario.objects.create_user('asesora', password='clave-segura-123')
        self.client.login(username='asesora', password='clave-segura-123')
        conversacion = Conversacion.objects.create(usuario=usuario, titulo='Consulta de telas')
        Mensaje.objects.create(
            conversacion=conversacion,
            rol=Mensaje.Rol.USUARIO,
            texto='Busco una tela fresca.',
        )
        Mensaje.objects.create(
            conversacion=conversacion,
            rol=Mensaje.Rol.ASISTENTE,
            texto='¿Para qué prenda?',
        )

        respuesta = self.client.post(reverse('disenador:conversar'), {
            'conversacion_id': conversacion.pk,
            'mensaje': 'Para un vestido de verano.',
        })

        self.assertRedirects(
            respuesta,
            reverse('disenador:asistente') + f'?c={conversacion.pk}',
        )
        self.assertEqual(conversacion.mensajes.count(), 4)
        self.assertEqual(
            conversar.call_args.args[0][-1],
            {'role': 'user', 'content': 'Para un vestido de verano.'},
        )

    @override_settings(AI_PROVEEDOR='backend')
    def test_chat_no_permite_consultar_conversacion_ajena(self):
        propietaria = Usuario.objects.create_user('duena_chat', password='clave-segura-123')
        Usuario.objects.create_user('otro_chat', password='clave-segura-123')
        conversacion = Conversacion.objects.create(usuario=propietaria, titulo='Privada')
        self.client.login(username='otro_chat', password='clave-segura-123')

        respuesta = self.client.post(reverse('disenador:conversar'), {
            'conversacion_id': conversacion.pk,
            'mensaje': '¿Qué tela recomiendas?',
        })

        self.assertEqual(respuesta.status_code, 404)
        self.assertEqual(conversacion.mensajes.count(), 0)

    def test_post_autenticado_guarda_conversacion_y_propuestas(self):
        Usuario.objects.create_user('disenadora', password='clave-segura-123')
        self.client.login(username='disenadora', password='clave-segura-123')
        respuesta = self.client.post(reverse('disenador:asistente'), self.datos)
        conversacion = Conversacion.objects.get()
        self.assertRedirects(respuesta, reverse('disenador:asistente') + f'?c={conversacion.pk}')
        self.assertEqual(conversacion.mensajes.count(), 2)
        self.assertEqual(conversacion.propuestas.count(), 3)
        self.assertTrue(all(p.es_demo for p in conversacion.propuestas.all()))

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