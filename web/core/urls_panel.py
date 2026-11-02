"""Rutas del panel administrativo personalizado de ATELIER."""

from django.urls import path

from . import panel

app_name = 'panel'

urlpatterns = [
    path('', panel.inicio, name='inicio'),
    path('usuarios/', panel.usuarios, name='usuarios'),
]
