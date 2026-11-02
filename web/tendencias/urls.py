"""Rutas del módulo de tendencias."""

from django.urls import path

from . import views

app_name = 'tendencias'

urlpatterns = [
    path('', views.lista, name='lista'),
]