"""Acceso a datos de conversaciones y propuestas de diseño."""

from django.shortcuts import get_object_or_404
from django.utils import timezone

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
        mensaje = Mensaje.objects.create(conversacion=conversacion, rol=rol, texto=texto)
        conversacion.actualizada = timezone.now()
        conversacion.save(update_fields=['actualizada'])
        return mensaje

    def mensaje_usuario(self, usuario, pk):
        """Obtiene un mensaje editable que pertenezca al usuario autenticado."""
        return get_object_or_404(
            Mensaje,
            pk=pk,
            rol=Mensaje.Rol.USUARIO,
            conversacion__usuario=usuario,
        )

    def mensajes_recientes(self, conversacion, limite=19):
        mensajes = list(conversacion.mensajes.order_by('-creada')[:limite])
        return list(reversed(mensajes))

    def crear_propuesta(self, **datos):
        return PropuestaDiseno.objects.create(**datos)

    def propuesta_usuario(self, usuario, pk):
        return get_object_or_404(PropuestaDiseno, pk=pk, usuario=usuario)

    def actualizar_estado_favorita(self, propuesta, valor):
        propuesta.es_favorita = valor
        propuesta.save(update_fields=['es_favorita'])

    def propuestas_usuario(self, usuario):
        return usuario.propuestas.all()
