"""Rutas principales del proyecto ATELIER (módulo web)."""

from django.conf import settings
from django.conf.urls.static import static
from django.contrib import admin
from django.urls import include, path

urlpatterns = [
    path('admin/', admin.site.urls),
    path('', include('core.urls')),
    path('catalogo/', include('catalogo.urls')),
    path('tendencias/', include('tendencias.urls')),
    path('disenador/', include('disenador.urls')),
    path('cuentas/', include('cuentas.urls')),
    path('informes/', include('core.urls_informes')),
]

if settings.DEBUG:
    urlpatterns += static(settings.MEDIA_URL, document_root=settings.MEDIA_ROOT)
    urlpatterns += static(settings.STATIC_URL, document_root=settings.STATICFILES_DIRS[0])