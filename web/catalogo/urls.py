"""Rutas del catálogo de ATELIER."""

from django.urls import path

from . import views

app_name = 'catalogo'

urlpatterns = [
    path('inventario/', views.inventario, name='inventario'),
    path('producto/<slug:slug>/', views.detalle, name='detalle'),
    path('', views.lista, name='lista'),
]