"""Vistas de la aplicación core: inicio y estadísticas del proyecto."""

from django.contrib import messages
from django.db.models import Count, Sum
from django.shortcuts import render

from catalogo.models import Categoria, Material, Producto
from cuentas.models import Coleccion
from disenador.models import PropuestaDiseno
from tendencias.models import Tendencia


def inicio(request):
    """Página principal de ATELIER."""
    categorias = Categoria.objects.all()
    destacados = Producto.objects.filter(es_destacado=True)[:8]
    tendencias_moda = Tendencia.objects.filter(activa=True)[:6]
    tendencias_catalogo = Producto.objects.filter(es_tendencia=True, es_destacado=True)[:4]
    nuevas = Producto.objects.filter(es_nuevo=True)[:4]
    colecciones = Coleccion.objects.filter(es_recomendada=True)[:4]
    acceso_destacado = tendencias_catalogo.first()

    context = {
        'categorias': categorias,
        'destacados': destacados,
        'tendencias_moda': tendencias_moda,
        'tendencias_catalogo': tendencias_catalogo,
        'nuevas': nuevas,
        'colecciones': colecciones,
        'acceso_destacado': acceso_destacado,
    }
    return render(request, 'core/inicio.html', context)


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