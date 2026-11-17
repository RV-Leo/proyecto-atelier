"""Consultas de lectura para portada, informes y panel administrativo."""

from django.contrib.auth import get_user_model
from django.db.models import Count, Sum

from catalogo.models import Categoria, Material, Producto
from cuentas.models import Coleccion
from disenador.models import PropuestaDiseno
from tendencias.models import Tendencia


class CoreRepository:
    """Agrupa consultas de lectura que combinan varios módulos del dominio."""

    def datos_portada(self):
        destacados = list(Producto.objects.filter(es_destacado=True)[:8])
        nuevas = list(Producto.objects.filter(es_nuevo=True)[:4])
        if not nuevas:
            nuevas = list(Producto.objects.order_by('-creado')[:4])
        portada = next((p for p in destacados if p.imagen), destacados[0] if destacados else None)
        return {
            'categorias': Categoria.objects.all(),
            'destacados': destacados,
            'tendencias_moda': Tendencia.objects.filter(activa=True).order_by('-popularidad')[:6],
            'colecciones': Coleccion.objects.filter(es_recomendada=True)[:4],
            'nuevas': nuevas,
            'producto_portada': portada,
        }

    def datos_informes(self):
        materiales = list(Material.objects.all())
        categorias = list(Categoria.objects.annotate(total=Count('productos')).filter(total__gt=0))
        return {
            'total_productos': Producto.objects.count(),
            'total_materiales': len(materiales),
            'total_tendencias': Tendencia.objects.filter(activa=True).count(),
            'total_propuestas': PropuestaDiseno.objects.count(),
            'total_colecciones': Coleccion.objects.count(),
            'materiales': materiales,
            'stock_materiales': Material.objects.aggregate(total=Sum('cantidad'))['total'] or 0,
            'agotados': Material.objects.filter(cantidad__lte=0).count(),
            'categorias': categorias,
            'stock_productos': Producto.objects.aggregate(total=Sum('unidades'))['total'] or 0,
            'agotados_productos': Producto.objects.filter(unidades=0).count(),
            'tendencias_top': list(
                Tendencia.objects.filter(activa=True).order_by('-popularidad')[:6]
            ),
        }

    def datos_panel(self):
        usuario = get_user_model()
        materiales = list(Material.objects.all())
        total_usuarios = usuario.objects.count()
        total_administradores = (
            usuario.objects.filter(groups__name='Administradores').distinct().count()
            + usuario.objects.filter(is_superuser=True).count()
        )
        return {
            'materiales': materiales,
            'total_usuarios': total_usuarios,
            'total_administradores': total_administradores,
            'productos': Producto.objects.count(),
            'categorias_total': Categoria.objects.count(),
            'tendencias_activas': Tendencia.objects.filter(activa=True).count(),
            'propuestas_total': PropuestaDiseno.objects.count(),
            'colecciones_total': Coleccion.objects.count(),
            'categorias': list(
                Categoria.objects.annotate(total=Count('productos')).order_by('-total', 'nombre')
            ),
            'productos_recientes': Producto.objects.select_related('categoria').order_by('-creado')[:6],
            'tendencias': Tendencia.objects.order_by('-popularidad')[:6],
            'propuestas_recientes': PropuestaDiseno.objects.select_related('usuario').order_by('-creada')[:6],
            'colecciones_recientes': Coleccion.objects.select_related('usuario').order_by('-creada')[:6],
            'usuarios_recientes': usuario.objects.order_by('-date_joined')[:6],
            'stock_materiales': Material.objects.aggregate(total=Sum('cantidad'))['total'] or 0,
        }

    def usuarios(self):
        return get_user_model().objects.prefetch_related('groups').order_by('-date_joined')
