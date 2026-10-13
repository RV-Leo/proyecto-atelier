"""Pruebas de la app core: inicio e informes."""

from django.test import TestCase
from django.urls import reverse

from core.management.commands.cargar_demo import Command as CargarDemo


class InicioTests(TestCase):
    def test_inicio_responde(self):
        respuesta = self.client.get(reverse('core:inicio'))
        self.assertEqual(respuesta.status_code, 200)
        self.assertContains(respuesta, 'ATELIER')
        self.assertContains(respuesta, 'Tu próxima inspiración')

    def test_informes_responde(self):
        respuesta = self.client.get(reverse('informes:informes'))
        self.assertEqual(respuesta.status_code, 200)
        self.assertContains(respuesta, 'Informes')


class CargarDemoTests(TestCase):
    def test_carga_y_es_idempotente(self):
        cargar = CargarDemo()
        cargar.handle()
        cargar.handle()  # no debe duplicar
        from catalogo.models import Categoria, Producto
        from tendencias.models import Tendencia

        self.assertEqual(Categoria.objects.filter(nombre='Vestidos').count(), 1)
        self.assertTrue(Producto.objects.exists())
        self.assertTrue(Tendencia.objects.exists())
        self.assertEqual(Producto.objects.filter(nombre='Vestido largo fluido').count(), 1)


class CrearAdminTests(TestCase):
    def test_crear_admin_idempotente(self):
        from io import StringIO

        from django.core.management import call_command
        from django.contrib.auth import get_user_model

        Usuario = get_user_model()
        llamada = StringIO()
        call_command('crear_admin', stdout=llamada)
        self.assertTrue(Usuario.objects.filter(username='admin', is_superuser=True).exists())
        # Segunda ejecución no duplica ni falla.
        call_command('crear_admin', stdout=llamada)
        self.assertEqual(Usuario.objects.filter(username='admin').count(), 1)