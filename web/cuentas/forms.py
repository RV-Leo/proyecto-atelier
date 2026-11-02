"""Formularios de cuentas: registro, perfil y colecciones."""

from django import forms
from django.contrib.auth.forms import UserCreationForm
from django.contrib.auth.models import User

from .models import Coleccion, Perfil


class RegistroForm(UserCreationForm):
    """Registro de nuevos usuarios de la plataforma web."""

    email = forms.EmailField(label='Correo electrónico', required=True)

    class Meta:
        model = User
        fields = ['username', 'email', 'password1', 'password2']

    def clean_email(self):
        email = self.cleaned_data['email'].lower()
        if User.objects.filter(email__iexact=email).exists():
            raise forms.ValidationError('Ya existe una cuenta con este correo electrónico.')
        return email

    def save(self, commit=True):
        usuario = super().save(commit=False)
        usuario.email = self.cleaned_data['email'].lower()
        if commit:
            usuario.save()
        return usuario


class PerfilForm(forms.ModelForm):
    """Edición de la información del perfil desde la web."""

    class Meta:
        model = Perfil
        fields = [
            'nombre_artistico',
            'bio',
            'ciudad',
            'preferencias',
            'avatar',
            'acepta_comunicaciones',
        ]
        widgets = {
            'bio': forms.Textarea(attrs={'rows': 3}),
            'preferencias': forms.Textarea(
                attrs={'rows': 3, 'placeholder': 'Ej.: telas naturales, paletas sobrias, siluetas amplias…'}
            ),
        }


class ColeccionForm(forms.ModelForm):
    """Creación de colecciones de productos del usuario."""

    class Meta:
        model = Coleccion
        fields = ['nombre', 'descripcion', 'imagen']
        widgets = {
            'descripcion': forms.Textarea(attrs={'rows': 3}),
        }