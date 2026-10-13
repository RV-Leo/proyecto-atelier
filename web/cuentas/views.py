"""Vistas de cuentas: registro, perfil, favoritos y colecciones."""

from django.contrib import messages
from django.contrib.auth import login
from django.contrib.auth.decorators import login_required
from django.shortcuts import get_object_or_404, redirect, render
from django.views.decorators.http import require_POST

from catalogo.models import Producto

from .forms import ColeccionForm, PerfilForm, RegistroForm
from .models import Coleccion, Favorito, Perfil


def registro(request):
    """Registro de usuario. Los usuarios registrados van directo a su perfil."""
    if request.user.is_authenticated:
        return redirect('cuentas:perfil')

    formulario = RegistroForm(request.POST or None)
    if request.method == 'POST' and formulario.is_valid():
        usuario = formulario.save()
        login(request, usuario)
        messages.success(request, f'¡Bienvenida, {usuario.username}! Tu perfil está listo.')
        return redirect('cuentas:perfil')

    return render(request, 'cuentas/registro.html', {'form': formulario})


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