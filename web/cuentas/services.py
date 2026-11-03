"""Casos de uso de cuentas y experiencia de cliente."""

from .repositories import CuentasRepository


class CuentasService:
    """Coordina consultas y cambios de cuentas sin depender de HTTP."""

    def __init__(self, repository=None):
        self.repository = repository or CuentasRepository()

    def panel_cliente(self, usuario):
        perfil = self.repository.perfil(usuario)
        colecciones = self.repository.colecciones_usuario(usuario)
        propuestas = self.repository.propuestas_usuario(usuario)
        favoritas = self.repository.propuestas_favoritas(usuario, limite=6)
        favoritos = self.repository.favoritos_con_producto(usuario, limite=6)
        tendencias = self.repository.tendencias_activas()
        nombre = (
            perfil.nombre_artistico if perfil and perfil.nombre_artistico
            else usuario.get_short_name() or usuario.get_username()
        )
        return {
            'perfil': perfil,
            'nombre': nombre,
            'colecciones': colecciones,
            'total_colecciones': colecciones.count(),
            'favoritas': favoritas,
            'total_favoritas': self.repository.propuestas_favoritas(usuario).count(),
            'propuestas': propuestas[:6],
            'total_propuestas': propuestas.count(),
            'favoritos': favoritos,
            'total_favoritos': self.repository.favoritas_usuario(usuario).count(),
            'recomendados': self.repository.productos_recomendados(),
            'tendencias': tendencias,
        }

    def datos_perfil(self, usuario):
        perfil, _ = self.repository.crear_o_obtener_perfil(usuario)
        return {
            'perfil': perfil,
            'colecciones': self.repository.colecciones_perfil(usuario),
            'favoritos': self.repository.favoritos_con_producto(usuario),
            'propuestas': self.repository.propuestas_recientes(usuario),
            'total_propuestas': self.repository.propuestas_usuario(usuario).count(),
        }

    def alternar_favorito(self, usuario, producto_id):
        producto = self.repository.producto_por_id(producto_id)
        favorito, creado = self.repository.favorito(usuario, producto)
        if not creado:
            self.repository.eliminar_favorito(favorito)
        return producto, creado

    def guardar_coleccion(self, usuario, formulario):
        coleccion = formulario.save(commit=False)
        coleccion.usuario = usuario
        return self.repository.guardar_coleccion(coleccion)

    def detalle_coleccion(self, usuario, slug):
        coleccion = self.repository.coleccion_por_slug(slug)
        propia = coleccion.usuario == usuario
        return {
            'coleccion': coleccion,
            'es_propia': propia,
            'productos_todos': (
                self.repository.productos() if propia
                else self.repository.productos().none()
            ),
        }

    def agregar_producto(self, usuario, slug, producto_id):
        coleccion = self.repository.coleccion_propia(usuario, slug)
        producto = self.repository.producto_por_id(producto_id)
        self.repository.agregar_producto_a_coleccion(coleccion, producto)
        return coleccion, producto

    def quitar_producto(self, usuario, slug, producto_id):
        coleccion = self.repository.coleccion_propia(usuario, slug)
        producto = self.repository.producto_por_id(producto_id)
        self.repository.quitar_producto_de_coleccion(coleccion, producto)
        return coleccion, producto

    def eliminar_coleccion(self, usuario, slug):
        coleccion = self.repository.coleccion_propia(usuario, slug)
        nombre = coleccion.nombre
        self.repository.eliminar_coleccion(coleccion)
        return nombre
