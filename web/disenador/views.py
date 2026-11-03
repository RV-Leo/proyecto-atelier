"""Controladores HTTP del asistente de diseño."""

from django.contrib import messages
from django.contrib.auth.decorators import login_required
from django.shortcuts import redirect, render
from django.urls import reverse
from django.views.decorators.http import require_POST

from .forms import DisenoForm
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
                return render(request, 'disenador/asistente.html', {
                    'form': formulario, 'conversacion': None, 'propuestas': [],
                })

            conversacion = generado['conversacion']
            propuestas_generadas = generado['propuestas']
            if request.user.is_authenticated:
                messages.success(
                    request,
                    'Tus propuestas de demostración se guardaron. '
                    'Explóralas y marca tus favoritas.',
                )
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
