"""Vistas del catálogo de ATELIER: listado, búsqueda, filtros y detalle."""

from django.core.paginator import Paginator
from django.db.models import Q
from django.shortcuts import get_object_or_404, render

from .models import Categoria, Material, Producto

POR_PAGINA = 12


def lista(request):
    """Catálogo con búsqueda por nombre/descripción y filtros."""
    productos = Producto.objects.all()
    buscador = request.GET
    q = buscador.get('q', '').strip()
    categoria_slug = buscador.get('categoria', '').strip()
    tipo = buscador.get('tipo', '').strip()
    temporada = buscador.get('temporada', '').strip()
    estilo = buscador.get('estilo', '').strip()

    if q:
        productos = productos.filter(
            Q(nombre__icontains=q) | Q(descripcion__icontains=q)
        )
    if categoria_slug:
        productos = productos.filter(categoria__slug=categoria_slug)
    if tipo:
        productos = productos.filter(tipo=tipo)
    if temporada:
        productos = productos.filter(temporada=temporada)
    if estilo:
        productos = productos.filter(estilo__iexact=estilo)

    categoria_activa = None
    if categoria_slug:
        categoria_activa = get_object_or_404(Categoria, slug=categoria_slug)

    paginator = Paginator(productos, POR_PAGINA)
    pagina = paginator.get_page(buscador.get('pagina'))

    estilos = list(
        Producto.objects.exclude(estilo='').values_list('estilo', flat=True)
        .distinct().order_by('estilo')
    )

    context = {
        'pagina': pagina,
        'categorias': Categoria.objects.all(),
        'categoria_activa': categoria_activa,
        'estilos': estilos,
        'filtros': {
            'q': q,
            'tipo': tipo,
            'temporada': temporada,
            'estilo': estilo,
            'categoria': categoria_slug,
        },
        'resumen': _resumen_paginacion(pagina),
    }
    return render(request, 'catalogo/lista.html', context)


def detalle(request, slug):
    """Detalle de un producto con relacionados y estado de favorito."""
    producto = get_object_or_404(Producto, slug=slug)
    relacionados = (
        Producto.objects.filter(categoria=producto.categoria)
        .exclude(pk=producto.pk)[:4]
    )
    es_favorito = False
    if request.user.is_authenticated:
        es_favorito = request.user.favoritos.filter(producto=producto).exists()

    context = {
        'producto': producto,
        'relacionados': relacionados,
        'es_favorito': es_favorito,
    }
    return render(request, 'catalogo/detalle.html', context)


def inventario(request):
    """Inventario de materiales y telas con estado del stock."""
    materiales = Material.objects.all()
    estado = request.GET.get('estado', '').strip()
    q = request.GET.get('q', '').strip()

    if q:
        materiales = materiales.filter(
            Q(nombre__icontains=q)
            | Q(categoria__icontains=q)
            | Q(color__icontains=q)
            | Q(tipo_tela__icontains=q)
        )

    materiales = list(materiales)
    if estado == 'agotado':
        materiales = [m for m in materiales if m.agotado]
    elif estado == 'bajo':
        materiales = [m for m in materiales if m.stock_bajo]
    elif estado == 'disponible':
        materiales = [m for m in materiales if m.disponible]

    categorias = list(
        Material.objects.values_list('categoria', flat=True).distinct().order_by('categoria')
    )
    disponible = sum(1 for m in Material.objects.all() if m.disponible)
    bajo = sum(1 for m in Material.objects.all() if m.stock_bajo)
    agotado = Material.objects.filter(cantidad__lte=0).count()

    context = {
        'materiales': materiales,
        'categorias': categorias,
        'estado_activo': estado,
        'q': q,
        'totales': {'disponible': disponible, 'bajo': bajo, 'agotado': agotado},
        'total': len(materiales),
    }
    return render(request, 'catalogo/inventario.html', context)


def _resumen_paginacion(pagina):
    if not pagina.has_other_pages():
        return None
    inicio = (pagina.number - 1) * POR_PAGINA + 1
    fin = min(pagina.number * POR_PAGINA, pagina.paginator.count)
    return {
        'inicio': inicio,
        'fin': fin,
        'total': pagina.paginator.count,
    }