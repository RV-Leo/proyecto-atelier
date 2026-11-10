"""Crea las cuentas de demostración de ATELIER en cualquier base de datos.

Al ejecutar ``migrate`` (por ejemplo en un clon nuevo o en otro dispositivo)
esta migración garantiza que existan las tres cuentas de desarrollo:

  · administracion / 123456  -> administrador de la web (grupo "Administradores").
  · cliente        / 123456  -> cliente normal (grupo "Clientes").
  · admin          / admin   -> superusuario para el panel oficial /admin/.

Advertencia de seguridad
------------------------
Estas contraseñas son EXCLUSIVAMENTE para desarrollo local. No deben usarse en
producción. Este archivo acompaña a ``manage.py configurar_cuentas`` y solo crea
las cuentas si aún no existen (no pisa contraseñas ya cambiadas).
"""

from django.contrib.auth.hashers import make_password
from django.db import migrations

GRUPO_ADMINISTRADORES = 'Administradores'
GRUPO_CLIENTES = 'Clientes'

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


def crear_cuentas(apps, schema_editor):
    Usuario = apps.get_model('auth', 'User')
    Group = apps.get_model('auth', 'Group')
    Perfil = apps.get_model('cuentas', 'Perfil')

    grupos = {}
    for nombre in (GRUPO_ADMINISTRADORES, GRUPO_CLIENTES):
        grupo, _ = Group.objects.get_or_create(name=nombre)
        grupos[nombre] = grupo

    for datos in CUENTAS:
        usuario = Usuario.objects.filter(username=datos['username']).first()
        if usuario is None:
            usuario = Usuario(
                username=datos['username'],
                email=datos['email'],
                is_staff=datos['is_staff'],
                is_superuser=datos['is_superuser'],
                is_active=True,
                password=make_password(datos['password']),
            )
            usuario.save()
        if datos['grupo']:
            usuario.groups.add(grupos[datos['grupo']])
        Perfil.objects.get_or_create(usuario=usuario)


class Migration(migrations.Migration):

    initial = True

    dependencies = [
        ('auth', '0012_alter_user_first_name_max_length'),
        ('cuentas', '0001_initial'),
    ]

    operations = [
        migrations.RunPython(crear_cuentas, migrations.RunPython.noop),
    ]
