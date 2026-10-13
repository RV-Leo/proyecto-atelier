"""Modelos del asistente de diseño con IA de ATELIER."""

from django.conf import settings
from django.db import models


class Conversacion(models.Model):
    """Conversación de un usuario con el asistente de diseño."""

    usuario = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        verbose_name='usuario',
        on_delete=models.CASCADE,
        related_name='conversaciones',
    )
    titulo = models.CharField('título', max_length=160, blank=True)
    creada = models.DateTimeField('creada', auto_now_add=True)
    actualizada = models.DateTimeField('actualizada', auto_now=True)

    class Meta:
        verbose_name = 'conversación'
        verbose_name_plural = 'conversaciones'
        ordering = ['-actualizada']

    def __str__(self):
        return self.titulo or f'Conversación de {self.usuario}'


class Mensaje(models.Model):
    """Turno de diálogo dentro de una conversación."""

    class Rol(models.TextChoices):
        USUARIO = 'usuario', 'Usuario'
        ASISTENTE = 'asistente', 'Asistente'

    conversacion = models.ForeignKey(
        Conversacion,
        on_delete=models.CASCADE,
        related_name='mensajes',
    )
    rol = models.CharField('rol', max_length=10, choices=Rol.choices)
    texto = models.TextField('texto')
    creada = models.DateTimeField('creada', auto_now_add=True)

    class Meta:
        verbose_name = 'mensaje'
        verbose_name_plural = 'mensajes'
        ordering = ['creada']

    def __str__(self):
        return f'{self.rol}: {self.texto[:50]}'


class PropuestaDiseno(models.Model):
    """Propuesta de diseño generada por el asistente y guardada por el usuario."""

    usuario = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        verbose_name='usuario',
        on_delete=models.CASCADE,
        related_name='propuestas',
    )
    conversacion = models.ForeignKey(
        Conversacion,
        verbose_name='conversación',
        on_delete=models.SET_NULL,
        null=True,
        blank=True,
        related_name='propuestas',
    )
    prompt = models.TextField('descripción del diseño')
    tipo_prenda = models.CharField('tipo de prenda', max_length=80, blank=True)
    estilo = models.CharField('estilo', max_length=80, blank=True)
    temporada = models.CharField('temporada', max_length=60, blank=True)
    calidad = models.CharField('calidad', max_length=40, blank=True)
    paleta = models.CharField('paleta de colores', max_length=160, blank=True)
    resultado_resumen = models.TextField('resumen del resultado', blank=True)
    imagen = models.ImageField('imagen', upload_to='propuestas/', blank=True)
    es_favorita = models.BooleanField('favorita', default=False)
    es_demo = models.BooleanField('simulada (demo)', default=True)
    coleccion = models.ForeignKey(
        'cuentas.Coleccion',
        verbose_name='colección',
        on_delete=models.SET_NULL,
        null=True,
        blank=True,
        related_name='disenos',
    )
    creada = models.DateTimeField('creada', auto_now_add=True)

    class Meta:
        verbose_name = 'propuesta de diseño'
        verbose_name_plural = 'propuestas de diseño'
        ordering = ['-creada']

    def __str__(self):
        return f'{self.tipo_prenda or self.prompt[:40]} de {self.usuario}'