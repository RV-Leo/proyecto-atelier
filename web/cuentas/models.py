"""Modelos de cuentas, perfil, colecciones y favoritos."""

from django.conf import settings
from django.contrib.auth.models import User
from django.db import models
from django.db.models.signals import post_save
from django.dispatch import receiver
from django.utils.text import slugify


class Perfil(models.Model):
    """Información complementaria del usuario."""

    usuario = models.OneToOneField(
        settings.AUTH_USER_MODEL,
        on_delete=models.CASCADE,
        related_name='perfil',
    )
    nombre_artistico = models.CharField('nombre artístico', max_length=80, blank=True)
    bio = models.TextField('bio', max_length=400, blank=True)
    ciudad = models.CharField('ciudad', max_length=80, blank=True)
    preferencias = models.TextField(
        'preferencias de diseño', max_length=600, blank=True,
        help_text='Estilos, colores y telas que prefieres en tus propuestas.',
    )
    avatar = models.ImageField('avatar', upload_to='perfiles/', blank=True)
    acepta_comunicaciones = models.BooleanField('quiere recibir novedades', default=True)

    class Meta:
        verbose_name = 'perfil'
        verbose_name_plural = 'perfiles'

    def __str__(self):
        return self.nombre_artistico or self.usuario.get_full_name() or self.usuario.username


class Coleccion(models.Model):
    """Colección de productos. Puede ser del usuario o curada por ATELIER."""

    usuario = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        verbose_name='usuario',
        on_delete=models.CASCADE,
        null=True,
        blank=True,
        related_name='colecciones',
    )
    nombre = models.CharField('nombre', max_length=100)
    slug = models.SlugField('slug', max_length=120, blank=True)
    descripcion = models.TextField('descripción', blank=True)
    imagen = models.ImageField('imagen', upload_to='colecciones/', blank=True)
    productos = models.ManyToManyField('catalogo.Producto', blank=True, related_name='colecciones')
    es_recomendada = models.BooleanField('colección recomendada', default=False)
    creada = models.DateTimeField('creada', auto_now_add=True)

    class Meta:
        verbose_name = 'colección'
        verbose_name_plural = 'colecciones'
        ordering = ['-creada']

    def __str__(self):
        return self.nombre

    def save(self, *args, **kwargs):
        if not self.slug:
            base = slugify(self.nombre)
            slug = base
            n = 1
            while Coleccion.objects.filter(slug=slug).exists():
                slug = f'{base}-{n}'
                n += 1
            self.slug = slug
        super().save(*args, **kwargs)


class Favorito(models.Model):
    """Producto marcado como favorito por un usuario."""

    usuario = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.CASCADE,
        related_name='favoritos',
    )
    producto = models.ForeignKey(
        'catalogo.Producto',
        on_delete=models.CASCADE,
        related_name='favoritos',
    )
    creada = models.DateTimeField('creada', auto_now_add=True)

    class Meta:
        verbose_name = 'favorito'
        verbose_name_plural = 'favoritos'
        ordering = ['-creada']
        constraints = [
            models.UniqueConstraint(fields=['usuario', 'producto'], name='uniq_favorito_usuario_producto'),
        ]

    def __str__(self):
        return f'{self.usuario} → {self.producto}'


@receiver(post_save, sender=User)
def crear_perfil_usuario(sender, instance, created, **kwargs):
    """Crea el perfil automáticamente al registrar un usuario."""
    if created:
        Perfil.objects.get_or_create(usuario=instance)