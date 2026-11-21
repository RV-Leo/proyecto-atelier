"""Pruebas del asistente de diseño con IA (modo demostración)."""

import shutil
import tempfile
from unittest.mock import patch

from django.contrib.auth import get_user_model
from django.db import OperationalError, connection
from django.test import RequestFactory, TestCase, TransactionTestCase, override_settings
from django.urls import reverse

from catalogo.models import Material

from .models import Conversacion, Mensaje, PropuestaDiseno
from .ia_gateway import PuenteIAError
from .services import DisenoService
from .servicios import ProveedorDemo, obtener_proveedor

Usuario = get_user_model()
MEDIA_TEMPORAL = tempfile.mkdtemp()


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
        self.assertContains(respuesta, 'modo demostración')

    def test_post_anonimo_no_guarda_y_avisa(self):
        respuesta = self.client.post(reverse('disenador:asistente'), self.datos)
        self.assertEqual(respuesta.status_code, 302)
        self.assertFalse(PropuestaDiseno.objects.exists())
        self.assertEqual(len(self.client.session.get('propuestas_demo', [])), 3)
        # El GET muestra las propuestas y limpia la sesión.
        pagina = self.client.get(reverse('disenador:asistente'))
        self.assertContains(pagina, 'Propuesta 1')
        self.assertEqual(self.client.session.get('propuestas_demo', []), [])

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

    def test_edicion_muestra_aviso_amigable_si_sqlite_sigue_ocupado(self):
        usuario = Usuario.objects.create_user('edicion-lock', password='clave-segura-123')
        conversacion = Conversacion.objects.create(usuario=usuario, titulo='Vestido')
        mensaje = Mensaje.objects.create(
            conversacion=conversacion, rol=Mensaje.Rol.USUARIO, texto='Vestido de lino'
        )
        self.client.login(username='edicion-lock', password='clave-segura-123')

        with patch('disenador.views.servicio_diseno.editar_mensaje',
                   side_effect=OperationalError('database is locked')):
            respuesta = self.client.post(
                reverse('disenador:editar_mensaje', args=[mensaje.pk]),
                {'texto': 'Vestido de algodón'},
            )

        self.assertRedirects(
            respuesta,
            reverse('disenador:asistente') + f'?c={conversacion.pk}#chat',
            fetch_redirect_response=False,
        )
        self.assertContains(
            self.client.get(reverse('disenador:asistente') + f'?c={conversacion.pk}'),
            'La conversación está ocupada y no se pudo guardar el cambio.',
        )


class EdicionMensajeTests(TransactionTestCase):
    reset_sequences = True

    def setUp(self):
        self.usuario = Usuario.objects.create_user('editora', password='clave-segura-123')
        self.conversacion = Conversacion.objects.create(
            usuario=self.usuario, titulo='Diseño de vestido'
        )
        self.mensaje_previo = Mensaje.objects.create(
            conversacion=self.conversacion,
            rol=Mensaje.Rol.USUARIO,
            texto='Diseña un vestido de lino',
        )
        Mensaje.objects.create(
            conversacion=self.conversacion,
            rol=Mensaje.Rol.ASISTENTE,
            texto='Te propongo un vestido de lino.',
        )
        self.mensaje_editado = Mensaje.objects.create(
            conversacion=self.conversacion,
            rol=Mensaje.Rol.USUARIO,
            texto='¿Qué telas están disponibles?',
        )
        self.respuesta_antigua = Mensaje.objects.create(
            conversacion=self.conversacion,
            rol=Mensaje.Rol.ASISTENTE,
            texto='Respuesta que pertenece al turno antiguo.',
        )
        self.request = RequestFactory().post('/disenador/mensaje/editar/')

    def test_edicion_llama_ia_fuera_de_transaccion_y_reemplaza_turnos_posteriores(self):
        puente = PuenteSinBloqueo()
        servicio = DisenoService(puente_ia=puente)

        servicio.editar_mensaje(
            self.usuario, self.mensaje_editado.pk, '¿Qué productos de lino hay?', self.request
        )

        self.assertFalse(puente.se_llamo_dentro_de_transaccion)
        self.mensaje_editado.refresh_from_db()
        self.assertEqual(self.mensaje_editado.texto, '¿Qué productos de lino hay?')
        self.assertFalse(Mensaje.objects.filter(pk=self.respuesta_antigua.pk).exists())
        self.assertEqual(
            self.conversacion.mensajes.order_by('-pk').first().texto,
            'Respuesta regenerada sin bloquear la base de datos.',
        )
        self.assertIn('Diseña un vestido de lino', puente.mensajes_enviados[0]['content'])
        self.assertNotIn('Respuesta que pertenece al turno antiguo.',
                         str(puente.mensajes_enviados))

    def test_fallo_de_ia_conserva_el_mensaje_y_el_historial_originales(self):
        servicio = DisenoService(puente_ia=PuenteConError())

        with self.assertRaises(PuenteIAError):
            servicio.editar_mensaje(
                self.usuario, self.mensaje_editado.pk, '¿Qué productos de lino hay?', self.request
            )

        self.mensaje_editado.refresh_from_db()
        self.assertEqual(self.mensaje_editado.texto, '¿Qué telas están disponibles?')
        self.assertTrue(Mensaje.objects.filter(pk=self.respuesta_antigua.pk).exists())


class PuenteSinBloqueo:
    def __init__(self):
        self.se_llamo_dentro_de_transaccion = None
        self.mensajes_enviados = []

    def conversar(self, mensajes, contexto=''):
        self.se_llamo_dentro_de_transaccion = connection.in_atomic_block
        self.mensajes_enviados = mensajes
        return 'Respuesta regenerada sin bloquear la base de datos.'


class PuenteConError:
    def conversar(self, mensajes, contexto=''):
        raise PuenteIAError('Gemini no respondió.')
