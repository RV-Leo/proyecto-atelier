"""Pruebas del módulo de tendencias."""

from django.test import TestCase
from django.urls import reverse

from .models import Tendencia


class TendenciasTests(TestCase):
    def setUp(self):
        self.color = Tendencia.objects.create(
            nombre='Azul marino profundo', tipo='COLOR', color='#102238', popularidad=90,
        )
        self.estilo = Tendencia.objects.create(
            nombre='Minimalismo sobrio', tipo='ESTILO', popularidad=80,
        )

    def test_slug_automatico(self):
        self.assertEqual(self.color.slug, 'azul-marino-profundo')

    def test_lista_responde(self):
        respuesta = self.client.get(reverse('tendencias:lista'))
        self.assertEqual(respuesta.status_code, 200)
        self.assertContains(respuesta, 'Azul marino profundo')

    def test_filtro_por_tipo(self):
        respuesta = self.client.get(reverse('tendencias:lista'), {'tipo': 'ESTILO'})
        self.assertContains(respuesta, 'Minimalismo sobrio')
        self.assertNotContains(respuesta, 'Azul marino profundo')