"""Comando idempotente para crear el superusuario de desarrollo."""

from django.contrib.auth import get_user_model
from django.core.management.base import BaseCommand, CommandError

Usuario = get_user_model()


class Command(BaseCommand):
    help = (
        'Crea el superusuario de desarrollo (admin). Es idempotente: si el '
        'usuario ya existe no lo duplica ni cambia su contraseña.'
    )

    def add_arguments(self, parser):
        parser.add_argument('--username', default='admin', help='Nombre de usuario (por defecto "admin").')
        parser.add_argument('--email', default='admin@atelier.local', help='Correo del usuario.')
        parser.add_argument('--password', default='admin', help='Contraseña (solo desarrollo local).')
        parser.add_argument('--noinput', action='store_true', help='No solicitar confirmación.')

    def handle(self, *args, **options):
        username = options['username']
        password = options['password']
        email = options['email']

        if not password:
            raise CommandError('Se requiere una contraseña.')

        if Usuario.objects.filter(username=username).exists():
            guard = Usuario.objects.get(username=username)
            if guard.is_superuser:
                self.stdout.write(
                    self.style.WARNING(
                        f'El superusuario "{username}" ya existe. No se modificó nada.'
                    )
                )
                return
            # Un usuario normal ocupa el nombre; se reporta sin alterarlo.
            self.stdout.write(
                self.style.WARNING(
                    f'Existe un usuario "{username}" pero no es superusuario. '
                    'No se modificó nada.'
                )
            )
            return

        superusuario = Usuario.objects.create_superuser(
            username=username, email=email, password=password
        )
        self.stdout.write(
            self.style.SUCCESS(f'Superusuario "{username}" creado correctamente.')
        )