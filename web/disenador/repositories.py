"""Acceso a datos de conversaciones y propuestas de diseño."""

from django.shortcuts import get_object_or_404

from .models import Conversacion, Mensaje, PropuestaDiseno


class DisenadorRepository:
    def conversacion_usuario(self, usuario, pk):
        return get_object_or_404(Conversacion, pk=pk, usuario=usuario)

    def ultima_conversacion(self, usuario):
        return usuario.conversaciones.first()

    def propuestas_conversacion(self, conversacion):
        return list(conversacion.propuestas.all())

    def crear_conversacion(self, usuario, titulo):
        return Conversacion.objects.create(usuario=usuario, titulo=titulo)

    def crear_mensaje(self, conversacion, rol, texto):
        return Mensaje.objects.create(conversacion=conversacion, rol=rol, texto=texto)

    def crear_propuesta(self, **datos):
        return PropuestaDiseno.objects.create(**datos)

    def propuesta_usuario(self, usuario, pk):
        return get_object_or_404(PropuestaDiseno, pk=pk, usuario=usuario)

    def actualizar_estado_favorita(self, propuesta, valor):
        propuesta.es_favorita = valor
        propuesta.save(update_fields=['es_favorita'])

    def propuestas_usuario(self, usuario):
        return usuario.propuestas.all()
