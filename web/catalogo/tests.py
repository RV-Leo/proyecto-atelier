"""Pruebas del catálogo: modelos, vistas y filtros."""

from django.test import TestCase
from django.urls import reverse

from .models import Categoria, Material, Producto


class ModelosTests(TestCase):
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
        disponible = Material.objects.create(nombre='Lino', categoria='Telas', cantidad=50, stock_minimo=10)
        bajo = Material.objects.create(nombre='Seda', categoria='Telas', cantidad=5, stock_minimo=10)
        agotado = Material.objects.create(nombre='Lana', categoria='Telas', cantidad=0, stock_minimo=10)
        self.assertEqual(disponible.estado, 'disponible')
        self.assertEqual(bajo.estado, 'bajo')
        self.assertEqual(agotado.estado, 'agotado')


class VistasTests(TestCase):
    def setUp(self):
        self.categoria = Categoria.objects.create(nombre='Vestidos')
        self.prenda = Producto.objects.create(
            nombre='Vestido Largo', descripcion='fluido y ligero',
            categoria=self.categoria, precio='80.00', unidades=5, es_destacado=True,
        )
        self.tela = Producto.objects.create(
            nombre='Lino Natural', descripcion='fibra vegetal', tipo='TELA',
            categoria=self.categoria, precio='18.00', unidades=2,
        )
        Material.objects.create(nombre='Lino', categoria='Telas', cantidad=40, stock_minimo=10)

    def test_lista_responde(self):
        respuesta = self.client.get(reverse('catalogo:lista'))
        self.assertEqual(respuesta.status_code, 200)
        self.assertContains(respuesta, 'Vestido Largo')
        self.assertContains(respuesta, 'Lino Natural')

    def test_busqueda_por_nombre_y_descripcion(self):
        respuesta = self.client.get(reverse('catalogo:lista'), {'q': 'vegetal'})
        self.assertContains(respuesta, 'Lino Natural')
        self.assertNotContains(respuesta, 'Vestido Largo')

    def test_filtro_por_tipo(self):
        respuesta = self.client.get(reverse('catalogo:lista'), {'tipo': 'TELA'})
        self.assertContains(respuesta, 'Lino Natural')
        self.assertNotContains(respuesta, 'Vestido Largo')

    def test_detalle_responde(self):
        respuesta = self.client.get(reverse('catalogo:detalle', args=[self.prenda.slug]))
        self.assertEqual(respuesta.status_code, 200)
        self.assertContains(respuesta, 'Vestido Largo')

    def test_detalle_slug_inexistente(self):
        respuesta = self.client.get(reverse('catalogo:detalle', args=['no-existe']))
        self.assertEqual(respuesta.status_code, 404)

    def test_inventario_responde(self):
        respuesta = self.client.get(reverse('catalogo:inventario'))
        self.assertEqual(respuesta.status_code, 200)
        self.assertContains(respuesta, 'Inventario de materiales')