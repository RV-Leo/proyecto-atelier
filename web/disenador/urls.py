"""Rutas del asistente de diseño con IA."""

from django.urls import path

from . import views

app_name = 'disenador'

urlpatterns = [
    path('', views.asistente, name='asistente'),
    path('historial/', views.historial, name='historial'),
    path('propuesta/<int:pk>/favorita/', views.alternar_favorita, name='alternar_favorita'),
]