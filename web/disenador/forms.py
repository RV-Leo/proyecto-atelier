"""Formularios del asistente de diseño con IA."""

from django import forms

TIPOS_PRENDA = [
    ('vestido', 'Vestido'),
    ('blusa', 'Blusa'),
    ('camisa', 'Camisa'),
    ('pantalon', 'Pantalón'),
    ('falda', 'Falda'),
    ('chaqueta', 'Chaqueta'),
    ('abrigo', 'Abrigo'),
    ('camiseta', 'Camiseta'),
    ('conjunto', 'Conjunto'),
    ('otro', 'Otro'),
]

ESTILOS = [
    ('minimalista', 'Minimalista'),
    ('clasico', 'Clásico'),
    ('elegante', 'Elegante'),
    ('romantico', 'Romántico'),
    ('boho', 'Boho'),
    ('deportivo', 'Deportivo'),
    ('vanguardista', 'Vanguardista'),
    ('casual', 'Casual'),
]

TEMPORADAS = [
    ('Primavera-Verano', 'Primavera-Verano'),
    ('Otoño-Invierno', 'Otoño-Invierno'),
    ('Atemporal', 'Atemporal'),
]

CALIDADES = [
    ('boceto', 'Boceto'),
    ('alta', 'Alta calidad'),
    ('editorial', 'Editorial'),
]

COLORES = [
    ('azul marino', 'Azul marino'),
    ('blanco', 'Blanco'),
    ('beige', 'Beige'),
    ('terracota', 'Terracota'),
    ('verde oliva', 'Verde oliva'),
    ('bordeaux', 'Bordeaux'),
    ('gris', 'Gris'),
    ('negro', 'Negro'),
]


class DisenoForm(forms.Form):
    """Parámetros para solicitar propuestas de diseño."""

    descripcion = forms.CharField(
        label='Describe tu diseño',
        widget=forms.Textarea(
            attrs={
                'rows': 3,
                'placeholder': 'Ej.: vestido largo fluido con cuello redondo, mangas amplias y cintura drapeada…',
            }
        ),
    )
    tipo_prenda = forms.ChoiceField(label='Tipo de prenda', choices=TIPOS_PRENDA)
    estilo = forms.ChoiceField(label='Estilo', choices=ESTILOS)
    temporada = forms.ChoiceField(label='Temporada', choices=TEMPORADAS)
    calidad = forms.ChoiceField(label='Calidad de imagen', choices=CALIDADES)
    paleta = forms.MultipleChoiceField(
        label='Colores', choices=COLORES,
        widget=forms.CheckboxSelectMultiple,
        initial=['azul marino', 'blanco'],
    )

    def clean_paleta(self):
        paleta = self.cleaned_data['paleta']
        return ', '.join(paleta) or 'neutros'