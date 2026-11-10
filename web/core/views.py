"""Vistas de la aplicación core: inicio y estadísticas del proyecto."""

from collections import Counter
from types import SimpleNamespace

from django.shortcuts import redirect, render
from django.utils.text import slugify

from catalogo import api_cliente
from catalogo.api_cliente import ApiNoDisponible
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

    error_api = None
    try:
        # La API ordena por fecha de creación descendente: lo primero es lo
        # último que se registró.
        prendas = api_cliente.listar_prendas(tamano=12).object_list
    except ApiNoDisponible as exc:
        prendas = []
        error_api = str(exc)

    destacados = prendas[:8]
    nuevas = prendas[:4]
    tipos = sorted({p.tipo for p in prendas if p.tipo})
    categorias = [
        SimpleNamespace(nombre=tipo.capitalize(), slug=slugify(tipo))
        for tipo in tipos
    ]

    context = {
        'categorias': categorias,
        'destacados': destacados,
        'tendencias_moda': Tendencia.objects.filter(activa=True).order_by('-popularidad')[:6],
        'colecciones': Coleccion.objects.filter(es_recomendada=True)[:4],
        'nuevas': nuevas,
        'producto_portada': destacados[0] if destacados else None,
        'error_api': error_api,
    }
    return render(request, 'core/inicio.html', context)


@administrador_requerido
def informes(request):
    """Estadísticas del proyecto: catálogo e inventario desde la API."""
    error_api = None
    try:
        pagina_prendas = api_cliente.listar_prendas(tamano=100)
        prendas = pagina_prendas.object_list
        materiales = api_cliente.listar_materiales()
    except ApiNoDisponible as exc:
        error_api = str(exc)
        pagina_prendas = api_cliente.Pagina.vacia()
        prendas = []
        materiales = []

    total_productos = pagina_prendas.paginator.count
    total_materiales = len(materiales)
    total_tendencias = Tendencia.objects.filter(activa=True).count()
    total_propuestas = PropuestaDiseno.objects.count()
    total_colecciones = Coleccion.objects.count()

    stock_materiales = sum(m.cantidad for m in materiales)
    agotados = sum(1 for m in materiales if m.agotado)
    stock_bajo = sum(1 for m in materiales if m.stock_bajo)

    conteo_tipos = Counter((p.tipo or 'Sin tipo').capitalize() for p in prendas)
    max_categoria = max(conteo_tipos.values(), default=1)
    grafico_categorias = [
        {'nombre': nombre, 'total': total, 'ancho': round(total / max_categoria * 100)}
        for nombre, total in conteo_tipos.most_common()
    ]

    total_tipos = len(conteo_tipos)
    analizadas = sum(1 for p in prendas if (p.estado or '').upper() == 'ANALIZADA')

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
        'total_tipos': total_tipos,
        'analizadas': analizadas,
        'tendencias_top': tendencias_top,
        'estados': estados,
        'error_api': error_api,
        'informes_demo': False,
    }
    return render(request, 'core/informes.html', context)
