"""Pruebas del catálogo: modelos locales, vistas sobre la API y cliente HTTP."""

from unittest.mock import patch

import requests
from django.contrib.auth import get_user_model
from django.test import SimpleTestCase, TestCase
from django.urls import reverse

from . import api_cliente
from .api_cliente import ApiNoDisponible, Material, Pagina, Paginador, Prenda
from .models import Categoria, Material as MaterialLocal, Producto


class ModelosTests(TestCase):
    """Los modelos locales siguen existiendo para las tablas internas."""

    def setUp(self):
        self.categoria = Categoria.objects.create(nombre='Vestidos')

    def test_slug_automatico(self):
        producto = Producto.objects.create(
            nombre='Vestido de Prueba', descripcion='x', categoria=self.categoria, precio='10.00'
        )
        self.assertEqual(producto.slug, 'vestido-de-prueba')

    def test_estado_tendencia_nuevo_agotado(self):
        tendencia = Producto.objects.create(
            nombre='A', descripcion='x', categoria=self.categoria, precio='5.00',
            unidades=3, es_tendencia=True,
        )
        nuevo = Producto.objects.create(
            nombre='B', descripcion='x', categoria=self.categoria, precio='5.00',
            unidades=3, es_nuevo=True,
        )
        agotado = Producto.objects.create(
            nombre='C', descripcion='x', categoria=self.categoria, precio='5.00', unidades=0
        )
        self.assertEqual(tendencia.estado, 'tendencia')
        self.assertEqual(nuevo.estado, 'nuevo')
        self.assertEqual(agotado.estado, 'agotado')
        self.assertFalse(agotado.disponible)

    def test_estado_material(self):
        disponible = MaterialLocal.objects.create(nombre='Lino', categoria='Telas', cantidad=50, stock_minimo=10)
        bajo = MaterialLocal.objects.create(nombre='Seda', categoria='Telas', cantidad=5, stock_minimo=10)
        agotado = MaterialLocal.objects.create(nombre='Lana', categoria='Telas', cantidad=0, stock_minimo=10)
        self.assertEqual(disponible.estado, 'disponible')
        self.assertEqual(bajo.estado, 'bajo')
        self.assertEqual(agotado.estado, 'agotado')


# --- Ayudas para simular respuestas de la API ----------------------------


def _prenda(id, nombre, slug, tipo, estilo='', colores='', tela='', descripcion='', estado='ANALIZADA'):
    return Prenda(
        id=id, nombre=nombre, slug=slug, tipo=tipo, estilo=estilo,
        colores=colores, tela=tela, descripcion=descripcion, estado=estado,
    )


def _pagina(items, numero=1, total=None, total_paginas=1):
    total = len(items) if total is None else total
    return Pagina(
        object_list=items,
        number=numero,
        tamano=len(items),
        paginator=Paginador(count=total, num_pages=total_paginas),
        primera=numero <= 1,
        ultima=numero >= total_paginas,
    )


def _material(**cambios):
    datos = {
        'id': 1,
        'nombre': 'Lino',
        'tipo': 'Telas',
        'color': 'Natural',
        'unidad': 'METRO',
        'stock': 40.0,
        'stock_minimo': 10.0,
        'estado': 'DISPONIBLE',
        'proveedor': 'Proveedor Andino',
        'precio_unitario': 12.5,
    }
    datos.update(cambios)
    return Material(**datos)


