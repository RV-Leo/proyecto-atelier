"""Utilidades para generar imágenes de demostración con Pillow.

Sirven para poblar el catálogo y las propuestas con imágenes elegantes
(gradientes y texturas simples) sin depender de Internet. En producción se
reemplazan por fotografías reales subidas desde Django Admin.
"""

import os
import random

from django.conf import settings
from PIL import Image, ImageDraw, ImageFilter

PALETA_BASE = [
    ('#102238', '#2E4A66'),   # azul marino
    ('#8AA1B4', '#DCE6EB'),   # azul grisáceo
    ('#6D4C41', '#A1887F'),   # marrón
    ('#455A64', '#90A4AE'),   # azul pizarra
    ('#4A4A4A', '#8D8D8D'),   # gris
    ('#5D4037', '#8D6E63'),   # café
    ('#37474F', '#607D8B'),   # azul petróleo
    ('#BDBDBD', '#ECEFF1'),   # blanco grisáceo
]


def _guardar(imagen, ruta_relativa):
    directorio = os.path.join(settings.MEDIA_ROOT, os.path.dirname(ruta_relativa))
    os.makedirs(directorio, exist_ok=True)
    ruta = os.path.join(settings.MEDIA_ROOT, ruta_relativa)
    imagen.save(ruta, 'JPEG', quality=88)
    return ruta_relativa


def generar_imagen(nombre, semilla, color_inicio=None, color_fin=None,
                   textura=False, ancho=900, alto=1200):
    """Genera una imagen con degradado suave y textura opcional de tela."""
    rng = random.Random(semilla)
    base = rng.choice(PALETA_BASE)
    c1 = color_inicio or base[0]
    c2 = color_fin or base[1]

    def _rgb(hex_color):
        hex_color = hex_color.lstrip('#')
        return tuple(int(hex_color[i:i + 2], 16) for i in (0, 2, 4))

    rgb1, rgb2 = _rgb(c1), _rgb(c2)
    imagen = Image.new('RGB', (ancho, alto), rgb1)
    draw = ImageDraw.Draw(imagen)

    # Degradado vertical
    for y in range(alto):
        factor = y / alto
        color = tuple(int(rgb1[i] + (rgb2[i] - rgb1[i]) * factor) for i in range(3))
        draw.line([(0, y), (ancho, y)], fill=color)

    # Capas de luz difusa
    capa = Image.new('RGB', (ancho, alto), (0, 0, 0))
    dcap = ImageDraw.Draw(capa)
    for _ in range(3):
        cx = rng.randint(0, ancho)
        cy = rng.randint(0, alto)
        radio = rng.randint(120, 320)
        tono = tuple(rng.randint(180, 255) for _ in range(3))
        dcap.ellipse((cx - radio, cy - radio, cx + radio, cy + radio), fill=tono)
    capa = capa.filter(ImageFilter.GaussianBlur(70))
    CapaRGB = capa
    imagen = Image.blend(imagen, CapaRGB, 0.18)

    if textura:
        draw2 = ImageDraw.Draw(imagen, 'RGBA')
        paso = rng.randint(28, 46)
        for i in range(-alto, ancho, paso):
            draw2.line([(i, alto), (i + alto, 0)], fill=(255, 255, 255, 18), width=2)
        for i in range(-alto, ancho, paso * 2):
            draw2.line([(i, alto), (i + alto, 0)], fill=(0, 0, 0, 14), width=3)

    nombre_limpo = nombre.strip().replace(' ', '_').lower()
    ruta = f'{nombre_limpo}_{rng.randint(1000, 9999)}.jpg'
    return _guardar(imagen, ruta)


def generar_textura_tela(nombre, color_inicio=None, color_fin=None, semilla=7):
    """Imagen en formato vertical apta para prendas y telas."""
    return generar_imagen(nombre, semilla, color_inicio, color_fin, textura=True)


def generar_textura_ancha(nombre, color_inicio=None, color_fin=None, semilla=7):
    """Imagen panorámica (16:9) para tendencias, colecciones y banners."""
    return generar_imagen(
        nombre, semilla, color_inicio, color_fin, textura=True, ancho=1600, alto=900
    )