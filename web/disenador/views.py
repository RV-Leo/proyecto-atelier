"""Vistas del asistente de diseño con IA."""

from django.contrib import messages
from django.contrib.auth.decorators import login_required
from django.shortcuts import get_object_or_404, redirect, render
from django.urls import reverse
from django.views.decorators.http import require_POST

from .forms import DisenoForm
from .models import Conversacion, Mensaje, PropuestaDiseno
from .servicios import obtener_proveedor


def asistente(request):
    """Pantalla del asistente: conversación y generación de propuestas."""
    form = DisenoForm()
    conversacion = None
    propuestas = []

    conv_id = request.GET.get('c')
    if conv_id and request.user.is_authenticated:
        conversacion = get_object_or_404(Conversacion, pk=conv_id, usuario=request.user)
        propuestas = list(conversacion.propuestas.all())
    elif request.session.get('propuestas_demo'):
        propuestas = request.session.pop('propuestas_demo', [])

    if request.user.is_authenticated and not conversacion:
        conversacion = request.user.conversaciones.first()

    if request.method == 'POST':
        form = DisenoForm(request.POST)
        if form.is_valid():
            datos = dict(form.cleaned_data)
            resumen = datos.pop('descripcion')
            proveedor = obtener_proveedor()
            try:
                resultado = proveedor.generar(resumen, datos)
            except NotImplementedError as error:
                messages.error(request, str(error))
                return render(request, 'disenador/asistente.html', {
                    'form': form, 'conversacion': None, 'propuestas': [],
                })

            propuestas_generadas = resultado['propuestas']

            if request.user.is_authenticated:
                conversacion = Conversacion.objects.create(
                    usuario=request.user,
                    titulo=resumen[:60] + ('…' if len(resumen) > 60 else ''),
                )
                Mensaje.objects.create(
                    conversacion=conversacion, rol=Mensaje.Rol.USUARIO, texto=resumen
                )
                lista = '\n'.join(
                    f'{i + 1}. {p["titulo"]}: {p["resumen"]}' for i, p in enumerate(propuestas_generadas)
                )
                Mensaje.objects.create(
                    conversacion=conversacion,
                    rol=Mensaje.Rol.ASISTENTE,
                    texto=f'{resultado["intro"]}\n{lista}',
                )
                for p in propuestas_generadas:
                    PropuestaDiseno.objects.create(
                        usuario=request.user,
                        conversacion=conversacion,
                        prompt=resumen,
                        tipo_prenda=datos['tipo_prenda'],
                        estilo=datos['estilo'],
                        temporada=datos['temporada'],
                        calidad=datos['calidad'],
                        paleta=datos['paleta'],
                        resultado_resumen=p['resumen'],
                        imagen=p['imagen'],
                        es_favorita=False,
                        es_demo=True,
                    )
                messages.success(
                    request,
                    'Tus propuestas de demostración se guardaron. '
                    'Explóralas y marca tus favoritas.',
                )
                return redirect(f'{reverse("disenador:asistente")}?c={conversacion.pk}')
            else:
                for propuesta in propuestas_generadas:
                    propuesta['resultado_resumen'] = propuesta.get('resumen', '')
                request.session['propuestas_demo'] = propuestas_generadas
                messages.info(
                    request,
                    'Inicia sesión para guardar y reutilizar tus propuestas de diseño.',
                )
                return redirect('disenador:asistente')

    contexto = {
        'form': form,
        'conversacion': conversacion,
        'propuestas': propuestas,
    }
    return render(request, 'disenador/asistente.html', contexto)


@login_required
@require_POST
def alternar_favorita(request, pk):
    """Marca o desmarca una propuesta como favorita (solo del propietario)."""
    propuesta = get_object_or_404(PropuestaDiseno, pk=pk, usuario=request.user)
    propuesta.es_favorita = not propuesta.es_favorita
    propuesta.save(update_fields=['es_favorita'])
    if propuesta.es_favorita:
        messages.success(request, 'Propuesta guardada en tus favoritas.')
    else:
        messages.success(request, 'Propuesta quitada de favoritas.')
    siguiente = request.POST.get('next') or 'cuentas:perfil'
    return redirect(siguiente)


@login_required
def historial(request):
    """Historial de propuestas de diseño del usuario."""
    propuestas = request.user.propuestas.all()
    filtro = request.GET.get('filtro', 'todas')
    if filtro == 'favoritas':
        propuestas = propuestas.filter(es_favorita=True)
    contexto = {
        'propuestas': propuestas,
        'filtro': filtro,
        'total': propuestas.count(),
    }
    return render(request, 'disenador/historial.html', contexto)