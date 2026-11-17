"""Acceso a datos de tendencias."""

from .models import Tendencia
from django.db.models import Q


class TendenciasRepository:
    def activas(self):
        return Tendencia.objects.filter(activa=True)

    def temporadas(self):
        return list(
            Tendencia.objects.exclude(temporada='').values_list('temporada', flat=True)
            .distinct().order_by('temporada')
        )

    def buscar(self, filtros):
        tendencias = self.activas()
        if filtros.get('q'):
            tendencias = tendencias.filter(
                Q(nombre__icontains=filtros['q'])
                | Q(descripcion__icontains=filtros['q'])
            )
        if filtros.get('tipo'):
            tendencias = tendencias.filter(tipo=filtros['tipo'])
        if filtros.get('temporada'):
            tendencias = tendencias.filter(temporada__icontains=filtros['temporada'])
        return tendencias

    def tipos(self):
        return Tendencia.Tipo.choices
