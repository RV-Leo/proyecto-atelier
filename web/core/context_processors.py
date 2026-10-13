"""Context processors de ATELIER.

Expone el rol del usuario para que las plantillas adapten la navegación. La
seguridad real se aplica en el servidor; el menú es solo una mejora visual.
"""

from .permisos import es_administrador


def roles(request):
    usuario = getattr(request, 'user', None)
    return {
        'es_administrador': es_administrador(usuario),
        'es_cliente': bool(
            usuario and usuario.is_authenticated and not es_administrador(usuario)
        ),
    }
