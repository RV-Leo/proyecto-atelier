"""Panel administrativo personalizado de ATELIER.

Vistas de solo administradores con indicadores reales, tablas de gestión y
accesos directos al panel de Django para crear, editar y eliminar registros.
"""

from django.contrib.auth import get_user_model
from django.db.models import Count, Sum
from django.shortcuts import render

from catalogo.models import Categoria, Material, Producto
from cuentas.models import Coleccion
from disenador.models import PropuestaDiseno
from tendencias.models import Tendencia

from .permisos import GRUPO_ADMINISTRADORES, administrador_requerido

Usuario = get_user_model()


@administrador_requerido
def inicio(request):
    """Panel principal: indicadores, tablas y accesos de gestión."""
    materiales = list(Material.objects.all())
    agotados = [m for m in materiales if m.agotado]
    stock_bajo = [m for m in materiales if m.stock_bajo and not m.agotado]

    total_usuarios = Usuario.objects.count()
    total_administradores = Usuario.objects.filter(
        groups__name='Administradores'
    ).distinct().count() + Usuario.objects.filter(is_superuser=True).count()
    total_clientes = max(total_usuarios - total_administradores, 0)

    indicadores = [
        {'etiqueta': 'Productos en catálogo', 'valor': Producto.objects.count(), 'icono': 'prenda'},
        {'etiqueta': 'Categorías', 'valor': Categoria.objects.count(), 'icono': 'categoria'},
        {'etiqueta': 'Materiales', 'valor': len(materiales), 'icono': 'inventario'},
        {'etiqueta': 'Stock bajo', 'valor': len(stock_bajo), 'icono': 'alerta'},
        {'etiqueta': 'Materiales agotados', 'valor': len(agotados), 'icono': 'alerta'},
        {'etiqueta': 'Tendencias activas', 'valor': Tendencia.objects.filter(activa=True).count(), 'icono': 'tendencia'},
        {'etiqueta': 'Propuestas de diseño', 'valor': PropuestaDiseno.objects.count(), 'icono': 'diseno'},
        {'etiqueta': 'Colecciones', 'valor': Coleccion.objects.count(), 'icono': 'coleccion'},
        {'etiqueta': 'Usuarios registrados', 'valor': total_usuarios, 'icono': 'usuario'},
        {'etiqueta': 'Clientes', 'valor': total_clientes, 'icono': 'usuario'},
    ]

    categorias = list(
        Categoria.objects.annotate(total=Count('productos')).order_by('-total', 'nombre')
    )
    max_categoria = max((c.total for c in categorias), default=1)
    grafico_categorias = [
        {'nombre': c.nombre, 'total': c.total, 'ancho': round(c.total / max_categoria * 100)}
        for c in categorias
    ]

    contexto = {
        'indicadores': indicadores,
        'grafico_categorias': grafico_categorias,
        'productos_recientes': Producto.objects.select_related('categoria').order_by('-creado')[:6],
        'categorias': categorias,
        'materiales_bajo': (stock_bajo + agotados)[:8],
        'total_stock_bajo': len(stock_bajo),
        'total_agotados': len(agotados),
        'tendencias': Tendencia.objects.order_by('-popularidad')[:6],
        'propuestas_recientes': (
            PropuestaDiseno.objects.select_related('usuario').order_by('-creada')[:6]
        ),
        'colecciones_recientes': (
            Coleccion.objects.select_related('usuario').order_by('-creada')[:6]
        ),
        'usuarios_recientes': Usuario.objects.order_by('-date_joined')[:6],
        'stock_materiales': Material.objects.aggregate(total=Sum('cantidad'))['total'] or 0,
    }
    return render(request, 'panel/inicio.html', contexto)


@administrador_requerido
def usuarios(request):
    """Listado de usuarios y su rol dentro de ATELIER."""
    lista = Usuario.objects.prefetch_related('groups').order_by('-date_joined')
    filas = []
    for usuario in lista:
        if usuario.is_superuser:
            rol, clase = 'Superusuario', 'rol-administrador'
        elif usuario.is_staff or usuario.groups.filter(name=GRUPO_ADMINISTRADORES).exists():
            rol, clase = 'Administrador', 'rol-administrador'
        else:
            rol, clase = 'Cliente', 'rol-cliente'
        filas.append({'usuario': usuario, 'rol': rol, 'clase': clase})

    contexto = {
        'filas': filas,
        'total': lista.count(),
    }
    return render(request, 'panel/usuarios.html', contexto)