class VistasTests(TestCase):
    """Las vistas piden los datos a la API; aquí se simula esa respuesta."""

    def setUp(self):
        self.prenda = _prenda(
            1, 'Vestido Largo', 'vestido-largo', 'vestido',
            estilo='romántico', colores='marfil', tela='lino',
            descripcion='fluido y ligero', estado='ANALIZADA',
        )
        self.tela = _prenda(
            2, 'Lino Natural', 'lino-natural', 'tela',
            colores='natural', tela='lino', descripcion='fibra vegetal', estado='GENERADA',
        )
        self.pagina = _pagina([self.prenda, self.tela])

        self.listar = patch(
            'catalogo.views.api_cliente.listar_prendas', return_value=self.pagina
        ).start()
        self.addCleanup(patch.stopall)

        patch(
            'catalogo.views.api_cliente.opciones_catalogo',
            return_value=(['tela', 'vestido'], ['romántico']),
        ).start()
        self.obtener = patch(
            'catalogo.views.api_cliente.obtener_por_slug', return_value=self.prenda
        ).start()
        self.materiales = patch(
            'catalogo.views.api_cliente.listar_materiales',
            return_value=[_material()],
        ).start()

    def test_lista_responde(self):
        respuesta = self.client.get(reverse('catalogo:lista'))
        self.assertEqual(respuesta.status_code, 200)
        self.assertContains(respuesta, 'Vestido Largo')
        self.assertContains(respuesta, 'Lino Natural')
        self.assertContains(respuesta, '2 prendas')

    def test_busqueda_se_envia_a_la_api(self):
        self.listar.return_value = _pagina([self.tela])
        respuesta = self.client.get(reverse('catalogo:lista'), {'q': 'vegetal'})
        self.assertContains(respuesta, 'Lino Natural')
        self.assertNotContains(respuesta, 'Vestido Largo')
        self.assertEqual(self.listar.call_args.kwargs['q'], 'vegetal')

    def test_filtro_por_tipo_se_envia_a_la_api(self):
        self.listar.return_value = _pagina([self.tela])
        respuesta = self.client.get(reverse('catalogo:lista'), {'tipo': 'tela'})
        self.assertContains(respuesta, 'Lino Natural')
        self.assertNotContains(respuesta, 'Vestido Largo')
        self.assertEqual(self.listar.call_args.kwargs['tipo'], 'tela')

    def test_paginacion_se_envia_a_la_api(self):
        self.client.get(reverse('catalogo:lista'), {'pagina': '2'})
        self.assertEqual(self.listar.call_args.kwargs['pagina'], 2)
        self.assertEqual(self.listar.call_args.kwargs['tamano'], 12)

    def test_lista_avisa_si_la_api_no_responde(self):
        self.listar.side_effect = ApiNoDisponible('API caída')
        respuesta = self.client.get(reverse('catalogo:lista'))
        self.assertEqual(respuesta.status_code, 200)
        self.assertContains(respuesta, 'No se pudo cargar el catálogo')
        self.assertContains(respuesta, 'API caída')

    def test_detalle_responde(self):
        respuesta = self.client.get(reverse('catalogo:detalle', args=[self.prenda.slug]))
        self.assertEqual(respuesta.status_code, 200)
        self.assertContains(respuesta, 'Vestido Largo')
        self.assertContains(respuesta, 'Analizada')
        self.assertContains(respuesta, 'lino')

    def test_detalle_slug_inexistente(self):
        self.obtener.return_value = None
        respuesta = self.client.get(reverse('catalogo:detalle', args=['no-existe']))
        self.assertEqual(respuesta.status_code, 404)

    def test_detalle_avisa_si_la_api_no_responde(self):
        self.obtener.side_effect = ApiNoDisponible('API caída')
        respuesta = self.client.get(reverse('catalogo:detalle', args=['vestido-largo']))
        self.assertEqual(respuesta.status_code, 503)
        self.assertContains(respuesta, 'No se pudo cargar la prenda', status_code=503)

    def test_inventario_requiere_sesion(self):
        respuesta = self.client.get(reverse('catalogo:inventario'))
        self.assertEqual(respuesta.status_code, 302)
        self.assertIn(reverse('cuentas:acceder'), respuesta.url)

    def test_inventario_para_usuario_autenticado(self):
        Usuario = get_user_model()
        Usuario.objects.create_user('clienta', password='clave-segura-123')
        self.client.login(username='clienta', password='clave-segura-123')
        respuesta = self.client.get(reverse('catalogo:inventario'))
        self.assertEqual(respuesta.status_code, 200)
        self.assertContains(respuesta, 'Inventario de materiales')
        self.assertContains(respuesta, 'Lino')

    def test_inventario_filtra_por_estado_en_la_api(self):
        Usuario = get_user_model()
        Usuario.objects.create_user('clienta', password='clave-segura-123')
        self.client.login(username='clienta', password='clave-segura-123')
        self.client.get(reverse('catalogo:inventario'), {'estado': 'bajo'})
        self.materiales.assert_called_with(q=None, estado='bajo')


