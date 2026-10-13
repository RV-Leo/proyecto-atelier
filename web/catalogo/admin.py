"""Configuración de Django Admin para el catálogo."""

from django.contrib import admin

from .models import Categoria, Material, Producto


@admin.register(Categoria)
class CategoriaAdmin(admin.ModelAdmin):
    list_display = ['nombre', 'slug', 'orden', 'cantidad_productos']
    list_editable = ['orden']
    search_fields = ['nombre', 'descripcion']
    prepopulated_fields = {'slug': ('nombre',)}

    @admin.display(description='Productos')
    def cantidad_productos(self, objeto):
        return objeto.productos.count()


@admin.register(Producto)
class ProductoAdmin(admin.ModelAdmin):
    list_display = [
        'nombre', 'tipo', 'categoria', 'precio', 'unidades', 'disponible',
        'estado', 'es_destacado', 'es_tendencia', 'es_nuevo', 'actualizado',
    ]
    list_filter = ['tipo', 'categoria', 'temporada', 'es_destacado', 'es_tendencia', 'es_nuevo']
    search_fields = ['nombre', 'descripcion', 'color', 'composicion', 'estilo']
    prepopulated_fields = {'slug': ('nombre',)}
    list_editable = ['precio', 'unidades', 'es_destacado', 'es_tendencia', 'es_nuevo']
    fieldsets = (
        ('Información del producto', {
            'fields': (
                'nombre', 'slug', 'descripcion', 'categoria', 'tipo',
                'estilo', 'temporada', 'color', 'composicion',
            ),
        }),
        ('Precio e inventario', {
            'fields': ('precio', 'unidades', 'imagen'),
        }),
        ('Estados visuales', {
            'fields': ('es_destacado', 'es_tendencia', 'es_nuevo'),
        }),
    )

    @admin.display(boolean=True, description='Disponible')
    def disponible(self, objeto):
        return objeto.disponible

    @admin.display(description='Estado')
    def estado(self, objeto):
        return objeto.estado or 'Regular'


@admin.register(Material)
class MaterialAdmin(admin.ModelAdmin):
    list_display = [
        'nombre', 'categoria', 'color', 'tipo_tela', 'cantidad',
        'unidad_medida', 'stock_minimo', 'estado',
    ]
    list_filter = ['categoria', 'unidad_medida', 'tipo_tela']
    search_fields = ['nombre', 'categoria', 'color', 'composicion', 'ubicacion']
    prepopulated_fields = {'slug': ('nombre',)}
    list_editable = ['cantidad']

    @admin.display(description='Estado')
    def estado(self, objeto):
        return objeto.estado