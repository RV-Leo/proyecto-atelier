"""Vistas de la aplicación core: inicio y estadísticas del proyecto."""

from django.contrib import messages
from django.db.models import Count, Sum
from django.shortcuts import redirect, render

from catalogo.models import Categoria, Material, Producto
from cuentas.models import Coleccion
from disenador.models import PropuestaDiseno
from tendencias.models import Tendencia

from .permisos import administrador_requerido, es_administrador


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

    destacados = list(Producto.objects.filter(es_destacado=True)[:8])
    nuevas = list(Producto.objects.filter(es_nuevo=True)[:4])
    if not nuevas:
        nuevas = list(Producto.objects.order_by('-creado')[:4])
    portada = next(
        (p for p in destacados if p.imagen),
        destacados[0] if destacados else None,
    )

    context = {
        'categorias': Categoria.objects.all(),
        'destacados': destacados,
        'tendencias_moda': Tendencia.objects.filter(activa=True).order_by('-popularidad')[:6],
        'colecciones': Coleccion.objects.filter(es_recomendada=True)[:4],
        'nuevas': nuevas,
        'producto_portada': portada,
    }
    return render(request, 'core/inicio.html', context)


@administrador_requerido
def informes(request):
    """Estadísticas reales del proyecto a partir de la base de datos."""
    total_productos = Producto.objects.count()
    total_materiales = Material.objects.count()
    total_tendencias = Tendencia.objects.filter(activa=True).count()
    total_propuestas = PropuestaDiseno.objects.count()
    total_colecciones = Coleccion.objects.count()

    materiales = Material.objects.all()
    stock_materiales = materiales.aggregate(total=Sum('cantidad'))['total'] or 0
    agotados = Material.objects.filter(cantidad__lte=0).count()
    stock_bajo = sum(1 for m in materiales if m.stock_bajo and not m.agotado)

    productos_por_categoria = list(
        Categoria.objects.annotate(total=Count('productos')).filter(total__gt=0)
    )
    max_categoria = max((c.total for c in productos_por_categoria), default=1)
    grafico_categorias = [
        {'nombre': c.nombre, 'total': c.total, 'ancho': round(c.total / max_categoria * 100)}
        for c in sorted(productos_por_categoria, key=lambda c: c.total, reverse=True)
    ]
    stock_productos = Producto.objects.aggregate(total=Sum('unidades'))['total'] or 0
    agotados_productos = Producto.objects.filter(unidades=0).count()

    tendencias_top = list(Tendencia.objects.filter(activa=True).order_by('-popularidad')[:6])

    # Distribución de estado del inventario de materiales
    total_materiales_estado = total_materiales or 1
    estados = [
        {'clave': 'disponible', 'etiqueta': 'Disponibles', 'total': sum(1 for m in materiales if m.disponible)},
        {'clave': 'bajo', 'etiqueta': 'Stock bajo', 'total': stock_bajo},
        {'clave': 'agotado', 'etiqueta': 'Agotados', 'total': agotados},
    ]
    for estado in estados:
        estado['ancho'] = round(estado['total'] / total_materiales_estado * 100)

    context = {
        'total_productos': total_productos,
        'total_materiales': total_materiales,
        'stock_materiales': stock_materiales,
        'agotados': agotados,
        'stock_bajo': stock_bajo,
        'total_tendencias': total_tendencias,
        'total_propuestas': total_propuestas,
        'total_colecciones': total_colecciones,
        'grafico_categorias': grafico_categorias,
        'stock_productos': stock_productos,
        'agotados_productos': agotados_productos,
        'tendencias_top': tendencias_top,
        'estados': estados,
        'informes_demo': False,
    }
    return render(request, 'core/informes.html', context)