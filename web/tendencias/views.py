"""Vistas de tendencias textiles de ATELIER."""

from django.shortcuts import render

from .services import TendenciasService

servicio_tendencias = TendenciasService()


def lista(request):
    """Tendencias con filtros por tipo, temporada y búsqueda."""
    contexto = servicio_tendencias.lista({
        clave: request.GET.get(clave, '').strip()
        for clave in ('q', 'tipo', 'temporada')
    })
    return render(request, 'tendencias/lista.html', contexto)
