"""Configura de forma idempotente las cuentas y permisos de ATELIER.

Cuentas del entorno local de desarrollo:

  · administracion / 123456  -> administrador de la web (grupo "Administradores").
  · cliente        / 123456  -> cliente normal (grupo "Clientes").
  · admin          / admin   -> superusuario para el panel oficial /admin/.

Advertencia de seguridad
------------------------
Estas contraseñas son EXCLUSIVAMENTE para desarrollo local. No deben usarse en
producción ni guardarse en plantillas, JavaScript ni commits. Este comando no
se ejecuta automáticamente al iniciar el servidor: debe invocarse manualmente.
"""

from django.contrib.auth import get_user_model
from django.contrib.auth.models import Group, Permission
from django.core.management.base import BaseCommand

from core.permisos import GRUPO_ADMINISTRADORES, GRUPO_CLIENTES

Usuario = get_user_model()

APPS_CON_PERMISOS = ('catalogo', 'tendencias', 'disenador', 'cuentas', 'auth')

CUENTAS = [
    {
        'username': 'administracion',
        'password': '123456',
        'email': 'administracion@atelier.local',
        'is_staff': True,
        'is_superuser': False,
        'grupo': GRUPO_ADMINISTRADORES,
    },
    {
        'username': 'cliente',
        'password': '123456',
        'email': 'cliente@atelier.local',
        'is_staff': False,
        'is_superuser': False,
        'grupo': GRUPO_CLIENTES,
    },
    {
        'username': 'admin',
        'password': 'admin',
        'email': 'admin@atelier.local',
        'is_staff': True,
        'is_superuser': True,
        'grupo': None,
    },
]


class Command(BaseCommand):
    help = (
        'Crea o actualiza de forma idempotente las cuentas y permisos de '
        'ATELIER (administracion, cliente y admin). Se puede ejecutar varias '
        'veces sin duplicar usuarios.'
    )

    def handle(self, *args, **options):
        operaciones = []

        grupos = self._asegurar_grupos(operaciones)
        self._asignar_permisos(grupos[GRUPO_ADMINISTRADORES], operaciones)

        for datos in CUENTAS:
            self._asegurar_cuenta(datos, grupos, operaciones)

        self.stdout.write(self.style.SUCCESS('Configuración de cuentas de ATELIER:'))
        for operacion in operaciones:
            self.stdout.write(f'  · {operacion}')
        if not operaciones:
            self.stdout.write(self.style.WARNING('  Nada que cambiar: la configuración ya estaba aplicada.'))

    # ------------------------------------------------------------------ #

    def _asegurar_grupos(self, operaciones):
        grupos = {}
        for nombre in (GRUPO_ADMINISTRADORES, GRUPO_CLIENTES):
            grupo, creado = Group.objects.get_or_create(name=nombre)
            grupos[nombre] = grupo
            if creado:
                operaciones.append(f'grupo "{nombre}" creado')
        return grupos

    def _asignar_permisos(self, grupo_admin, operaciones):
        permisos = Permission.objects.filter(
            content_type__app_label__in=APPS_CON_PERMISOS
        )
        nuevos = [p for p in permisos if not grupo_admin.permissions.filter(pk=p.pk).exists()]
        if nuevos:
            grupo_admin.permissions.add(*nuevos)
            operaciones.append(
                f'{len(nuevos)} permisos añadidos al grupo "{GRUPO_ADMINISTRADORES}"'
            )

    def _asegurar_cuenta(self, datos, grupos, operaciones):
        username = datos['username']
        usuario, creado = Usuario.objects.get_or_create(
            username=username,
            defaults={'email': datos['email']},
        )

        cambios = []
        if creado:
            cambios.append('creada')
        if usuario.email != datos['email']:
            usuario.email = datos['email']
            cambios.append('email')
        if usuario.is_staff != datos['is_staff']:
            usuario.is_staff = datos['is_staff']
            cambios.append('staff')
        if usuario.is_superuser != datos['is_superuser']:
            usuario.is_superuser = datos['is_superuser']
            cambios.append('superusuario')
        if not usuario.is_active:
            usuario.is_active = True
            cambios.append('reactivada')
        usuario.set_password(datos['password'])
        cambios.append('contraseña')
        usuario.save()

        if datos['grupo']:
            if not usuario.groups.filter(name=datos['grupo']).exists():
                usuario.groups.add(grupos[datos['grupo']])
                cambios.append(f'grupo {datos["grupo"]}')

        operaciones.append(f'cuenta "{username}": {", ".join(cambios)}')
