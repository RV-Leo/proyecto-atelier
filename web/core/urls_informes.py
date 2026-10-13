"""Rutas del módulo de informes y estadísticas."""

from django.urls import path

from . import views

app_name = 'informes'

urlpatterns = [
    path('', views.informes, name='informes'),
]