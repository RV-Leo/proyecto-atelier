"""Pruebas de cuentas: registro, perfil, favoritos y colecciones."""

from django.contrib.auth import get_user_model
from django.test import TestCase
from django.urls import reverse

from catalogo.models import Categoria, Producto

from .models import Coleccion, Favorito, Perfil

Usuario = get_user_model()


class RegistroTests(TestCase):
    def test_get_registro(self):
        respuesta = self.client.get(reverse('cuentas:registro'))
        self.assertEqual(respuesta.status_code, 200)

    def test_registro_crea_usuario_perfil_e_inicia_sesion(self):
        respuesta = self.client.post(reverse('cuentas:registro'), {
            'username': 'nueva',
            'email': 'nueva@atelier.local',
            'password1': 'una-clave-muy-segura-9',
            'password2': 'una-clave-muy-segura-9',
        })
        self.assertRedirects(respuesta, reverse('cuentas:perfil'))
        usuario = Usuario.objects.get(username='nueva')
        self.assertTrue(Perfil.objects.filter(usuario=usuario).exists())
        self.assertEqual(int(self.client.session['_auth_user_id']), usuario.pk)

    def test_email_duplicado_falla(self):
        Usuario.objects.create_user('una', email='x@y.com', password='clave-segura-123')
        respuesta = self.client.post(reverse('cuentas:registro'), {
            'username': 'otra', 'email': 'X@Y.com',
            'password1': 'otra-clave-segura-9', 'password2': 'otra-clave-segura-9',
        })
        self.assertEqual(respuesta.status_code, 200)
        self.assertFalse(Usuario.objects.filter(username='otra').exists())


class PerfilTests(TestCase):
    def setUp(self):
        self.usuario = Usuario.objects.create_user('ana', password='clave-segura-123')

    def test_perfil_requiere_sesion(self):
        respuesta = self.client.get(reverse('cuentas:perfil'))
        self.assertRedirects(respuesta, f'{reverse("cuentas:acceder")}?next={reverse("cuentas:perfil")}')

    def test_perfil_autenticado(self):
        self.client.login(username='ana', password='clave-segura-123')
        respuesta = self.client.get(reverse('cuentas:perfil'))
        self.assertEqual(respuesta.status_code, 200)

    def test_actualizar_perfil(self):
        self.client.login(username='ana', password='clave-segura-123')
        respuesta = self.client.post(reverse('cuentas:perfil'), {
            'nombre_artistico': 'Ana Diseño',
            'bio': 'Diseñadora textil',
            'ciudad': 'Quito',
            'preferencias': 'telas naturales',
            'acepta_comunicaciones': 'on',
        })
        self.assertRedirects(respuesta, reverse('cuentas:perfil'))
        self.assertEqual(Perfil.objects.get(usuario=self.usuario).nombre_artistico, 'Ana Diseño')


class FavoritoTests(TestCase):
    def setUp(self):
        self.usuario = Usuario.objects.create_user('leo', password='clave-segura-123')
        self.categoria = Categoria.objects.create(nombre='Vestidos')
        self.producto = Producto.objects.create(
            nombre='Vestido', descripcion='x', categoria=self.categoria, precio='10.00', unidades=3,
        )

    def test_favorito_requiere_sesion(self):
        respuesta = self.client.post(reverse('cuentas:favorito_alternar', args=[self.producto.pk]))
        self.assertEqual(respuesta.status_code, 302)
        self.assertIn(reverse('cuentas:acceder'), respuesta.url)

    def test_favorito_alterna(self):
        self.client.login(username='leo', password='clave-segura-123')
        self.client.post(reverse('cuentas:favorito_alternar', args=[self.producto.pk]))
        self.assertTrue(Favorito.objects.filter(usuario=self.usuario, producto=self.producto).exists())
        self.client.post(reverse('cuentas:favorito_alternar', args=[self.producto.pk]))
        self.assertFalse(Favorito.objects.filter(usuario=self.usuario, producto=self.producto).exists())


class ColeccionTests(TestCase):
    def setUp(self):
        self.usuario = Usuario.objects.create_user('sofia', password='clave-segura-123')
        self.categoria = Categoria.objects.create(nombre='Vestidos')
        self.producto = Producto.objects.create(
            nombre='Vestido', descripcion='x', categoria=self.categoria, precio='10.00', unidades=3,
        )
        self.client.login(username='sofia', password='clave-segura-123')

    def test_crear_coleccion(self):
        respuesta = self.client.post(reverse('cuentas:coleccion_nueva'), {
            'nombre': 'Mi cápsula', 'descripcion': 'prendas de verano',
        })
        coleccion = Coleccion.objects.get(nombre='Mi cápsula')
        self.assertEqual(coleccion.usuario, self.usuario)
        self.assertRedirects(respuesta, reverse('cuentas:coleccion_detalle', args=[coleccion.slug]))

    def test_agregar_y_quitar_producto(self):
        coleccion = Coleccion.objects.create(nombre='Cápsula', usuario=self.usuario)
        self.client.post(
            reverse('cuentas:coleccion_agregar_producto', args=[coleccion.slug]),
            {'producto_id': self.producto.pk},
        )
        self.assertIn(self.producto, coleccion.productos.all())
        self.client.post(
            reverse('cuentas:coleccion_quitar_producto', args=[coleccion.slug]),
            {'producto_id': self.producto.pk},
        )
        self.assertNotIn(self.producto, coleccion.productos.all())

    def test_no_se_puede_tocar_coleccion_ajena(self):
        otra = Usuario.objects.create_user('intrusa', password='clave-segura-123')
        coleccion = Coleccion.objects.create(nombre='Ajena', usuario=otra)
        respuesta = self.client.post(
            reverse('cuentas:coleccion_agregar_producto', args=[coleccion.slug]),
            {'producto_id': self.producto.pk},
        )
        self.assertEqual(respuesta.status_code, 404)

    def test_eliminar_coleccion(self):
        coleccion = Coleccion.objects.create(nombre='Borrar', usuario=self.usuario)
        self.client.post(reverse('cuentas:coleccion_eliminar', args=[coleccion.slug]))
        self.assertFalse(Coleccion.objects.filter(pk=coleccion.pk).exists())