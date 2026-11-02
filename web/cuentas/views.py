"""Vistas de cuentas: acceso, registro, perfil, favoritos y colecciones."""

from django.contrib import messages
from django.contrib.auth import login
from django.contrib.auth.decorators import login_required
from django.contrib.auth.views import LoginView
from django.shortcuts import get_object_or_404, redirect, render
from django.urls import reverse
from django.utils.http import url_has_allowed_host_and_scheme
from django.views.decorators.http import require_POST

from catalogo.models import Producto
from core.permisos import cliente_requerido, es_administrador
from disenador.models import PropuestaDiseno
from tendencias.models import Tendencia

from .forms import ColeccionForm, PerfilForm, RegistroForm
from .models import Coleccion, Favorito, Perfil

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
    usuario = request.user
    perfil = Perfil.objects.filter(usuario=usuario).first()

    colecciones = Coleccion.objects.filter(usuario=usuario).order_by('-creada')
    favoritas = PropuestaDiseno.objects.filter(usuario=usuario, es_favorita=True)[:6]
    propuestas = PropuestaDiseno.objects.filter(usuario=usuario)[:6]
    favoritos = Favorito.objects.filter(usuario=usuario).select_related('producto')[:6]

    recomendados = list(Producto.objects.filter(es_destacado=True)[:4])
    if not recomendados:
        recomendados = list(Producto.objects.order_by('-creado')[:4])
    tendencias = Tendencia.objects.filter(activa=True).order_by('-popularidad')[:6]

    nombre = (
        perfil.nombre_artistico
        if perfil and perfil.nombre_artistico
        else usuario.get_short_name() or usuario.get_username()
    )

    contexto = {
        'perfil': perfil,
        'nombre': nombre,
        'colecciones': colecciones,
        'total_colecciones': colecciones.count(),
        'favoritas': favoritas,
        'total_favoritas': PropuestaDiseno.objects.filter(
            usuario=usuario, es_favorita=True
        ).count(),
        'propuestas': propuestas,
        'total_propuestas': PropuestaDiseno.objects.filter(usuario=usuario).count(),
        'favoritos': favoritos,
        'total_favoritos': Favorito.objects.filter(usuario=usuario).count(),
        'recomendados': recomendados,
        'tendencias': tendencias,
    }
    return render(request, 'cuentas/panel.html', contexto)


@login_required
def perfil(request):
    """Perfil del usuario con colecciones, favoritos, propuestas y preferencias."""
    perfil, _ = Perfil.objects.get_or_create(usuario=request.user)
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
        'colecciones': request.user.colecciones.all(),
        'favoritos': request.user.favoritos.select_related('producto'),
        'propuestas': request.user.propuestas.all()[:4],
        'total_propuestas': request.user.propuestas.count(),
    }
    return render(request, 'cuentas/perfil.html', contexto)


@login_required
@require_POST
def favorito_alternar(request, producto_id):
    """Agrega o quita un producto de los favoritos del usuario."""
    producto = get_object_or_404(Producto, pk=producto_id)
    favorito, creado = Favorito.objects.get_or_create(
        usuario=request.user, producto=producto
    )
    if creado:
        messages.success(request, f'"{producto.nombre}" se añadió a tus favoritos.')
    else:
        favorito.delete()
        messages.success(request, f'"{producto.nombre}" se quitó de tus favoritos.')

    destino = request.POST.get('next') or producto.get_absolute_url()
    return redirect(destino)


@login_required
def coleccion_nueva(request):
    """Pantalla de creación de una nueva colección."""
    formulario = ColeccionForm(request.POST or None, request.FILES or None)
    if request.method == 'POST' and formulario.is_valid():
        coleccion = formulario.save(commit=False)
        coleccion.usuario = request.user
        coleccion.save()
        messages.success(request, f'Colección "{coleccion.nombre}" creada.')
        return redirect('cuentas:coleccion_detalle', slug=coleccion.slug)
    return render(request, 'cuentas/coleccion_form.html', {'form': formulario})


def coleccion_detalle(request, slug):
    """Detalle de una colección. Las curadas por ATELIER son públicas."""
    coleccion = get_object_or_404(
        Coleccion.objects.prefetch_related('productos'), slug=slug
    )
    if coleccion.usuario and coleccion.usuario != request.user:
        messages.warning(request, 'No tienes acceso a esa colección.')
        return redirect('cuentas:perfil')

    es_propia = coleccion.usuario == request.user
    contexto = {
        'coleccion': coleccion,
        'es_propia': es_propia,
        'productos_todos': Producto.objects.all() if es_propia else Producto.objects.none(),
    }
    return render(request, 'cuentas/coleccion_detalle.html', contexto)


@login_required
@require_POST
def coleccion_agregar_producto(request, slug):
    """Agrega un producto a una colección propia del usuario."""
    coleccion = get_object_or_404(Coleccion, slug=slug, usuario=request.user)
    producto = get_object_or_404(Producto, pk=request.POST.get('producto_id'))
    coleccion.productos.add(producto)
    messages.success(request, f'"{producto.nombre}" se añadió a "{coleccion.nombre}".')
    return redirect(request.POST.get('next') or 'cuentas:coleccion_detalle', slug=coleccion.slug)


@login_required
@require_POST
def coleccion_quitar_producto(request, slug):
    """Quita un producto de una colección propia."""
    coleccion = get_object_or_404(Coleccion, slug=slug, usuario=request.user)
    producto = get_object_or_404(Producto, pk=request.POST.get('producto_id'))
    coleccion.productos.remove(producto)
    messages.success(request, f'"{producto.nombre}" se quitó de "{coleccion.nombre}".')
    return redirect('cuentas:coleccion_detalle', slug=coleccion.slug)


@login_required
@require_POST
def coleccion_eliminar(request, slug):
    """Elimina una colección del usuario."""
    coleccion = get_object_or_404(Coleccion, slug=slug, usuario=request.user)
    nombre = coleccion.nombre
    coleccion.delete()
    messages.success(request, f'Colección "{nombre}" eliminada.')
    return redirect('cuentas:perfil')