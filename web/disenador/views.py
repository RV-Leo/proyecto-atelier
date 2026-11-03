"""Vistas del asistente de diseño con IA."""

from django.contrib import messages
from django.conf import settings
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
            if proveedor.nombre == 'backend' and not request.user.is_authenticated:
                messages.error(request, 'Inicia sesión para consultar el modelo de IA.')
                return render(request, 'disenador/asistente.html', {
                    'form': form,
                    'conversacion': conversacion,
                    'propuestas': propuestas,
                    'modo_demo': settings.AI_PROVEEDOR == 'demo',
                })
            try:
                resultado = proveedor.generar(resumen, datos)
            except RuntimeError as error:
                messages.error(request, str(error))
                return render(request, 'disenador/asistente.html', {
                    'form': form,
                    'conversacion': None,
                    'propuestas': [],
                    'modo_demo': settings.AI_PROVEEDOR == 'demo',
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
        'modo_demo': settings.AI_PROVEEDOR == 'demo',
    }
    return render(request, 'disenador/asistente.html', contexto)


@login_required
@require_POST
def conversar(request):
    """Envía una pregunta al modelo y guarda el intercambio en la cuenta."""
    texto = request.POST.get('mensaje', '').strip()
    if not texto:
        messages.error(request, 'Escribe una pregunta antes de enviarla.')
        return redirect('disenador:asistente')
    if len(texto) > 4000:
        messages.error(request, 'La pregunta no puede superar los 4000 caracteres.')
        return redirect('disenador:asistente')

    proveedor = obtener_proveedor()
    if proveedor.nombre != 'backend':
        messages.error(request, 'El chat requiere configurar el proveedor "backend".')
        return redirect('disenador:asistente')

    conversacion_id = request.POST.get('conversacion_id', '').strip()
    if conversacion_id:
        conversacion = get_object_or_404(
            Conversacion,
            pk=conversacion_id,
            usuario=request.user,
        )
    else:
        conversacion = Conversacion.objects.create(
            usuario=request.user,
            titulo=texto[:60] + ('…' if len(texto) > 60 else ''),
        )

    historial = list(
        conversacion.mensajes.order_by('-creada', '-pk')[:19]
    )[::-1]
    mensajes_chat = [
        {
            'role': 'user' if mensaje.rol == Mensaje.Rol.USUARIO else 'assistant',
            'content': mensaje.texto,
        }
        for mensaje in historial
    ]
    mensajes_chat.append({'role': 'user', 'content': texto})

    try:
        respuesta = proveedor.conversar(mensajes_chat)
    except RuntimeError as error:
        messages.error(request, str(error))
        return redirect(f'{reverse("disenador:asistente")}?c={conversacion.pk}')

    Mensaje.objects.bulk_create([
        Mensaje(
            conversacion=conversacion,
            rol=Mensaje.Rol.USUARIO,
            texto=texto,
        ),
        Mensaje(
            conversacion=conversacion,
            rol=Mensaje.Rol.ASISTENTE,
            texto=respuesta,
        ),
    ])
    if not conversacion.titulo:
        conversacion.titulo = texto[:60] + ('…' if len(texto) > 60 else '')
        conversacion.save(update_fields=['titulo', 'actualizada'])
    return redirect(f'{reverse("disenador:asistente")}?c={conversacion.pk}')


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