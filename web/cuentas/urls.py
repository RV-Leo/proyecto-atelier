"""Rutas de cuentas: autenticación, perfil, favoritos y colecciones."""

from django.contrib.auth import views as auth_views
from django.urls import path

from . import views

app_name = 'cuentas'

urlpatterns = [
    path('acceder/', auth_views.LoginView.as_view(template_name='cuentas/login.html'), name='acceder'),
    path('salir/', auth_views.LogoutView.as_view(), name='salir'),
    path('registro/', views.registro, name='registro'),
    path('perfil/', views.perfil, name='perfil'),
    path('favorito/<int:producto_id>/alternar/', views.favorito_alternar, name='favorito_alternar'),
    path('colecciones/nueva/', views.coleccion_nueva, name='coleccion_nueva'),
    path('colecciones/<slug:slug>/', views.coleccion_detalle, name='coleccion_detalle'),
    path(
        'colecciones/<slug:slug>/agregar-producto/',
        views.coleccion_agregar_producto,
        name='coleccion_agregar_producto',
    ),
    path(
        'colecciones/<slug:slug>/quitar-producto/',
        views.coleccion_quitar_producto,
        name='coleccion_quitar_producto',
    ),
    path('colecciones/<slug:slug>/eliminar/', views.coleccion_eliminar, name='coleccion_eliminar'),
]