"""Vistas de cuentas: acceso, registro, perfil, favoritos y colecciones."""

from django.contrib import messages
from django.contrib.auth import login
from django.contrib.auth.decorators import login_required
from django.contrib.auth.views import LoginView
from django.shortcuts import redirect, render
from django.urls import reverse
from django.utils.http import url_has_allowed_host_and_scheme
from django.views.decorators.http import require_POST

from core.permisos import cliente_requerido, es_administrador

from .forms import ColeccionForm, PerfilForm, RegistroForm
from .services import CuentasService

servicio_cuentas = CuentasService()

# Rutas reservadas al área administrativa: un cliente nunca debe aterrizar aquí.
RUTAS_ADMINISTRATIVAS = ('/panel/', '/admin/', '/informes/')


def _destino_seguro(request, destino):
    """Valida que ``destino`` sea una URL local y permitida para un cliente."""
    if not destino:
        return None
    permitido = url_has_allowed_host_and_scheme(
        destino, allowed_hosts={request.get_host()}, require_https=request.is_secure()
    )
    if not permitido:
        return None
    if destino.startswith(RUTAS_ADMINISTRATIVAS):
        return None
    return destino


class AccesoView(LoginView):
    """Acceso único para administradores y clientes.

    La redirección se decide según los permisos reales del usuario, nunca por
    su nombre: los administradores van al panel de ATELIER y los clientes a su
    dashboard privado. La URL de retorno (``next``) solo se respeta si es local
    y no apunta al área administrativa.
    """

    template_name = 'cuentas/login.html'
    redirect_authenticated_user = True

    def get_success_url(self):
        if es_administrador(self.request.user):
            return reverse('panel:inicio')
        return (
            _destino_seguro(self.request, self.get_redirect_url())
            or reverse('cuentas:panel')
        )


def registro(request):
    """Registro de usuario. Los nuevos clientes van a su dashboard privado."""
    if request.user.is_authenticated:
        if es_administrador(request.user):
            return redirect('panel:inicio')
        return redirect('cuentas:panel')

    destino = _destino_seguro(
        request, request.POST.get('next') or request.GET.get('next')
    )
    formulario = RegistroForm(request.POST or None)
    if request.method == 'POST' and formulario.is_valid():
        usuario = formulario.save()
        login(request, usuario)
        messages.success(
            request,
            'Tu cuenta está lista. Bienvenida a tu espacio de ATELIER.',
        )
        return redirect(destino or reverse('cuentas:panel'))

    return render(request, 'cuentas/registro.html', {'form': formulario, 'next': destino})


@cliente_requerido
def panel(request):
    """Dashboard privado del cliente: su espacio personal en ATELIER."""
    contexto = servicio_cuentas.panel_cliente(request.user)
    return render(request, 'cuentas/panel.html', contexto)


@login_required
def perfil(request):
    """Perfil del usuario con colecciones, favoritos, propuestas y preferencias."""
    datos = servicio_cuentas.datos_perfil(request.user)
    perfil = datos['perfil']
    formulario = PerfilForm(
        request.POST or None, request.FILES or None, instance=perfil
    )

    if request.method == 'POST' and formulario.is_valid():
        formulario.save()
        messages.success(request, 'Tu perfil se actualizó correctamente.')
        return redirect('cuentas:perfil')

    contexto = {
        'form': formulario,
        'perfil': perfil,
        **{clave: valor for clave, valor in datos.items() if clave != 'perfil'},
    }
    return render(request, 'cuentas/perfil.html', contexto)


@login_required
@require_POST
def favorito_alternar(request, producto_id):
    """Agrega o quita un producto de los favoritos del usuario."""
    producto, creado = servicio_cuentas.alternar_favorito(request.user, producto_id)
    if creado:
        messages.success(request, f'"{producto.nombre}" se añadió a tus favoritos.')
    else:
        messages.success(request, f'"{producto.nombre}" se quitó de tus favoritos.')

    destino = request.POST.get('next') or producto.get_absolute_url()
    return redirect(destino)


@login_required
def coleccion_nueva(request):
    """Pantalla de creación de una nueva colección."""
    formulario = ColeccionForm(request.POST or None, request.FILES or None)
    if request.method == 'POST' and formulario.is_valid():
        coleccion = servicio_cuentas.guardar_coleccion(request.user, formulario)
        messages.success(request, f'Colección "{coleccion.nombre}" creada.')
        return redirect('cuentas:coleccion_detalle', slug=coleccion.slug)
    return render(request, 'cuentas/coleccion_form.html', {'form': formulario})


def coleccion_detalle(request, slug):
    """Detalle de una colección. Las curadas por ATELIER son públicas."""
    contexto = servicio_cuentas.detalle_coleccion(request.user, slug)
    coleccion = contexto['coleccion']
    if coleccion.usuario and coleccion.usuario != request.user:
        messages.warning(request, 'No tienes acceso a esa colección.')
        return redirect('cuentas:perfil')

    return render(request, 'cuentas/coleccion_detalle.html', contexto)


@login_required
@require_POST
def coleccion_agregar_producto(request, slug):
    """Agrega un producto a una colección propia del usuario."""
    coleccion, producto = servicio_cuentas.agregar_producto(
        request.user, slug, request.POST.get('producto_id')
    )
    messages.success(request, f'"{producto.nombre}" se añadió a "{coleccion.nombre}".')
    return redirect(request.POST.get('next') or 'cuentas:coleccion_detalle', slug=coleccion.slug)


@login_required
@require_POST
def coleccion_quitar_producto(request, slug):
    """Quita un producto de una colección propia."""
    coleccion, producto = servicio_cuentas.quitar_producto(
        request.user, slug, request.POST.get('producto_id')
    )
    messages.success(request, f'"{producto.nombre}" se quitó de "{coleccion.nombre}".')
    return redirect('cuentas:coleccion_detalle', slug=coleccion.slug)


@login_required
@require_POST
def coleccion_eliminar(request, slug):
    """Elimina una colección del usuario."""
    nombre = servicio_cuentas.eliminar_coleccion(request.user, slug)
    messages.success(request, f'Colección "{nombre}" eliminada.')
    return redirect('cuentas:perfil')
