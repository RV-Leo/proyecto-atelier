"""Configuración de Django Admin para cuentas (perfil, colecciones, favoritos)."""

from django.contrib import admin

from .models import Coleccion, Favorito, Perfil


@admin.register(Perfil)
class PerfilAdmin(admin.ModelAdmin):
    list_display = ['usuario', 'nombre_artistico', 'ciudad', 'acepta_comunicaciones']
    search_fields = ['usuario__username', 'usuario__email', 'nombre_artistico', 'ciudad']
    list_filter = ['acepta_comunicaciones', 'ciudad']


@admin.register(Coleccion)
class ColeccionAdmin(admin.ModelAdmin):
    list_display = ['nombre', 'usuario', 'es_recomendada', 'cantidad_productos', 'creada']
    list_filter = ['es_recomendada', 'creada']
    search_fields = ['nombre', 'descripcion', 'usuario__username']
    filter_horizontal = ['productos']
    list_editable = ['es_recomendada']

    @admin.display(description='Productos')
    def cantidad_productos(self, objeto):
        return objeto.productos.count()


@admin.register(Favorito)
class FavoritoAdmin(admin.ModelAdmin):
    list_display = ['usuario', 'producto', 'creada']
    search_fields = ['usuario__username', 'producto__nombre']
    list_filter = ['creada']