class ClienteApiTests(SimpleTestCase):
    """Contrato con la API: autenticación, mapeo y errores."""

    @patch('catalogo.api_cliente.requests.get')
    def test_listar_prendas_mapea_la_respuesta(self, get):
        get.return_value.status_code = 200
        get.return_value.json.return_value = {
            'items': [{
                'id': 28,
                'nombre': 'Vestido Largo',
                'slug': 'vestido-largo',
                'tipoPrenda': 'vestido',
                'estilo': 'romántico',
                'colores': 'marfil',
                'tela': 'lino',
                'descripcion': 'fluido y ligero',
                'estado': 'ANALIZADA',
            }],
            'pagina': 1, 'tamano': 12, 'total': 1, 'totalPaginas': 1,
            'primera': True, 'ultima': True,
        }

        pagina = api_cliente.listar_prendas()

        self.assertEqual(pagina.paginator.count, 1)
        self.assertEqual(pagina.number, 1)
        self.assertEqual(pagina.object_list[0].nombre, 'Vestido Largo')
        self.assertEqual(pagina.object_list[0].tipo, 'vestido')
        self.assertEqual(pagina.object_list[0].get_estado_display(), 'Analizada')
        self.assertEqual(get.call_args.kwargs['auth'], ('atelier', 'atelier-dev'))
        self.assertEqual(get.call_args.kwargs['params']['pagina'], 1)

    @patch('catalogo.api_cliente.requests.get')
    def test_obtener_por_slug_inexistente_devuelve_none(self, get):
        get.return_value.status_code = 404
        self.assertIsNone(api_cliente.obtener_por_slug('no-existe'))

    @patch('catalogo.api_cliente.requests.get')
    def test_error_http_lanza_api_no_disponible(self, get):
        get.return_value.status_code = 500
        with self.assertRaises(ApiNoDisponible):
            api_cliente.listar_prendas()

    @patch('catalogo.api_cliente.requests.get')
    def test_error_de_red_lanza_api_no_disponible(self, get):
        get.side_effect = requests.ConnectionError('sin conexión')
        with self.assertRaises(ApiNoDisponible):
            api_cliente.listar_prendas()

    @patch('catalogo.api_cliente.requests.get')
    def test_listar_materiales_mapea_y_traduce_el_estado(self, get):
        get.return_value.status_code = 200
        get.return_value.json.return_value = {
            'items': [{
                'id': 5,
                'nombre': 'Lino',
                'tipo': 'Telas',
                'color': 'Natural',
                'unidad': 'METRO',
                'stock': 40,
                'stockMinimo': 10,
                'estado': 'DISPONIBLE',
                'proveedor': 'Proveedor Andino',
                'precioUnitario': 12.5,
                'activo': True,
            }],
            'pagina': 1, 'tamano': 1, 'total': 1, 'totalPaginas': 1,
            'primera': True, 'ultima': True,
        }

        materiales = api_cliente.listar_materiales(estado='disponible')

        self.assertEqual(get.call_args.kwargs['params']['estado'], 'DISPONIBLE')
        self.assertEqual(materiales[0].nombre, 'Lino')
        self.assertEqual(materiales[0].cantidad, 40.0)
        self.assertTrue(materiales[0].disponible)
        self.assertEqual(materiales[0].get_unidad_medida_display(), 'Metro')
