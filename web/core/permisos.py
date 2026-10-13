"""Permisos y roles de ATELIER.

El tipo de usuario no se deduce del nombre de cuenta: se determina a partir de
los grupos y permisos de Django. Un usuario con el grupo ``Administradores`` (o
un usuario de staff/superusuario) es administrador de la web; el resto son
clientes.
"""

from functools import wraps
from urllib.parse import quote

from django.core.exceptions import PermissionDenied
from django.shortcuts import redirect
from django.urls import reverse

GRUPO_ADMINISTRADORES = 'Administradores'
GRUPO_CLIENTES = 'Clientes'


def es_administrador(usuario):
    """Devuelve True si el usuario puede administrar la web de ATELIER."""
    if not usuario or not usuario.is_authenticated:
        return False
    if usuario.is_superuser or usuario.is_staff:
        return True
    return usuario.groups.filter(name=GRUPO_ADMINISTRADORES).exists()


def es_cliente(usuario):
    """Devuelve True para un usuario autenticado sin rol administrativo."""
    return bool(usuario and usuario.is_authenticated and not es_administrador(usuario))


def administrador_requerido(vista):
    """Restringe una vista a los administradores.

    - Usuario anónimo: se redirige al acceso con ``?next=``.
    - Usuario autenticado sin permisos: se rechaza con HTTP 403.
    """

    @wraps(vista)
    def _envoltorio(request, *args, **kwargs):
        if not request.user.is_authenticated:
            acceso = reverse('cuentas:acceder')
            return redirect(f'{acceso}?next={quote(request.get_full_path())}')
        if not es_administrador(request.user):
            raise PermissionDenied(
                'No tienes permisos para acceder al área administrativa de ATELIER.'
            )
        return vista(request, *args, **kwargs)

    return _envoltorio
