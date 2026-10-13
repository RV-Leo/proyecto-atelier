"""Configuración de Django Admin para tendencias."""

from django.contrib import admin

from .models import Tendencia


@admin.register(Tendencia)
class TendenciaAdmin(admin.ModelAdmin):
    list_display = [
        'nombre', 'tipo', 'temporada', 'popularidad', 'color', 'activa', 'orden',
    ]
    list_filter = ['tipo', 'temporada', 'activa']
    search_fields = ['nombre', 'descripcion']
    prepopulated_fields = {'slug': ('nombre',)}
    list_editable = ['popularidad', 'activa', 'orden']