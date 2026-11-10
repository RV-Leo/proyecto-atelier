"""Vistas del catálogo de ATELIER.

El catálogo y el inventario se leen de la API del backend (Spring Boot). La web
no consulta PostgreSQL ni guarda prendas o materiales propios: solo mantiene en
su base las tablas internas de Django (usuarios, sesiones, admin).
"""

from django.contrib.auth.decorators import login_required
from django.http import Http404
from django.shortcuts import render

from . import api_cliente
from .api_cliente import ApiNoDisponible

POR_PAGINA = 12


def _entero(valor, defecto=1):
    try:
        return max(int(valor), 1)
    except (TypeError, ValueError):
        return defecto


def lista(request):
    """Catálogo con búsqueda y filtros, servido por la API."""
    filtros = {
        'q': request.GET.get('q', '').strip(),
        'tipo': request.GET.get('tipo', '').strip(),
        'estilo': request.GET.get('estilo', '').strip(),
        'estado': request.GET.get('estado', '').strip(),
    }
    activos = {clave: valor for clave, valor in filtros.items() if valor}

    error_api = None
    tipos, estilos = [], []
    try:
        tipos, estilos = api_cliente.opciones_catalogo()
    except ApiNoDisponible as exc:
        error_api = str(exc)

    try:
        pagina = api_cliente.listar_prendas(
            pagina=_entero(request.GET.get('pagina')),
            tamano=POR_PAGINA,
            **activos,
        )
    except ApiNoDisponible as exc:
        error_api = error_api or str(exc)
        pagina = api_cliente.Pagina.vacia()

    context = {
        'pagina': pagina,
        'tipos': tipos,
        'estilos': estilos,
        'filtros': filtros,
        'error_api': error_api,
        'resumen': _resumen_paginacion(pagina),
    }
    return render(request, 'catalogo/lista.html', context)


def detalle(request, slug):
    """Detalle de una prenda y prendas del mismo tipo."""
    try:
        producto = api_cliente.obtener_por_slug(slug)
    except ApiNoDisponible as exc:
        return render(
            request,
            'catalogo/detalle.html',
            {'producto': None, 'relacionados': [], 'error_api': str(exc)},
            status=503,
        )

    if producto is None:
        raise Http404('No existe la prenda %s' % slug)

    relacionados = []
    try:
        pagina = api_cliente.listar_prendas(
            pagina=1, tamano=4, tipo=producto.tipo or None
        )
        relacionados = [p for p in pagina.object_list if p.slug != producto.slug][:4]
    except ApiNoDisponible:
        pass

    context = {
        'producto': producto,
        'relacionados': relacionados,
        # Los favoritos siguen apuntando a la tabla local de productos, que ya
        # no se usa para el catálogo. Se ocultan mientras la API no exponga un
        # identificador estable para guardarlos.
        'favoritos_disponibles': False,
        'error_api': None,
    }
    return render(request, 'catalogo/detalle.html', context)


@login_required
def inventario(request):
    """Inventario de materiales y telas, servido por la API."""
    q = request.GET.get('q', '').strip()
    estado = request.GET.get('estado', '').strip()

    error_api = None
    todos, materiales = [], []
    try:
        todos = api_cliente.listar_materiales()
        if q or estado:
            materiales = api_cliente.listar_materiales(q=q or None, estado=estado or None)
        else:
            materiales = todos
    except ApiNoDisponible as exc:
        error_api = str(exc)

    totales = {
        'disponible': sum(1 for m in todos if m.disponible),
        'bajo': sum(1 for m in todos if m.stock_bajo),
        'agotado': sum(1 for m in todos if m.agotado),
    }
    categorias = sorted({m.categoria for m in todos if m.categoria})

    context = {
        'materiales': materiales,
        'categorias': categorias,
        'estado_activo': estado,
        'q': q,
        'totales': totales,
        'total': len(materiales),
        'error_api': error_api,
    }
    return render(request, 'catalogo/inventario.html', context)


def _resumen_paginacion(pagina):
    if not pagina.has_other_pages():
        return None
    tamano = pagina.tamano or POR_PAGINA
    inicio = (pagina.number - 1) * tamano + 1
    fin = min(pagina.number * tamano, pagina.paginator.count)
    return {
        'inicio': inicio,
        'fin': fin,
        'total': pagina.paginator.count,
    }
