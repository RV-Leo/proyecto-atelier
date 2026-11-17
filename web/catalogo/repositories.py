"""Acceso a datos del catálogo e inventario."""

from django.db.models import F, Q
from django.shortcuts import get_object_or_404

from .models import Categoria, Material, Producto


class CatalogoRepository:
    """Centraliza las consultas ORM del módulo de catálogo."""

    def productos(self):
        return Producto.objects.all()

    def productos_filtrados(self, filtros):
        productos = self.productos()
        if filtros.get('q'):
            productos = productos.filter(
                Q(nombre__icontains=filtros['q']) | Q(descripcion__icontains=filtros['q'])
            )
        if filtros.get('categoria'):
            productos = productos.filter(categoria__slug=filtros['categoria'])
        if filtros.get('tipo'):
            productos = productos.filter(tipo=filtros['tipo'])
        if filtros.get('temporada'):
            productos = productos.filter(temporada=filtros['temporada'])
        if filtros.get('estilo'):
            productos = productos.filter(estilo__iexact=filtros['estilo'])
        return productos

    def producto_por_slug(self, slug):
        return get_object_or_404(Producto, slug=slug)

    def categorias(self):
        return Categoria.objects.all()

    def categoria_por_slug(self, slug):
        return get_object_or_404(Categoria, slug=slug)

    def estilos(self):
        return list(
            Producto.objects.exclude(estilo='').values_list('estilo', flat=True)
            .distinct().order_by('estilo')
        )

    def relacionados(self, producto, limite=4):
        return Producto.objects.filter(categoria=producto.categoria).exclude(
            pk=producto.pk
        )[:limite]

    def es_favorito(self, usuario, producto):
        return usuario.favoritos.filter(producto=producto).exists()

    def materiales(self, filtros):
        materiales = Material.objects.all()
        q = filtros.get('q', '')
        if q:
            materiales = materiales.filter(
                Q(nombre__icontains=q) | Q(categoria__icontains=q)
                | Q(color__icontains=q) | Q(tipo_tela__icontains=q)
            )
        estado = filtros.get('estado', '')
        if estado == 'agotado':
            materiales = materiales.filter(cantidad__lte=0)
        elif estado == 'bajo':
            materiales = materiales.filter(cantidad__gt=0, cantidad__lte=F('stock_minimo'))
        elif estado == 'disponible':
            materiales = materiales.filter(cantidad__gt=0)
        return materiales

    def categorias_materiales(self):
        return list(
            Material.objects.values_list('categoria', flat=True).distinct()
            .order_by('categoria')
        )

    def totales_inventario(self):
        return {
            'disponible': Material.objects.filter(cantidad__gt=0).count(),
            'bajo': Material.objects.filter(
                cantidad__gt=0, cantidad__lte=F('stock_minimo')
            ).count(),
            'agotado': Material.objects.filter(cantidad__lte=0).count(),
        }

    def materiales_disponibles(self):
        return Material.objects.filter(cantidad__gt=0)

    def existen_materiales_agotados(self):
        return Material.objects.filter(cantidad__lte=0).exists()
