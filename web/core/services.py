"""Casos de uso que preparan datos para las pantallas generales de ATELIER."""

from .permisos import GRUPO_ADMINISTRADORES
from .repositories import CoreRepository


class CoreService:
    """Construye modelos de lectura para portada, informes y administración."""

    def __init__(self, repository=None):
        self.repository = repository or CoreRepository()

    def portada(self):
        return self.repository.datos_portada()

    def informes(self):
        datos = self.repository.datos_informes()
        materiales = datos.pop('materiales')
        agotados = datos['agotados']
        stock_bajo = sum(1 for material in materiales if material.stock_bajo and not material.agotado)
        categorias = datos.pop('categorias')
        max_categoria = max((categoria.total for categoria in categorias), default=1)
        grafico_categorias = [
            {'nombre': categoria.nombre, 'total': categoria.total,
             'ancho': round(categoria.total / max_categoria * 100)}
            for categoria in sorted(categorias, key=lambda categoria: categoria.total, reverse=True)
        ]
        total_estado = datos['total_materiales'] or 1
        estados = [
            {'clave': 'disponible', 'etiqueta': 'Disponibles',
             'total': sum(1 for material in materiales if material.disponible)},
            {'clave': 'bajo', 'etiqueta': 'Stock bajo', 'total': stock_bajo},
            {'clave': 'agotado', 'etiqueta': 'Agotados', 'total': agotados},
        ]
        for estado in estados:
            estado['ancho'] = round(estado['total'] / total_estado * 100)
        return {
            **datos,
            'stock_bajo': stock_bajo,
            'grafico_categorias': grafico_categorias,
            'estados': estados,
            'informes_demo': False,
        }

    def panel_administrativo(self):
        datos = self.repository.datos_panel()
        materiales = datos.pop('materiales')
        agotados = [material for material in materiales if material.agotado]
        stock_bajo = [material for material in materiales if material.stock_bajo and not material.agotado]
        total_administradores = datos['total_administradores']
        total_usuarios = datos['total_usuarios']
        categorias = datos.pop('categorias')
        max_categoria = max((categoria.total for categoria in categorias), default=1)
        indicadores = [
            {'etiqueta': 'Productos en catálogo', 'valor': datos['productos'], 'icono': 'prenda'},
            {'etiqueta': 'Categorías', 'valor': datos['categorias_total'], 'icono': 'categoria'},
            {'etiqueta': 'Materiales', 'valor': len(materiales), 'icono': 'inventario'},
            {'etiqueta': 'Stock bajo', 'valor': len(stock_bajo), 'icono': 'alerta'},
            {'etiqueta': 'Materiales agotados', 'valor': len(agotados), 'icono': 'alerta'},
            {'etiqueta': 'Tendencias activas', 'valor': datos['tendencias_activas'], 'icono': 'tendencia'},
            {'etiqueta': 'Propuestas de diseño', 'valor': datos['propuestas_total'], 'icono': 'diseno'},
            {'etiqueta': 'Colecciones', 'valor': datos['colecciones_total'], 'icono': 'coleccion'},
            {'etiqueta': 'Usuarios registrados', 'valor': total_usuarios, 'icono': 'usuario'},
            {'etiqueta': 'Clientes', 'valor': max(total_usuarios - total_administradores, 0), 'icono': 'usuario'},
        ]
        grafico = [
            {'nombre': categoria.nombre, 'total': categoria.total,
             'ancho': round(categoria.total / max_categoria * 100)}
            for categoria in categorias
        ]
        return {
            **datos,
            'indicadores': indicadores,
            'grafico_categorias': grafico,
            'categorias': categorias,
            'materiales_bajo': (stock_bajo + agotados)[:8],
            'total_stock_bajo': len(stock_bajo),
            'total_agotados': len(agotados),
        }

    def usuarios_administrativos(self):
        lista = self.repository.usuarios()
        filas = []
        for usuario in lista:
            if usuario.is_superuser:
                rol, clase = 'Superusuario', 'rol-administrador'
            elif usuario.is_staff or usuario.groups.filter(name=GRUPO_ADMINISTRADORES).exists():
                rol, clase = 'Administrador', 'rol-administrador'
            else:
                rol, clase = 'Cliente', 'rol-cliente'
            filas.append({'usuario': usuario, 'rol': rol, 'clase': clase})
        return {'filas': filas, 'total': lista.count()}
