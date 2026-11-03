"""Casos de uso del catálogo e inventario."""

from django.core.paginator import Paginator

from .repositories import CatalogoRepository


class CatalogoService:
    """Aplica filtros y prepara los datos que consume la interfaz."""

    def __init__(self, repository=None):
        self.repository = repository or CatalogoRepository()

    def lista(self, filtros, numero_pagina, por_pagina=12):
        productos = self.repository.productos_filtrados(filtros)
        categoria = filtros.get('categoria', '')

        categoria_activa = None
        if categoria:
            categoria_activa = self.repository.categoria_por_slug(categoria)
        pagina = Paginator(productos, por_pagina).get_page(numero_pagina)
        inicio = (pagina.number - 1) * por_pagina + 1
        fin = min(pagina.number * por_pagina, pagina.paginator.count)
        resumen = (
            {'inicio': inicio, 'fin': fin, 'total': pagina.paginator.count}
            if pagina.has_other_pages() else None
        )
        return {
            'pagina': pagina,
            'categorias': self.repository.categorias(),
            'categoria_activa': categoria_activa,
            'estilos': self.repository.estilos(),
            'filtros': {
                'q': filtros.get('q', ''),
                'tipo': filtros.get('tipo', ''),
                'temporada': filtros.get('temporada', ''),
                'estilo': filtros.get('estilo', ''),
                'categoria': categoria,
            },
            'resumen': resumen,
        }

    def detalle(self, slug, usuario):
        producto = self.repository.producto_por_slug(slug)
        es_favorito = (
            self.repository.es_favorito(usuario, producto)
            if usuario.is_authenticated else False
        )
        return {
            'producto': producto,
            'relacionados': self.repository.relacionados(producto),
            'es_favorito': es_favorito,
        }

    def inventario(self, filtros):
        materiales = list(self.repository.materiales(filtros))
        return {
            'materiales': materiales,
            'categorias': self.repository.categorias_materiales(),
            'estado_activo': filtros.get('estado', ''),
            'q': filtros.get('q', ''),
            'totales': self.repository.totales_inventario(),
            'total': len(materiales),
        }
