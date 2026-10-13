"""Rutas de la aplicación core: inicio y estadísticas."""

from django.urls import path

from . import views

app_name = 'core'

urlpatterns = [
    path('', views.inicio, name='inicio'),
]