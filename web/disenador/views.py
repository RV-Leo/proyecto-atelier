"""Controladores HTTP del asistente de diseño."""

from django.contrib import messages
from django.contrib.auth.decorators import login_required
from django.db import OperationalError
from django.shortcuts import redirect, render
from django.urls import reverse
from django.views.decorators.http import require_POST

from .ia_gateway import PuenteIAError
from .services import DisenoService

servicio_diseno = DisenoService()


def asistente(request):
    """Presenta una conversación unificada de consulta y diseño."""
    conv_id = request.GET.get('c')
    if request.GET.get('nueva'):
        conv_id = None
    conversacion, propuestas, timeline = servicio_diseno.estado_asistente(request.user, conv_id)
    if request.GET.get('nueva'):
        conversacion, propuestas, timeline = None, [], []

    contexto = {
        'conversacion': conversacion,
        'propuestas': propuestas,
        'timeline': timeline,
    }
    return render(request, 'disenador/asistente.html', contexto)


@login_required
@require_POST
def conversar(request):
    """Envía un mensaje al puente de IA y conserva el historial de la cuenta."""
    texto = request.POST.get('mensaje', '').strip()
    if not texto:
        messages.error(request, 'Escribe un mensaje para conversar con Atelier IA.')
        return redirect('disenador:asistente')
    if len(texto) > 4000:
        messages.error(request, 'El mensaje no puede superar los 4000 caracteres.')
        return redirect('disenador:asistente')

    conversacion_id = request.POST.get('conversacion_id') or None
    if conversacion_id and not conversacion_id.isdecimal():
        messages.error(request, 'No se encontró esa conversación.')
        return redirect('disenador:asistente')
    try:
        conversacion = servicio_diseno.procesar_turno(
            request.user, texto, request, conversacion_id=conversacion_id
        )
    except PuenteIAError as error:
        messages.error(request, str(error))
        destino = reverse('disenador:asistente')
        if conversacion_id:
            destino += f'?c={conversacion_id}'
        return redirect(f'{destino}#chat')

    return redirect(f'{reverse("disenador:asistente")}?c={conversacion.pk}#chat')


@login_required
@require_POST
def editar_mensaje(request, pk):
    """Edita un mensaje propio y regenera la conversación desde ese turno."""
    texto = request.POST.get('texto', '').strip()
    if not texto:
        messages.error(request, 'El mensaje editado no puede quedar vacío.')
        return redirect('disenador:asistente')
    if len(texto) > 4000:
        messages.error(request, 'El mensaje no puede superar los 4000 caracteres.')
        return redirect('disenador:asistente')

    conversacion_id = None
    try:
        mensaje = servicio_diseno.repository.mensaje_usuario(request.user, pk)
        conversacion_id = mensaje.conversacion_id
        conversacion = servicio_diseno.editar_mensaje(request.user, pk, texto, request)
    except PuenteIAError as error:
        messages.error(request, str(error))
        conversacion = servicio_diseno.repository.conversacion_usuario(
            request.user, conversacion_id
        )
    except OperationalError as error:
        if 'database is locked' not in str(error).lower():
            raise
        messages.error(
            request,
            'La conversación está ocupada y no se pudo guardar el cambio. '
            'Espera un momento y vuelve a intentarlo.',
        )
        destino = reverse('disenador:asistente')
        if conversacion_id:
            destino += f'?c={conversacion_id}'
        return redirect(f'{destino}#chat')
    else:
        messages.success(request, 'Mensaje editado y conversación regenerada desde ese punto.')

    return redirect(f'{reverse("disenador:asistente")}?c={conversacion.pk}#chat')


@login_required
@require_POST
def alternar_favorita(request, pk):
    """Marca o desmarca una propuesta propia como favorita."""
    es_favorita = servicio_diseno.alternar_favorita(request.user, pk)
    if es_favorita:
        messages.success(request, 'Propuesta guardada en tus favoritas.')
    else:
        messages.success(request, 'Propuesta quitada de favoritas.')
    return redirect(request.POST.get('next') or 'cuentas:perfil')


@login_required
def historial(request):
    """Presenta el historial propio, con filtro opcional de favoritas."""
    contexto = servicio_diseno.historial(request.user, request.GET.get('filtro', 'todas'))
    return render(request, 'disenador/historial.html', contexto)
