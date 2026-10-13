"""Modelos del catálogo de ATELIER: categorías, productos y materiales."""

from decimal import Decimal

from django.core.validators import MinValueValidator
from django.db import models
from django.urls import reverse
from django.utils.text import slugify


class Categoria(models.Model):
    """Agrupación de productos del catálogo (prendas, telas, accesorios)."""

    nombre = models.CharField('nombre', max_length=80, unique=True)
    slug = models.SlugField('slug', max_length=90, unique=True, blank=True)
    descripcion = models.TextField('descripción', blank=True)
    orden = models.PositiveIntegerField('orden', default=0)

    class Meta:
        verbose_name = 'categoría'
        verbose_name_plural = 'categorías'
        ordering = ['orden', 'nombre']

    def __str__(self):
        return self.nombre

    def save(self, *args, **kwargs):
        if not self.slug:
            self.slug = slugify(self.nombre)
        super().save(*args, **kwargs)


class Producto(models.Model):
    """Prenda, tela o accesorio disponible en el catálogo."""

    class Tipo(models.TextChoices):
        PRENDA = 'PRENDA', 'Prenda'
        TELA = 'TELA', 'Tela'
        ACCESORIO = 'ACCESORIO', 'Accesorio'

    class Temporada(models.TextChoices):
        PRIMAVERA_VERANO = 'PV', 'Primavera-Verano'
        OTONO_INVIERNO = 'OI', 'Otoño-Invierno'
        ATEMPORAL = 'AT', 'Atemporal'

    nombre = models.CharField('nombre', max_length=120)
    slug = models.SlugField('slug', max_length=140, unique=True, blank=True)
    descripcion = models.TextField('descripción')
    tipo = models.CharField('tipo', max_length=10, choices=Tipo.choices, default=Tipo.PRENDA)
    categoria = models.ForeignKey(
        Categoria,
        verbose_name='categoría',
        on_delete=models.PROTECT,
        related_name='productos',
    )
    estilo = models.CharField('estilo', max_length=80, blank=True)
    temporada = models.CharField(
        'temporada', max_length=2, choices=Temporada.choices, default=Temporada.ATEMPORAL
    )
    color = models.CharField('color', max_length=60, blank=True)
    composicion = models.CharField('composición', max_length=120, blank=True)
    precio = models.DecimalField(
        'precio', max_digits=10, decimal_places=2, validators=[MinValueValidator(Decimal('0.00'))]
    )
    unidades = models.PositiveIntegerField('unidades en inventario', default=0)
    imagen = models.ImageField('imagen', upload_to='productos/', blank=True)
    es_destacado = models.BooleanField('destacado', default=False)
    es_tendencia = models.BooleanField('tendencia', default=False)
    es_nuevo = models.BooleanField('nuevo', default=False)
    creado = models.DateTimeField('creado', auto_now_add=True)
    actualizado = models.DateTimeField('actualizado', auto_now=True)

    class Meta:
        verbose_name = 'producto'
        verbose_name_plural = 'productos'
        ordering = ['-creado']

    def __str__(self):
        return self.nombre

    def save(self, *args, **kwargs):
        if not self.slug:
            base = slugify(self.nombre)
            self.slug = base
        super().save(*args, **kwargs)

    def get_absolute_url(self):
        return reverse('catalogo:detalle', kwargs={'slug': self.slug})

    @property
    def disponible(self):
        return self.unidades > 0

    @property
    def estado(self):
        """Etiqueta visual del producto: Tendencia, Nuevo o Agotado."""
        if not self.disponible:
            return 'agotado'
        if self.es_tendencia:
            return 'tendencia'
        if self.es_nuevo:
            return 'nuevo'
        return None


class Material(models.Model):
    """Material / tela del inventario de producción."""

    class Unidad(models.TextChoices):
        METRO = 'm', 'Metro'
        ROLLO = 'rollo', 'Rollo'
        KG = 'kg', 'Kilogramo'
        UNIDAD = 'u', 'Unidad'

    categoria = models.CharField('categoría', max_length=80)
    nombre = models.CharField('nombre', max_length=120)
    slug = models.SlugField('slug', max_length=140, unique=True, blank=True)
    color = models.CharField('color', max_length=60, blank=True)
    composicion = models.CharField('composición', max_length=120, blank=True)
    tipo_tela = models.CharField('tipo de tela', max_length=80, blank=True)
    cantidad = models.DecimalField(
        'cantidad disponible', max_digits=10, decimal_places=2,
        validators=[MinValueValidator(Decimal('0.00'))],
    )
    unidad_medida = models.CharField(
        'unidad de medida', max_length=10, choices=Unidad.choices, default=Unidad.METRO
    )
    stock_minimo = models.DecimalField(
        'stock mínimo', max_digits=10, decimal_places=2, default=Decimal('10.00'),
        validators=[MinValueValidator(Decimal('0.00'))],
    )
    precio_unitario = models.DecimalField(
        'precio unitario', max_digits=10, decimal_places=2, default=Decimal('0.00'),
        validators=[MinValueValidator(Decimal('0.00'))],
    )
    ubicacion = models.CharField('ubicación', max_length=80, blank=True)
    actualizado = models.DateTimeField('actualizado', auto_now=True)

    class Meta:
        verbose_name = 'material'
        verbose_name_plural = 'materiales'
        ordering = ['categoria', 'nombre']

    def __str__(self):
        return self.nombre

    def save(self, *args, **kwargs):
        if not self.slug:
            self.slug = slugify(f'{self.nombre}-{self.color}' if self.color else self.nombre)
        super().save(*args, **kwargs)

    @property
    def agotado(self):
        return self.cantidad <= 0

    @property
    def stock_bajo(self):
        return 0 < self.cantidad <= self.stock_minimo

    @property
    def disponible(self):
        return self.cantidad > 0

    @property
    def estado(self):
        """Estado visual del stock: agotado, bajo o disponible."""
        if self.agotado:
            return 'agotado'
        if self.stock_bajo:
            return 'bajo'
        return 'disponible'