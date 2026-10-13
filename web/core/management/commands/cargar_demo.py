"""Comando para cargar datos de demostración de ATELIER (idempotente).

Si un objeto ya existe no se duplica ni se sobrescribe; las imágenes se
generan solo la primera vez. El superusuario de desarrollo se crea si falta.
"""

from django.contrib.auth import get_user_model
from django.core.management.base import BaseCommand

from catalogo.models import Categoria, Material, Producto
from core import placeholder
from cuentas.models import Coleccion
from disenador.models import Conversacion, Mensaje, PropuestaDiseno
from tendencias.models import Tendencia

Usuario = get_user_model()


class Command(BaseCommand):
    help = 'Carga datos iniciales de demostración de ATELIER sin duplicarlos.'

    def handle(self, *args, **options):
        creados = []

        # --- Superusuario de desarrollo (idempotente) ---------------------
        if not Usuario.objects.filter(username='admin').exists():
            Usuario.objects.create_superuser('admin', 'admin@atelier.local', 'admin')
            creados.append('superusuario "admin"')

        # --- Usuario de demostración --------------------------------------
        if not Usuario.objects.filter(username='atelier_demo').exists():
            Usuario.objects.create_user('atelier_demo', 'demo@atelier.local', 'demo12345')
            creados.append('usuario demo "atelier_demo"')

        # --- Categorías ---------------------------------------------------
        categorias_ok = self._categorias(creados)

        # --- Productos ----------------------------------------------------
        self._productos(creados)

        # --- Materiales (inventario) --------------------------------------
        self._materiales(creados)

        # --- Tendencias ---------------------------------------------------
        self._tendencias(creados)

        # --- Colecciones recomendadas -------------------------------------
        self._colecciones(creados)

        demo, _ = Usuario.objects.get_or_create(username='atelier_demo')
        self._propuesta_demo(demo, creados)

        if creados:
            self.stdout.write(self.style.SUCCESS('Datos de demostración creados:'))
            for nombre in creados:
                self.stdout.write(f'  · {nombre}')
        else:
            self.stdout.write(self.style.WARNING(
                'Los datos de demostración ya estaban cargados. No se duplicó nada.'
            ))

    # ------------------------------------------------------------------ #

    def _crear(self, modelo, clave, valores, creados, etiqueta=None):
        objeto, creado = modelo.objects.get_or_create(**clave, defaults=valores)
        if creado:
            creados.append(etiqueta or str(objeto))
        return objeto, creado

    def _categorias(self, creados):
        categorias = [
            ('Vestidos', 'Prendas de una sola pieza con siluetas fluidas.', 1),
            ('Camisas y blusas', 'Parte inferior con estructura y versatilidad.', 2),
            ('Pantalones y faldas', 'Piezas inferiores para todo el año.', 3),
            ('Chaquetas y abrigos', 'Capas abrigadoras con carácter.', 4),
            ('Telas y tejidos', 'Materiales por metro para producción.', 5),
            ('Accesorios', 'Detalles y acabados para completar el look.', 6),
        ]
        mapa = {}
        for nombre, descripcion, orden in categorias:
            objeto, _ = self._crear(
                Categoria,
                {'nombre': nombre},
                {'descripcion': descripcion, 'orden': orden},
                creados,
            )
            mapa[nombre] = objeto
        return mapa

    def _productos(self, creados):
        datos = [
            # (nombre, tipo, categoria, estilo, temporada, color, composicion, precio, unidades, flags)
            ('Vestido largo fluido', 'PRENDA', 'Vestidos', 'Minimalista', 'PV',
             'Azul marino', '100% algodón orgánico', '84.00', 12, dict(es_destacado=True, es_tendencia=True)),
            ('Vestido midi drapeado', 'PRENDA', 'Vestidos', 'Elegante', 'OI',
             'Bordeaux', '95% viscosa, 5% elastano', '98.00', 8, dict(es_tendencia=True)),
            ('Vestido de lino natural', 'PRENDA', 'Vestidos', 'Boho', 'PV',
             'Beige', '100% lino', '76.50', 6, dict(es_nuevo=True)),
            ('Camisa de algodón clásica', 'PRENDA', 'Camisas y blusas', 'Clásico', 'AT',
             'Blanco', '100% algodón', '42.00', 25, dict(es_destacado=True)),
            ('Blusa con lazo en seda', 'PRENDA', 'Camisas y blusas', 'Romántico', 'OI',
             'Gris perla', '100% seda', '92.00', 9, dict(es_tendencia=True)),
            ('Camisa oversize de popelina', 'PRENDA', 'Camisas y blusas', 'Minimalista', 'AT',
             'Verde oliva', '100% algodón', '55.00', 14, dict(es_nuevo=True)),
            ('Pantalón de vestir de lana', 'PRENDA', 'Pantalones y faldas', 'Clásico', 'OI',
             'Gris', '98% lana, 2% elastano', '88.00', 11, dict(es_destacado=True)),
            ('Falda midi plisada', 'PRENDA', 'Pantalones y faldas', 'Elegante', 'PV',
             'Azul marino', '100% poliéster reciclado', '64.00', 7, dict(es_tendencia=True)),
            ('Pantalón cargo utilitario', 'PRENDA', 'Pantalones y faldas', 'Deportivo', 'AT',
             'Verde oliva', '98% algodón, 2% elastano', '72.00', 0, {}),
            ('Jeans rectos lavado medio', 'PRENDA', 'Pantalones y faldas', 'Casual', 'AT',
             'Azul', '99% algodón, 1% elastano', '68.00', 18, dict(es_destacado=True)),
            ('Chaquetón de lana camel', 'PRENDA', 'Chaquetas y abrigos', 'Clásico', 'OI',
             'Camel', '90% lana, 10% cachemira', '190.00', 5, dict(es_tendencia=True)),
            ('Chaqueta sastre cropped', 'PRENDA', 'Chaquetas y abrigos', 'Elegante', 'AT',
             'Negro', '96% lana, 4% elastano', '140.00', 10, dict(es_destacado=True)),
            ('Abrigo de paño azul marino', 'PRENDA', 'Chaquetas y abrigos', 'Clásico', 'OI',
             'Azul marino', '80% lana, 20% poliamida', '210.00', 4, {}),
            ('Lino estampado primavera', 'TELA', 'Telas y tejidos', 'Boho', 'PV',
             'Beige', '100% lino', '18.50', 60, dict(es_destacado=True)),
            ('Seda satén duquesa', 'TELA', 'Telas y tejidos', 'Elegante', 'OI',
             'Bordeaux', '100% seda', '34.00', 25, dict(es_tendencia=True)),
            ('Algodón orgánico popelina', 'TELA', 'Telas y tejidos', 'Clásico', 'AT',
             'Blanco', '100% algodón orgánico', '11.90', 120, dict(es_destacado=True)),
            ('Lana cachemira molton', 'TELA', 'Telas y tejidos', 'Clásico', 'OI',
             'Camel', '95% lana, 5% cachemira', '42.00', 30, dict(es_nuevo=True)),
            ('Gasa de viscosa estampada', 'TELA', 'Telas y tejidos', 'Romántico', 'PV',
             'Verde oliva', '100% viscosa', '9.80', 45, dict(es_nuevo=True)),
            ('Gabardina técnica impermeable', 'TELA', 'Telas y tejidos', 'Deportivo', 'AT',
             'Azul marino', '100% poliéster reciclado', '15.50', 0, {}),
            ('Cinturón de cuero marrón', 'ACCESORIO', 'Accesorios', 'Clásico', 'AT',
             'Marrón', 'Piel curtida', '28.00', 20, dict(es_destacado=True)),
            ('Pañuelo de seda estampado', 'ACCESORIO', 'Accesorios', 'Elegante', 'PV',
             'Multicolor', '100% seda', '38.00', 16, dict(es_tendencia=True)),
            ('Set de botones de nácar', 'ACCESORIO', 'Accesorios', 'Clásico', 'AT',
             'Natural', 'Nácar', '6.50', 200, dict(es_nuevo=True)),
        ]

        for (nombre, tipo, nombre_cat, estilo, temporada, color,
             composicion, precio, unidades, flags) in datos:
            valores = {
                'tipo': tipo,
                'categoria': Categoria.objects.get(nombre=nombre_cat),
                'estilo': estilo,
                'temporada': temporada,
                'color': color,
                'composicion': composicion,
                'precio': precio,
                'unidades': unidades,
                'descripcion': f'{nombre} en color {color.lower()} con estilo {estilo.lower()}.',
                **flags,
            }
            objeto, creado = self._crear(
                Producto, {'nombre': nombre}, valores, creados,
            )
            if creado and not objeto.imagen:
                ruta = placeholder.generar_textura_tela(
                    f'productos/{objeto.slug}', color_inicio=_color_inicio(composicion, color)
                )
                objeto.imagen = ruta
                objeto.save(update_fields=['imagen'])

    def _materiales(self, creados):
        datos = [
            # (categoria, nombre, color, composicion, tipo, cantidad, unidad, minimo, precio, ubicacion)
            ('Tejidos base', 'Algodón orgánico popelina', 'Blanco', '100% algodón orgánico',
             'Popelina', '120', 'm', '30', '11.90', 'Almacén A'),
            ('Tejidos base', 'Lino natural', 'Beige', '100% lino',
             'Lino', '60', 'm', '15', '18.50', 'Almacén A'),
            ('Tejidos base', 'Viscosa drapeada', 'Azul marino', '100% viscosa',
             'Viscosa', '8', 'm', '20', '9.80', 'Almacén A'),
            ('Tejidos base', 'Jersey de punto', 'Gris', '95% algodón, 5% elastano',
             'Jersey', '45', 'm', '18', '12.50', 'Almacén B'),
            ('Tejidos premium', 'Seda satén', 'Bordeaux', '100% seda',
             'Satén', '25', 'm', '10', '34.00', 'Almacén C'),
            ('Tejidos premium', 'Lana cachemira', 'Camel', '95% lana, 5% cachemira',
             'Molton', '30', 'm', '12', '42.00', 'Almacén C'),
            ('Tejidos técnicos', 'Gabardina técnica', 'Azul marino', '100% poliéster reciclado',
             'Gabardina', '0', 'm', '15', '15.50', 'Almacén B'),
            ('Tejidos técnicos', 'Entretela adhesiva', 'Blanco', 'Poliamida',
             'Entretela', '12', 'm', '10', '4.50', 'Almacén B'),
            ('Pieles', 'Cuero curtido vegano', 'Marrón', 'Poliuretano reciclado',
             'Cuero', '90', 'm', '25', '20.00', 'Almacén C'),
            ('Ferretería', 'Botones de nácar', 'Natural', 'Nácar natural',
             'Nácar', '200', 'u', '40', '6.50', 'Cajones D'),
            ('Hilo y avíos', 'Hilo de algodón 100%', 'Blanco', '100% algodón',
             'Hilo', '80', 'u', '20', '2.20', 'Cajones D'),
            ('Hilo y avíos', 'Cremalleras metálicas', 'Plata', 'Metal y poliéster',
             'Cremallera', '35', 'u', '15', '3.80', 'Cajones D'),
        ]
        for (categoria, nombre, color, composicion, tipo, cantidad,
             unidad, minimo, precio, ubicacion) in datos:
            self._crear(
                Material,
                {'nombre': nombre, 'color': color},
                {
                    'categoria': categoria,
                    'composicion': composicion,
                    'tipo_tela': tipo,
                    'cantidad': cantidad,
                    'unidad_medida': unidad,
                    'stock_minimo': minimo,
                    'precio_unitario': precio,
                    'ubicacion': ubicacion,
                },
                creados,
            )

    def _tendencias(self, creados):
        datos = [
            ('Azul marino profundo', 'COLOR', 'Otoño-Invierno', '#102238', 92, 'El nuevo negro del armario.', 1),
            ('Terracota cálida', 'COLOR', 'Atemporal', '#C1643B', 88, 'Tierra, calidez y carácter.', 2),
            ('Verde oliva militar', 'COLOR', 'Atemporal', '#4A5D23', 81, 'Verde sutil para piezas utilitarias y sobrias.', 3),
            ('Seda satinada', 'TELA', 'Otoño-Invierno', '#B8CBD5', 90, 'Brillo sutil para noches editoriales.', 4),
            ('Lino arrugado', 'TELA', 'Primavera-Verano', '#DCE6EB', 84, 'Natural y fresco, con textura imperfecta.', 5),
            ('Lana de cachemira', 'TELA', 'Otoño-Invierno', '#C9B8A3', 87, 'Suavidad y abrigo premium.', 6),
            ('Minimalismo sobrio', 'ESTILO', 'Atemporal', '', 91, 'Líneas limpias, tonos neutros, cero ruido.', 7),
            ('Oversize editorial', 'ESTILO', 'Atemporal', '', 85, 'Volúmenes amplios con porte contemporáneo.', 8),
            ('Boho contemporáneo', 'ESTILO', 'Primavera-Verano', '', 78, 'Encajes, capas y siluetas relajadas.', 9),
            ('Vestidos fluidos', 'PRENDA', 'Primavera-Verano', '', 89, 'Caída libre para el verano.', 10),
            ('Sastrería relajada', 'PRENDA', 'Atemporal', '', 86, 'Corte clásico, actitud moderna.', 11),
            ('Primavera-Verano 2026', 'TEMPORADA', 'Primavera-Verano', '', 95, 'Luz, lino y paletas claras.', 12),
            ('Otoño-Invierno 2026', 'TEMPORADA', 'Otoño-Invierno', '#102238', 82, 'Profundidad, lana y oscuros elegantes.', 13),
        ]
        for (nombre, tipo, temporada, color, popularidad, descripcion, orden) in datos:
            objeto, creado = self._crear(
                Tendencia,
                {'nombre': nombre},
                {
                    'tipo': tipo,
                    'temporada': temporada,
                    'color': color,
                    'popularidad': popularidad,
                    'descripcion': descripcion,
                    'orden': orden,
                },
                creados,
            )
            if creado and not objeto.imagen:
                objeto.imagen = placeholder.generar_textura_ancha(
                    f'tendencias/{objeto.slug}', color_inicio=(color or '#B8CBD5') or None
                )
                objeto.save(update_fields=['imagen'])

    def _colecciones(self, creados):
        colecciones = [
            ('Atemporal con carácter', 'Clásicos depurados para un guardarropa que no envejece.',
             ['Camisa de algodón clásica', 'Pantalón de vestir de lana', 'Chaqueta sastre cropped',
              'Jeans rectos lavado medio', 'Cinturón de cuero marrón'], '#2E4A66'),
            ('Verano en lino', 'Luz, ligereza y fibras naturales para los días cálidos.',
             ['Vestido de lino natural', 'Lino estampado primavera', 'Falda midi plisada',
              'Vestido largo fluido'], '#8AA1B4'),
            ('Noche de bordeaux', 'Una paleta profunda para salir: seda, sastrería y tonos vinos.',
             ['Vestido midi drapeado', 'Blusa con lazo en seda', 'Seda satén duquesa',
              'Pañuelo de seda estampado'], '#5D4037'),
            ('Uniforme urbano', 'Prendas versátiles con silueta editorial para la ciudad.',
             ['Camisa oversize de popelina', 'Vestido largo fluido', 'Set de botones de nácar',
              'Gabardina técnica impermeable'], '#37474F'),
        ]
        for nombre, descripcion, productos, tono in colecciones:
            objeto, creado = self._crear(
                Coleccion,
                {'nombre': nombre, 'usuario': None},
                {'descripcion': descripcion, 'es_recomendada': True},
                creados,
            )
            if creado:
                objeto.imagen = placeholder.generar_textura_ancha(
                    f'colecciones/{objeto.slug}', color_inicio=tono
                )
                objeto.save(update_fields=['imagen'])
            for nombre_producto in productos:
                producto = Producto.objects.filter(nombre=nombre_producto).first()
                if producto:
                    objeto.productos.add(producto)

    def _propuesta_demo(self, usuario, creados):
        if PropuestaDiseno.objects.filter(usuario=usuario).exists():
            return
        conversacion = Conversacion.objects.create(
            usuario=usuario, titulo='Cápsula de verano en lino'
        )
        Mensaje.objects.create(
            conversacion=conversacion,
            rol=Mensaje.Rol.USUARIO,
            texto='Vestido largo de lino con cuello redondo para primavera-verano.',
        )
        propuesta = PropuestaDiseno(
            usuario=usuario,
            conversacion=conversacion,
            prompt='Vestido largo de lino con cuello redondo para primavera-verano.',
            tipo_prenda='vestido',
            estilo='boho',
            temporada='Primavera-Verano',
            calidad='editorial',
            paleta='azul marino, blanco',
            resultado_resumen='Sugerencia: vestido fluido en lino beige con cintura drapeada.',
            es_favorita=True,
            es_demo=True,
        )
        propuesta.imagen = placeholder.generar_textura_tela(
            'propuestas/capsula_verano_lino', color_inicio='#8AA1B4'
        )
        propuesta.save()
        Mensaje.objects.create(
            conversacion=conversacion,
            rol=Mensaje.Rol.ASISTENTE,
            texto='Te dejé tres ideas de vestido de lino. La primera está marcada como favorita.',
        )
        creados.append('propuesta de demostración para "atelier_demo"')


def _color_inicio(composicion, color):
    """Mapea un color genérico a un tono visual de la imagen de demostración."""
    tonos = {
        'Azul marino': '#2E4A66',
        'Bordeaux': '#5D4037',
        'Beige': '#C9B8A3',
        'Blanco': '#DCE6EB',
        'Gris perla': '#8AA1B4',
        'Verde oliva': '#4A5D23',
        'Gris': '#607D8B',
        'Azul': '#37474F',
        'Camel': '#A1887F',
        'Negro': '#37474F',
        'Marrón': '#8D6E63',
        'Multicolor': '#8AA1B4',
        'Natural': '#C9B8A3',
    }
    return tonos.get(color)