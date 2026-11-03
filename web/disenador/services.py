"""Casos de uso del asistente de diseño."""

from .models import Mensaje
from .repositories import DisenadorRepository
from .servicios import obtener_proveedor


class DisenoService:
    """Orquesta generación, persistencia y consulta de propuestas."""

    def __init__(self, repository=None, proveedor=None):
        self.repository = repository or DisenadorRepository()
        self.proveedor = proveedor or obtener_proveedor

    def estado_asistente(self, usuario, conversacion_id=None, propuestas_demo=None):
        conversacion = None
        propuestas = []
        if conversacion_id and usuario.is_authenticated:
            conversacion = self.repository.conversacion_usuario(usuario, conversacion_id)
            propuestas = self.repository.propuestas_conversacion(conversacion)
        elif propuestas_demo:
            propuestas = propuestas_demo
        if usuario.is_authenticated and not conversacion:
            conversacion = self.repository.ultima_conversacion(usuario)
        return conversacion, propuestas

    def generar(self, usuario, resumen, opciones):
        proveedor = self.proveedor() if callable(self.proveedor) else self.proveedor
        resultado = proveedor.generar(resumen, opciones)
        propuestas = resultado['propuestas']
        conversacion = None
        if usuario.is_authenticated:
            titulo = resumen[:60] + ('…' if len(resumen) > 60 else '')
            conversacion = self.repository.crear_conversacion(usuario, titulo)
            self.repository.crear_mensaje(conversacion, Mensaje.Rol.USUARIO, resumen)
            lista = '\n'.join(
                f'{indice + 1}. {propuesta["titulo"]}: {propuesta["resumen"]}'
                for indice, propuesta in enumerate(propuestas)
            )
            self.repository.crear_mensaje(
                conversacion, Mensaje.Rol.ASISTENTE, f'{resultado["intro"]}\n{lista}'
            )
            for propuesta in propuestas:
                self.repository.crear_propuesta(
                    usuario=usuario,
                    conversacion=conversacion,
                    prompt=resumen,
                    tipo_prenda=opciones['tipo_prenda'],
                    estilo=opciones['estilo'],
                    temporada=opciones['temporada'],
                    calidad=opciones['calidad'],
                    paleta=opciones['paleta'],
                    resultado_resumen=propuesta['resumen'],
                    imagen=propuesta['imagen'],
                    es_favorita=False,
                    es_demo=True,
                )
        else:
            for propuesta in propuestas:
                propuesta['resultado_resumen'] = propuesta.get('resumen', '')
        return {'resultado': resultado, 'conversacion': conversacion, 'propuestas': propuestas}

    def alternar_favorita(self, usuario, pk):
        propuesta = self.repository.propuesta_usuario(usuario, pk)
        nueva_estado = not propuesta.es_favorita
        self.repository.actualizar_estado_favorita(propuesta, nueva_estado)
        return nueva_estado

    def historial(self, usuario, filtro):
        propuestas = self.repository.propuestas_usuario(usuario)
        if filtro == 'favoritas':
            propuestas = propuestas.filter(es_favorita=True)
        return {'propuestas': propuestas, 'filtro': filtro, 'total': propuestas.count()}
