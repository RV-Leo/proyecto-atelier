"""Pruebas de cuentas: autenticación, roles, privacidad, perfil y colecciones."""

from django.contrib.auth import authenticate, get_user_model
from django.core.management import call_command
from django.test import TestCase
from django.urls import reverse

from catalogo.models import Categoria, Producto
from core.permisos import GRUPO_ADMINISTRADORES, es_administrador
from disenador.models import PropuestaDiseno

from .models import Coleccion, Favorito, Perfil

Usuario = get_user_model()


class CuentasConfiguracionTests(TestCase):
    """Verifica las cuentas y permisos creados por configurar_cuentas."""

    @classmethod
    def setUpTestData(cls):
        call_command('configurar_cuentas', verbosity=0)

    def test_las_tres_cuentas_existen(self):
        for username in ('administracion', 'cliente', 'admin'):
            self.assertTrue(
                Usuario.objects.filter(username=username).exists(),
                f'Falta la cuenta {username}',
            )

    def test_contrasenas_funcionan(self):
        self.assertIsNotNone(authenticate(username='administracion', password='123456'))
        self.assertIsNotNone(authenticate(username='cliente', password='123456'))
        self.assertIsNotNone(authenticate(username='admin', password='admin'))

    def test_administracion_es_administrador_de_la_web(self):
        usuario = Usuario.objects.get(username='administracion')
        self.assertTrue(es_administrador(usuario))
        self.assertTrue(usuario.groups.filter(name=GRUPO_ADMINISTRADORES).exists())
        self.assertFalse(usuario.is_superuser)

    def test_cliente_no_tiene_rol_administrativo(self):
        cliente = Usuario.objects.get(username='cliente')
        self.assertFalse(es_administrador(cliente))
        self.assertFalse(cliente.is_staff)
        self.assertFalse(cliente.is_superuser)
        self.assertFalse(cliente.groups.filter(name=GRUPO_ADMINISTRADORES).exists())

    def test_admin_es_superusuario(self):
        self.assertTrue(Usuario.objects.get(username='admin').is_superuser)

    def test_comando_es_idempotente(self):
        call_command('configurar_cuentas', verbosity=0)
        call_command('configurar_cuentas', verbosity=0)
        self.assertEqual(Usuario.objects.filter(username='administracion').count(), 1)
        self.assertEqual(Usuario.objects.filter(username='cliente').count(), 1)
        self.assertEqual(Usuario.objects.filter(username='admin').count(), 1)


class RolesYAccesoTests(TestCase):
    """Redirecciones por permisos y protección de las vistas administrativas."""

    @classmethod
    def setUpTestData(cls):
        call_command('configurar_cuentas', verbosity=0)

    def test_administracion_inicia_sesion_y_va_al_panel(self):
        respuesta = self.client.post(
            reverse('cuentas:acceder'),
            {'username': 'administracion', 'password': '123456'},
        )
        self.assertRedirects(respuesta, reverse('panel:inicio'))

    def test_cliente_inicia_sesion_y_va_a_su_panel(self):
        respuesta = self.client.post(
            reverse('cuentas:acceder'),
            {'username': 'cliente', 'password': '123456'},
        )
        self.assertRedirects(respuesta, reverse('cuentas:panel'))

    def test_anonimo_no_accede_al_panel(self):
        respuesta = self.client.get(reverse('panel:inicio'))
        self.assertEqual(respuesta.status_code, 302)
        self.assertIn(reverse('cuentas:acceder'), respuesta.url)

    def test_cliente_no_accede_al_panel(self):
        self.client.login(username='cliente', password='123456')
        self.assertEqual(self.client.get(reverse('panel:inicio')).status_code, 403)
        self.assertEqual(self.client.get(reverse('panel:usuarios')).status_code, 403)

    def test_administracion_accede_al_panel(self):
        self.client.login(username='administracion', password='123456')
        respuesta = self.client.get(reverse('panel:inicio'))
        self.assertEqual(respuesta.status_code, 200)
        self.assertContains(respuesta, 'Panel administrativo')

    def test_administracion_accede_a_usuarios(self):
        self.client.login(username='administracion', password='123456')
        self.assertEqual(self.client.get(reverse('panel:usuarios')).status_code, 200)

    def test_informes_requieren_administrador(self):
        # Anónimo -> redirige al acceso.
        respuesta = self.client.get(reverse('informes:informes'))
        self.assertEqual(respuesta.status_code, 302)
        self.assertIn(reverse('cuentas:acceder'), respuesta.url)
        # Cliente -> 403.
        self.client.login(username='cliente', password='123456')
        self.assertEqual(self.client.get(reverse('informes:informes')).status_code, 403)
        # Administrador -> 200.
        self.client.logout()
        self.client.login(username='administracion', password='123456')
        self.assertEqual(self.client.get(reverse('informes:informes')).status_code, 200)

    def test_cerrar_sesion_finaliza_la_sesion(self):
        self.client.login(username='cliente', password='123456')
        respuesta = self.client.post(reverse('cuentas:salir'))
        self.assertEqual(respuesta.status_code, 302)
        self.assertNotIn('_auth_user_id', self.client.session)

    def test_admin_oficial_entra_en_django_admin(self):
        self.client.login(username='admin', password='admin')
        self.assertEqual(self.client.get(reverse('admin:index')).status_code, 200)

    def test_cliente_no_entra_en_django_admin(self):
        self.client.login(username='cliente', password='123456')
        respuesta = self.client.get(reverse('admin:index'))
        self.assertIn(respuesta.status_code, (302, 403))


