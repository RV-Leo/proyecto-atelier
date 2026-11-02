"""Pruebas del asistente de diseño con IA (modo demostración)."""

import shutil
import tempfile

from django.contrib.auth import get_user_model
from django.test import TestCase, override_settings
from django.urls import reverse

from catalogo.models import Material

from .models import Conversacion, PropuestaDiseno
from .servicios import ProveedorDemo, obtener_proveedor

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