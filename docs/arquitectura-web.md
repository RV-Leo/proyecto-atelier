# Arquitectura de la aplicación web

La web mantiene Django, sus modelos, rutas, formularios, plantillas y recursos
estáticos. La reorganización separa la entrada HTTP, las reglas de negocio y el
acceso a datos; las URL, los nombres de plantilla, los contextos y las
funcionalidades existentes se conservan.

```text
web/
├── atelier/                 Configuración y rutas raíz de Django
├── core/
│   ├── repositories.py      Consultas para portada, panel e informes
│   ├── services.py          Casos de uso y preparación de indicadores
│   ├── views.py             Controladores HTTP de portada e informes
│   └── panel.py             Controladores HTTP del panel administrativo
├── catalogo/
│   ├── models.py            Producto, categoría y material
│   ├── repositories.py      Consultas de catálogo e inventario
│   ├── services.py          Búsqueda, filtros, paginación y detalle
│   └── views.py             Adaptación de solicitudes y respuestas
├── cuentas/
│   ├── models.py            Perfil, colección y favorito
│   ├── repositories.py      Persistencia y consultas de cuentas
│   ├── services.py          Panel de cliente y casos de uso de colecciones
│   ├── forms.py             Validación de entradas
│   └── views.py             Controladores HTTP de cuenta
├── disenador/
│   ├── models.py            Conversación, mensaje y propuesta
│   ├── repositories.py      Persistencia de conversaciones y propuestas
│   ├── services.py          Orquestación del caso de uso de diseño
│   ├── servicios.py         Proveedores intercambiables (demo/Qwen)
│   └── views.py             Controladores HTTP del asistente
├── tendencias/
│   ├── models.py            Modelo de tendencia
│   ├── repositories.py      Consultas de tendencias
│   ├── services.py          Filtros y datos de presentación
│   └── views.py             Controlador HTTP de tendencias
├── templates/               Interfaz HTML actual, sin rediseño
└── static/                  CSS, JavaScript e imágenes actuales
```

## Responsabilidad de las capas

- **Model** (`models.py`): representa los datos y sus relaciones en Django.
- **Repository** (`repositories.py`): concentra las consultas y escrituras ORM.
- **Service** (`services.py`): aplica reglas y coordina repositorios o proveedores.
- **View** (`views.py`): lee la solicitud, invoca un servicio y elige plantilla,
  redirección o mensajes HTTP.
- **Form** (`forms.py`): valida y normaliza los datos enviados por el usuario.
- **Template y static**: presentan la interfaz y el comportamiento visual.

## Principios SOLID aplicados

- **Responsabilidad única:** cada capa tiene una tarea definida; por ejemplo,
  `catalogo.views` ya no construye consultas ni implementa los filtros.
- **Abierto/cerrado y sustitución:** el asistente usa proveedores con la misma
  operación `generar`; se puede cambiar el proveedor sin alterar la vista.
- **Segregación de interfaces:** los proveedores ofrecen la operación necesaria
  para generar propuestas, sin acoplar la interfaz a detalles del modelo.
- **Inversión de dependencias:** los servicios reciben repositorios (y el
  servicio de diseño recibe su proveedor), por lo que pueden sustituirse sin
  cambiar las vistas.

Las migraciones y los modelos existentes se mantienen en su ubicación actual,
por lo que este cambio de capas no requiere modificar el esquema de la base de
datos.
