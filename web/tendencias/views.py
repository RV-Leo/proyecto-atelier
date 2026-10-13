"""Vistas de tendencias textiles de ATELIER."""

from django.db.models import Q
from django.shortcuts import render

from .models import Tendencia


def lista(request):
    """Tendencias con filtros por tipo, temporada y búsqueda."""
    tendencias = Tendencia.objects.filter(activa=True)
    buscador = request.GET
    q = buscador.get('q', '').strip()
    tipo = buscador.get('tipo', '').strip()
    temporada = buscador.get('temporada', '').strip()

    if q:
        tendencias = tendencias.filter(
            Q(nombre__icontains=q) | Q(descripcion__icontains=q)
        )
    if tipo:
        tendencias = tendencias.filter(tipo=tipo)
    if temporada:
        tendencias = tendencias.filter(temporada__icontains=temporada)

    temporadas = list(
        Tendencia.objects.exclude(temporada='').values_list('temporada', flat=True)
        .distinct().order_by('temporada')
    )

    contexto = {
        'tendencias': tendencias,
        'tipos': Tendencia.Tipo.choices,
        'temporadas': temporadas,
        'filtros': {'q': q, 'tipo': tipo, 'temporada': temporada},
    }
    return render(request, 'tendencias/lista.html', contexto)