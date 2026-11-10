"""Pruebas de la app core: inicio e informes."""

from unittest.mock import patch

from django.contrib.auth import get_user_model
from django.test import TestCase
from django.urls import reverse

from catalogo.api_cliente import ApiNoDisponible, Pagina, Paginador, Prenda
from core.management.commands.cargar_demo import Command as CargarDemo


def _pagina(items, total=None, total_paginas=1):
    total = len(items) if total is None else total
    return Pagina(
        object_list=items,
        number=1,
        tamano=len(items),
        paginator=Paginador(count=total, num_pages=total_paginas),
        primera=True,
        ultima=True,
    )


def _prenda(nombre, slug, tipo='vestido', estado='ANALIZADA'):
    return Prenda(
        id=1, nombre=nombre, slug=slug, tipo=tipo, estilo='romántico',
        colores='marfil', tela='lino', descripcion='fluido y ligero', estado=estado,
    )


class InicioTests(TestCase):
    def setUp(self):
        # La portada y los informes consultan la API: se simula para no depender
        # de que el backend esté levantado durante las pruebas.
        self.listar = patch(
            'core.views.api_cliente.listar_prendas', return_value=_pagina([])
        ).start()
        patch('core.views.api_cliente.listar_materiales', return_value=[]).start()
        self.addCleanup(patch.stopall)

    def test_inicio_publico_responde(self):
        respuesta = self.client.get(reverse('core:inicio'))
        self.assertEqual(respuesta.status_code, 200)
        self.assertContains(respuesta, 'ATELIER')
        self.assertContains(respuesta, 'Del hilo a la idea')

    def test_portada_invita_a_iniciar_sesion_o_crear_cuenta(self):
        respuesta = self.client.get(reverse('core:inicio'))
        self.assertContains(respuesta, 'Iniciar sesión')
        self.assertContains(respuesta, 'Crear cuenta')
        self.assertContains(respuesta, reverse('cuentas:acceder'))
        self.assertContains(respuesta, reverse('cuentas:registro'))

    def test_inicio_muestra_prendas_de_la_api(self):
        self.listar.return_value = _pagina([_prenda('Vestido Largo', 'vestido-largo')])
        respuesta = self.client.get(reverse('core:inicio'))
        self.assertContains(respuesta, 'Vestido Largo')

    def test_inicio_avisa_si_la_api_no_responde(self):
        self.listar.side_effect = ApiNoDisponible('API caída')
        respuesta = self.client.get(reverse('core:inicio'))
        self.assertEqual(respuesta.status_code, 200)
        self.assertContains(respuesta, 'El catálogo no está disponible')

    def test_informes_requieren_autenticacion(self):
        respuesta = self.client.get(reverse('informes:informes'))
        self.assertEqual(respuesta.status_code, 302)
        self.assertIn(reverse('cuentas:acceder'), respuesta.url)

    def test_informes_solo_para_administradores(self):
        Usuario = get_user_model()
        Usuario.objects.create_user('clienta', password='clave-segura-123')
        self.client.login(username='clienta', password='clave-segura-123')
        self.assertEqual(self.client.get(reverse('informes:informes')).status_code, 403)

    def test_informes_para_administrador(self):
        Usuario = get_user_model()
        Usuario.objects.create_user(
            'admin_web', password='clave-segura-123', is_staff=True
        )
        self.client.login(username='admin_web', password='clave-segura-123')
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