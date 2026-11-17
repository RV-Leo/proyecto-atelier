"""Controladores HTTP del panel administrativo de ATELIER."""

from django.shortcuts import render

from .permisos import administrador_requerido
from .services import CoreService

servicio_core = CoreService()


@administrador_requerido
def inicio(request):
    """Muestra el panel con indicadores y registros recientes."""
    return render(request, 'panel/inicio.html', servicio_core.panel_administrativo())


@administrador_requerido
def usuarios(request):
    """Muestra las cuentas registradas y sus roles."""
    return render(request, 'panel/usuarios.html', servicio_core.usuarios_administrativos())
