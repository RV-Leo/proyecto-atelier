"""Vistas de la aplicación core: inicio y estadísticas del proyecto."""

from django.shortcuts import redirect, render

from .permisos import administrador_requerido, es_administrador
from .services import CoreService

servicio_core = CoreService()


def inicio(request):
    """Portada pública de ATELIER.

    Cada estado de sesión recibe una experiencia distinta:

      · Visitante  -> portada pública de presentación.
      · Cliente    -> su dashboard privado (``cuentas:panel``).
      - Administrador: su panel administrativo (``panel:inicio``).
    """
    if es_administrador(request.user):
        return redirect('panel:inicio')
    if request.user.is_authenticated:
        return redirect('cuentas:panel')

    return render(request, 'core/inicio.html', servicio_core.portada())


@administrador_requerido
def informes(request):
    """Estadísticas reales del proyecto a partir de la base de datos."""
    return render(request, 'core/informes.html', servicio_core.informes())
