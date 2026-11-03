"""Vistas del catálogo de ATELIER: listado, búsqueda, filtros y detalle."""

from django.contrib.auth.decorators import login_required
from django.shortcuts import render

from .services import CatalogoService

servicio_catalogo = CatalogoService()


def lista(request):
    """Catálogo con búsqueda por nombre/descripción y filtros."""
    context = servicio_catalogo.lista(
        {clave: request.GET.get(clave, '').strip() for clave in
         ('q', 'categoria', 'tipo', 'temporada', 'estilo')},
        request.GET.get('pagina'),
    )
    return render(request, 'catalogo/lista.html', context)


def detalle(request, slug):
    """Detalle de un producto con relacionados y estado de favorito."""
    context = servicio_catalogo.detalle(slug, request.user)
    return render(request, 'catalogo/detalle.html', context)


@login_required
def inventario(request):
    """Inventario de materiales y telas con estado del stock."""
    context = servicio_catalogo.inventario({
        'estado': request.GET.get('estado', '').strip(),
        'q': request.GET.get('q', '').strip(),
    })
    return render(request, 'catalogo/inventario.html', context)
