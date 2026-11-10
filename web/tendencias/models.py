"""Modelos de tendencias textiles de ATELIER."""

from django.db import models
from django.utils.text import slugify


class Tendencia(models.Model):
    """Tendencia de moda: color, tela, estilo, prenda o temporada."""

    class Tipo(models.TextChoices):
        COLOR = 'COLOR', 'Color'
        TELA = 'TELA', 'Tela'
        ESTILO = 'ESTILO', 'Estilo'
        PRENDA = 'PRENDA', 'Prenda'
        TEMPORADA = 'TEMPORADA', 'Temporada'

    nombre = models.CharField('nombre', max_length=100)
    slug = models.SlugField('slug', max_length=120, unique=True, blank=True)
    descripcion = models.TextField('descripción', blank=True)
    tipo = models.CharField('tipo', max_length=10, choices=Tipo.choices, default=Tipo.ESTILO)
    temporada = models.CharField('temporada', max_length=60, blank=True)
    color = models.CharField('color dominante (hex)', max_length=20, blank=True)
    popularidad = models.PositiveIntegerField('popularidad', default=0)
    imagen = models.ImageField('imagen', upload_to='tendencias/', blank=True)
    activa = models.BooleanField('activa', default=True)
    orden = models.PositiveIntegerField('orden', default=0)

    class Meta:
        verbose_name = 'tendencia'
        verbose_name_plural = 'tendencias'
        ordering = ['orden', '-popularidad']

    def __str__(self):
        return self.nombre

    def save(self, *args, **kwargs):
        if not self.slug:
            self.slug = slugify(self.nombre)
        super().save(*args, **kwargs)