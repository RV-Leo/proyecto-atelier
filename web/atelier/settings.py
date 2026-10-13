"""
Configuración de Django para el proyecto ATELIER (módulo web).

Para desarrollo local se usa SQLite y los archivos de medios en disco.
En producción se deben ajustar SECRET_KEY, DEBUG, ALLOWED_HOSTS y el
almacenamiento de medios/estáticos mediante variables de entorno.
"""

import os
from pathlib import Path

# Build paths inside the project like this: BASE_DIR / 'subdir'.
BASE_DIR = Path(__file__).resolve().parent.parent


# SECURITY WARNING: keep the secret key used in production secret!
SECRET_KEY = os.environ.get(
    'DJANGO_SECRET_KEY',
    'django-insecure-4-^e^619uk(pufqjf+pyy&6&fh8@gi^5iida-n6u$#8fwxbz3w',
)

# SECURITY WARNING: don't run with debug turned on in production!
DEBUG = os.environ.get('DJANGO_DEBUG', '1') == '1'

ALLOWED_HOSTS = [h.strip() for h in os.environ.get('DJANGO_ALLOWED_HOSTS', '*').split(',')]


# Application definition

INSTALLED_APPS = [
    # Aplicaciones nativas de Django
    'django.contrib.admin',
    'django.contrib.auth',
    'django.contrib.contenttypes',
    'django.contrib.sessions',
    'django.contrib.messages',
    'django.contrib.staticfiles',
    # Módulos de ATELIER (web)
    'core',
    'catalogo',
    'tendencias',
    'disenador',
    'cuentas',
]

MIDDLEWARE = [
    'django.middleware.security.SecurityMiddleware',
    'django.contrib.sessions.middleware.SessionMiddleware',
    'django.middleware.common.CommonMiddleware',
    'django.middleware.csrf.CsrfViewMiddleware',
    'django.contrib.auth.middleware.AuthenticationMiddleware',
    'django.contrib.messages.middleware.MessageMiddleware',
    'django.middleware.clickjacking.XFrameOptionsMiddleware',
]

ROOT_URLCONF = 'atelier.urls'

TEMPLATES = [
    {
        'BACKEND': 'django.template.backends.django.DjangoTemplates',
        'DIRS': [BASE_DIR / 'templates'],
        'APP_DIRS': True,
        'OPTIONS': {
            'context_processors': [
                'django.template.context_processors.request',
                'django.contrib.auth.context_processors.auth',
                'django.contrib.messages.context_processors.messages',
            ],
        },
    },
]

WSGI_APPLICATION = 'atelier.wsgi.application'


# Database
# https://docs.djangoproject.com/en/5.2/ref/settings/#databases
# SQLite se mantiene como base de datos de desarrollo según el README.

DATABASES = {
    'default': {
        'ENGINE': 'django.db.backends.sqlite3',
        'NAME': BASE_DIR / 'db.sqlite3',
    }
}


# Password validation
# https://docs.djangoproject.com/en/5.2/ref/settings/#auth-password-validators

AUTH_PASSWORD_VALIDATORS = [
    {
        'NAME': 'django.contrib.auth.password_validation.UserAttributeSimilarityValidator',
    },
    {
        'NAME': 'django.contrib.auth.password_validation.MinimumLengthValidator',
    },
    {
        'NAME': 'django.contrib.auth.password_validation.CommonPasswordValidator',
    },
    {
        'NAME': 'django.contrib.auth.password_validation.NumericPasswordValidator',
    },
]


# Internationalization
# https://docs.djangoproject.com/en/5.2/topics/i18n/

LANGUAGE_CODE = 'es-ec'

TIME_ZONE = 'America/Guayaquil'

USE_I18N = True

USE_TZ = True


# Static files (CSS, JavaScript, Images)
# https://docs.djangoproject.com/en/5.2/howto/static-files/

STATIC_URL = 'static/'
STATICFILES_DIRS = [BASE_DIR / 'static']
STATIC_ROOT = BASE_DIR / 'staticfiles'

# Archivos multimedia (imágenes de productos, propuestas, etc.)
MEDIA_URL = 'media/'
MEDIA_ROOT = BASE_DIR / 'media'

# Default primary key field type
# https://docs.djangoproject.com/en/5.2/ref/settings/#default-auto-field

DEFAULT_AUTO_FIELD = 'django.db.models.BigAutoField'


# Autenticación y redirecciones

LOGIN_URL = 'cuentas:acceder'
LOGIN_REDIRECT_URL = 'core:inicio'
LOGOUT_REDIRECT_URL = 'core:inicio'


# Mensajes (framework de Django)

MESSAGE_TAGS = {
    'debug': 'debug',
    'info': 'info',
    'success': 'success',
    'warning': 'warning',
    'error': 'error',
}


# Asistente de diseño con IA
# --------------------------
# 'demo':  Asistente de demostración (no genera imágenes reales).
#          Las propuestas son simuladas y así se indican en la interfaz.
# Para conectar un modelo real (p. ej. Qwen-Image) en el futuro, crear un
# proveedor en disenador/servicios y cambiar este valor.
AI_PROVEEDOR = os.environ.get('ATELIER_AI_PROVEEDOR', 'demo')
AI_URL_BASE = os.environ.get('ATELIER_AI_URL_BASE', '')