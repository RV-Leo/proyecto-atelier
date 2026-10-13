"""Configuración de Django Admin para el asistente de diseño."""

from django.contrib import admin

from .models import Conversacion, Mensaje, PropuestaDiseno


class MensajeInline(admin.TabularInline):
    model = Mensaje
    extra = 0
    readonly_fields = ['rol', 'texto', 'creada']
    can_delete = False


@admin.register(Conversacion)
class ConversacionAdmin(admin.ModelAdmin):
    list_display = ['titulo', 'usuario', 'cantidad_mensajes', 'creada', 'actualizada']
    list_filter = ['usuario', 'creada']
    search_fields = ['titulo', 'usuario__username']
    inlines = [MensajeInline]

    @admin.display(description='Mensajes')
    def cantidad_mensajes(self, objeto):
        return objeto.mensajes.count()


@admin.register(Mensaje)
class MensajeAdmin(admin.ModelAdmin):
    list_display = ['conversacion', 'rol', 'texto_corto', 'creada']
    list_filter = ['rol', 'creada']
    search_fields = ['texto', 'conversacion__titulo']

    @admin.display(description='Texto')
    def texto_corto(self, objeto):
        return objeto.texto[:60] + ('…' if len(objeto.texto) > 60 else '')


@admin.register(PropuestaDiseno)
class PropuestaDisenoAdmin(admin.ModelAdmin):
    list_display = [
        'id', 'usuario', 'tipo_prenda', 'estilo', 'temporada',
        'es_favorita', 'es_demo', 'creada',
    ]
    list_filter = ['es_favorita', 'es_demo', 'temporada', 'estilo', 'tipo_prenda']
    search_fields = ['prompt', 'resultado_resumen', 'usuario__username']
    list_editable = ['es_favorita']