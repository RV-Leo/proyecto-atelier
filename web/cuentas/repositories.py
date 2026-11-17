"""Consultas y persistencia de cuentas, perfiles y colecciones."""

from django.shortcuts import get_object_or_404

from catalogo.models import Producto
from disenador.models import PropuestaDiseno
from tendencias.models import Tendencia

from .models import Coleccion, Favorito, Perfil


class CuentasRepository:
    """Encapsula el acceso ORM usado por los casos de uso de cuentas."""

    def perfil(self, usuario):
        return Perfil.objects.filter(usuario=usuario).first()

    def crear_o_obtener_perfil(self, usuario):
        return Perfil.objects.get_or_create(usuario=usuario)

    def colecciones_usuario(self, usuario):
        return Coleccion.objects.filter(usuario=usuario).order_by('-creada')

    def propuestas_usuario(self, usuario):
        return PropuestaDiseno.objects.filter(usuario=usuario)

    def propuestas_recientes(self, usuario, limite=4):
        return self.propuestas_usuario(usuario)[:limite]

    def favoritas_usuario(self, usuario):
        return Favorito.objects.filter(usuario=usuario)

    def favoritos_con_producto(self, usuario, limite=None):
        favoritos = self.favoritas_usuario(usuario).select_related('producto')
        return favoritos[:limite] if limite else favoritos

    def propuestas_favoritas(self, usuario, limite=None):
        propuestas = self.propuestas_usuario(usuario).filter(es_favorita=True)
        return propuestas[:limite] if limite else propuestas

    def productos_recomendados(self, limite=4):
        productos = list(Producto.objects.filter(es_destacado=True)[:limite])
        return productos or list(Producto.objects.order_by('-creado')[:limite])

    def tendencias_activas(self, limite=6):
        return Tendencia.objects.filter(activa=True).order_by('-popularidad')[:limite]

    def favorito(self, usuario, producto):
        return Favorito.objects.get_or_create(usuario=usuario, producto=producto)

    def eliminar_favorito(self, favorito):
        favorito.delete()

    def coleccion_por_slug(self, slug):
        return get_object_or_404(
            Coleccion.objects.prefetch_related('productos'), slug=slug
        )

    def coleccion_propia(self, usuario, slug):
        return get_object_or_404(Coleccion, slug=slug, usuario=usuario)

    def producto_por_id(self, producto_id):
        return get_object_or_404(Producto, pk=producto_id)

    def productos(self):
        return Producto.objects.all()

    def colecciones_perfil(self, usuario):
        return usuario.colecciones.all()

    def guardar_coleccion(self, coleccion):
        coleccion.save()
        return coleccion

    def agregar_producto_a_coleccion(self, coleccion, producto):
        coleccion.productos.add(producto)

    def quitar_producto_de_coleccion(self, coleccion, producto):
        coleccion.productos.remove(producto)

    def eliminar_coleccion(self, coleccion):
        coleccion.delete()
