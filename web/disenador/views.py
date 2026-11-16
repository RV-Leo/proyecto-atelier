"""Controladores HTTP del asistente de diseño."""

from django.contrib import messages
from django.contrib.auth.decorators import login_required
from django.shortcuts import redirect, render
from django.urls import reverse
from django.views.decorators.http import require_POST

from .forms import DisenoForm
from .ia_gateway import PuenteIAError
from .services import DisenoService

servicio_diseno = DisenoService()


def asistente(request):
    """Recibe la solicitud del usuario y presenta el resultado del asistente."""
    formulario = DisenoForm()
    propuestas_demo = None
    conv_id = request.GET.get('c')
    if not (conv_id and request.user.is_authenticated) and request.session.get('propuestas_demo'):
        propuestas_demo = request.session.pop('propuestas_demo', [])
    conversacion, propuestas = servicio_diseno.estado_asistente(
        request.user, conv_id, propuestas_demo
    )

    if request.method == 'POST':
        formulario = DisenoForm(request.POST)
        if formulario.is_valid():
            datos = dict(formulario.cleaned_data)
            resumen = datos.pop('descripcion')
            try:
                generado = servicio_diseno.generar(request.user, resumen, datos)
            except NotImplementedError as error:
                messages.error(request, str(error))
            except PuenteIAError as error:
                messages.error(request, str(error))
            else:
                conversacion = generado['conversacion']
                propuestas_generadas = generado['propuestas']
                es_demo = generado['resultado'].get('demostracion', True)
                if request.user.is_authenticated:
                    mensajes_guardado = (
                        'Tus propuestas de demostración se guardaron.'
                        if es_demo else 'Tus imágenes generadas por IA se guardaron.'
                    )
                    messages.success(request, f'{mensajes_guardado} Explóralas y marca tus favoritas.')
                    return redirect(f'{reverse("disenador:asistente")}?c={conversacion.pk}')

                request.session['propuestas_demo'] = propuestas_generadas
                messages.info(
                    request,
                    'Inicia sesión para guardar y reutilizar tus propuestas de diseño.',
                )
                return redirect('disenador:asistente')

    contexto = {
        'form': formulario,
        'conversacion': conversacion,
        'propuestas': propuestas,
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
        conversacion = servicio_diseno.conversar(
            request.user, texto, conversacion_id=conversacion_id
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
