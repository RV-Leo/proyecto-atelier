"""Casos de uso de consulta y filtrado de tendencias."""

from .repositories import TendenciasRepository


class TendenciasService:
    def __init__(self, repository=None):
        self.repository = repository or TendenciasRepository()

    def lista(self, filtros):
        tendencias = self.repository.buscar(filtros)
        q, tipo, temporada = filtros.get('q', ''), filtros.get('tipo', ''), filtros.get('temporada', '')
        return {
            'tendencias': tendencias,
            'tipos': self.repository.tipos(),
            'temporadas': self.repository.temporadas(),
            'filtros': {'q': q, 'tipo': tipo, 'temporada': temporada},
        }