class PrivacidadTests(TestCase):
    """Cada cliente solo accede a sus propios datos privados."""

    def setUp(self):
        self.duena = Usuario.objects.create_user('duena', password='clave-segura-123')
        self.otro = Usuario.objects.create_user('otro', password='clave-segura-123')
        self.coleccion = Coleccion.objects.create(nombre='Privada de Dueña', usuario=self.duena)
        self.propuesta = PropuestaDiseno.objects.create(usuario=self.duena, prompt='vestido privado')
        self.client.login(username='otro', password='clave-segura-123')

    def test_no_puede_ver_coleccion_privada_ajena(self):
        respuesta = self.client.get(
            reverse('cuentas:coleccion_detalle', args=[self.coleccion.slug])
        )
        self.assertEqual(respuesta.status_code, 302)
        self.assertEqual(respuesta.url, reverse('cuentas:perfil'))

    def test_no_puede_marcar_favorita_una_propuesta_ajena(self):
        respuesta = self.client.post(
            reverse('disenador:alternar_favorita', args=[self.propuesta.pk])
        )
        self.assertEqual(respuesta.status_code, 404)

    def test_historial_no_muestra_propuestas_ajenas(self):
        respuesta = self.client.get(reverse('disenador:historial'))
        self.assertEqual(respuesta.status_code, 200)
        self.assertNotContains(respuesta, 'vestido privado')


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
        self.assertRedirects(respuesta, reverse('cuentas:panel'))
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


class SeparacionDeExperienciasTests(TestCase):
    """Visitante, cliente y administrador ven experiencias claramente distintas."""

    @classmethod
    def setUpTestData(cls):
        call_command('configurar_cuentas', verbosity=0)

    def test_visitante_ve_portada_publica(self):
        respuesta = self.client.get(reverse('core:inicio'))
        self.assertEqual(respuesta.status_code, 200)
        self.assertContains(respuesta, 'Del hilo a la idea')
        self.assertContains(respuesta, 'Iniciar sesión')
        self.assertContains(respuesta, 'Crear cuenta')

    def test_cliente_no_ve_la_portada_sino_su_dashboard(self):
        self.client.login(username='cliente', password='123456')
        self.assertRedirects(
            self.client.get(reverse('core:inicio')), reverse('cuentas:panel')
        )
        panel = self.client.get(reverse('cuentas:panel'))
        self.assertEqual(panel.status_code, 200)
        self.assertContains(panel, 'Mi espacio privado')
        # La experiencia del cliente no mezcla herramientas administrativas.
        self.assertNotContains(panel, 'Panel administrativo')
        self.assertNotContains(panel, 'Django Admin')

    def test_administrador_no_ve_el_dashboard_de_cliente(self):
        self.client.login(username='administracion', password='123456')
        self.assertRedirects(
            self.client.get(reverse('cuentas:panel')), reverse('panel:inicio')
        )

    def test_administrador_es_enviado_al_panel_desde_inicio(self):
        self.client.login(username='administracion', password='123456')
        self.assertRedirects(
            self.client.get(reverse('core:inicio')), reverse('panel:inicio')
        )

    def test_visitante_es_enviado_al_acceso_con_destino(self):
        respuesta = self.client.get(reverse('cuentas:panel'))
        self.assertEqual(respuesta.status_code, 302)
        self.assertIn(reverse('cuentas:acceder'), respuesta.url)
        self.assertIn('next=', respuesta.url)

    def test_login_ignora_destino_administrativo_para_cliente(self):
        respuesta = self.client.post(
            f"{reverse('cuentas:acceder')}?next={reverse('panel:inicio')}",
            {'username': 'cliente', 'password': '123456'},
        )
        self.assertRedirects(respuesta, reverse('cuentas:panel'))

    def test_login_ignora_destino_externo(self):
        respuesta = self.client.post(
            f"{reverse('cuentas:acceder')}?next=https://sitio-falso.example/",
            {'username': 'cliente', 'password': '123456'},
        )
        self.assertRedirects(respuesta, reverse('cuentas:panel'))

    def test_login_muestra_aviso_amigable_con_destino(self):
        respuesta = self.client.get(
            f"{reverse('cuentas:acceder')}?next={reverse('disenador:historial')}"
        )
        self.assertContains(respuesta, 'Tu próximo diseño empieza aquí')

    def test_registro_conserva_destino_seguro(self):
        respuesta = self.client.post(
            reverse('cuentas:registro'),
            {
                'username': 'connext',
                'email': 'connext@atelier.local',
                'password1': 'clave-muy-segura-123',
                'password2': 'clave-muy-segura-123',
                'next': reverse('disenador:historial'),
            },
        )
        self.assertRedirects(respuesta, reverse('disenador:historial'))