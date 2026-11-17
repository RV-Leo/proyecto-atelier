<div align="center">

<img src="docs/atelier-banner.svg" alt="Atelier" width="100%">

### Del hilo a la idea. De la idea a la prenda.

*Diseña prendas, explora tendencias y revisa si puedes producir tus ideas.*

![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Django](https://img.shields.io/badge/Django-092E20?style=for-the-badge&logo=django&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)
![Pollinations.ai](https://img.shields.io/badge/Pollinations.ai-Im%C3%A1genes-FF6F61?style=for-the-badge)
![IA](https://img.shields.io/badge/IA-Generativa-FF6F61?style=for-the-badge)

</div>

---

## ¿Qué es Atelier?

Atelier es un proyecto para el **rubro textil**. Reúne herramientas para crear diseños, consultar tendencias y revisar si una prenda se puede fabricar con los materiales disponibles.

| Módulo | ¿Para qué sirve? |
|---|---|
| **Diseño generativo** | Crear imágenes de prendas a partir de detalles como el tipo, el estilo, los colores y la tela. |
| **Tendencias** | Consultar tendencias de Europa y América a partir de Pinterest y tiendas de marca. |
| **Viabilidad e inventario** | Revisar si hay materiales para producir una prenda y buscar alternativas cuando falte stock. |

## Stack tecnológico

| Capa | Tecnología | Versión |
|---|---|---|
| Backend | Java + Spring Boot + Maven | 21 / 4.1.1 |
| Base de datos | PostgreSQL + Flyway (migraciones V1–V5) | 18 |
| Web | Django (Python) | 5.2 / 3.13 |
| Móvil | Kotlin / Android + Retrofit | — |
| IA de texto | Google AI Studio (Gemini) | `gemini-3.5-flash-lite` |
| IA de imágenes | **Pollinations.ai** (HTTP, sin clave) | — |
| Agentes (prototipo, rama) | FastAPI + Pydantic | 0.115 / 2.9 |

## ¿Cómo funciona?

```
App móvil (Kotlin / Android) ──┐
                               ├──> API REST (Spring Boot) ──┬──> IA de texto (Gemini)
Aplicación web (Django) ───────┘                             └──> IA de imágenes (Pollinations / IA local)
                                               │
                                               v
                                 Base de datos PostgreSQL (atelier)
```

La web y el móvil **no se conectan a PostgreSQL**: piden todo a la API por HTTP. Ver [`docs/base-de-datos.md`](docs/base-de-datos.md).

## Modelos de IA

El proyecto usa **dos proveedores distintos** para la IA y tiene un objetivo a futuro: la IA local del instituto.

| Uso | Proveedor actual | Modelo | ¿Clave? | Estado |
|---|---|---|---|---|
| **Texto** (prompt de imagen, explicación de viabilidad) | Google AI Studio (Gemini), API compatible con OpenAI | `gemini-3.5-flash-lite` | Sí, solo en el backend | En uso |
| **Imágenes de prendas** | **Pollinations.ai** (`GET /prompt/...`) | No hay modelo configurable | **No** | En uso |
| **Objetivo** | WEBUI de IA local del instituto | Qwen-Image / Qwen-Image-Edit | No | Pendiente |
| Tendencias y viabilidad (futuro) | Modelos Qwen locales | Qwen3.6-35B-A3B | No | Pendiente |

**Por qué Pollinations:** Google AI Studio no incluye generación de imágenes en su Free Tier y no queremos pagar. Pollinations devuelve un JPEG real sin clave ni registro, así que sirve como solución temporal hasta conectar la IA local.

**La clave de Google nunca sale del backend hacia las imágenes.** El texto y las imágenes usan clientes separados (`ClienteIaCompuesto`); el cliente de imágenes no envía la cabecera `Authorization`.

En [`docs/modelos-ia.md`](docs/modelos-ia.md) está el diseño original con Qwen.

## Generación de imágenes de diseño (implementado)

La web crea la prenda en el backend y este genera la imagen con la IA. La imagen se guarda en disco y se sirve por HTTP.

```
web: formulario (/disenador/)
  │  POST /api/v1/prendas                                  (crea la prenda)
  │  POST /api/v1/prendas/{id}/diseno/propuestas {"cantidad":1}
  ▼
backend: DisenoServicio
  ├── Gemini redacta el prompt de la prenda                (texto)
  └── ClienteIaCompuesto → ClienteImagenPollinations
        GET https://image.pollinations.ai/prompt/{prompt}?width=768&height=768
        ▼
      JPEG real (768×768, con marca de agua)
        ▼
      GuardadorImagenes → imagenes-diseno/AAAA-MM-DD/uuid.jpg
        ▼
      respuesta: {"imagenUrl": "/imagenes/AAAA-MM-DD/uuid.jpg"}
  ▼
web: descarga la imagen y la guarda en media/propuestas/
  ▼
se muestra en /disenador/ e /disenador/historial/
```

### Clientes de IA del backend

| Archivo | Responsabilidad |
|---|---|
| `ia/GeneradorImagenes.java` | Contrato de un proveedor de imágenes (independiente del de texto). |
| `ia/ClienteImagenPollinations.java` | Llama a Pollinations (`GET /prompt/...`), **sin `Authorization`**, y reintenta si hay límite. |
| `ia/ClienteIaCompuesto.java` | Combina texto (Gemini) e imágenes (Pollinations) sin mezclar protocolos ni claves. |
| `ia/GuardadorImagenes.java` | Guarda los bytes y elige la extensión por firma mágica (jpg/png/gif/webp). |
| `ia/ConfigIa.java` | Registra el cliente compuesto y sirve `/imagenes/**` sin autenticación. |

### Límites del proveedor gratuito

- **1 imagen cada 45 segundos por IP**, máximo 768×768 y con marca de agua.
- Por eso la web pide **una imagen por envío** (`ATELIER_AI_MAX_IMAGENES` en `web/.env`, por defecto 1). Pedir 3 en el mismo segundo devolvía `503`.
- Si se supera el límite, la web muestra un aviso claro; ya no aparece un error 500 con traceback.
- Para sustituir Pollinations por la IA local solo hay que cambiar `IA_URL_IMAGEN` en el backend.

### Variables de entorno del backend

| Variable | Por defecto | Uso |
|---|---|---|
| `IA_URL` | vacío (IA de texto desactivada) | Servidor compatible con OpenAI. En local apunta a Gemini. |
| `IA_API_KEY` | vacío | Clave del proveedor de texto. Vive en `application-local.properties` (ignorado por git). |
| `IA_URL_IMAGEN` | vacío | Proveedor de imágenes distinto del de texto (p. ej. `https://image.pollinations.ai`). |
| `IA_MODELO_TEXTO` | `Qwen3.6-35B-A3B` | Modelo de texto (en local, `gemini-3.5-flash-lite`). |
| `IA_MODELO_IMAGEN` | `Qwen-Image` | Solo para proveedores OpenAI-compatibles; Pollinations no lo usa. |
| `IA_TIMEOUT_SEGUNDOS` | `120` | Pollinations tarda ~12 s por imagen. |
| `IA_DIRECTORIO_IMAGENES` | `imagenes-diseno` | Carpeta donde se guardan los JPEG. |

### Dónde queda cada imagen

| Ubicación | Ruta |
|---|---|
| Backend (disco) | `backend/imagenes-diseno/AAAA-MM-DD/uuid.jpg` |
| Backend (URL) | `http://localhost:8080/imagenes/AAAA-MM-DD/uuid.jpg` |
| Web (copia para mostrar) | `web/media/propuestas/propuesta_{id}_{n}.jpg` |
| PostgreSQL | `propuesta_diseno.imagen_url` (ruta `/imagenes/...`) |
| SQLite / BD de la web | `disenador_propuestadiseno.imagen` (ruta relativa a `media/`) |

## Estructura del proyecto

```
atelier/
├── backend/   → API REST, seguridad, base de datos e IA de imágenes (Spring Boot)
├── web/       → Aplicación web y asistente de diseño (Django)
├── mobile/    → App Android (Kotlin)
├── docs/      → Documentación del proyecto
└── ai-worker/ → Prototipo de agentes asíncronos (rama feature/ai-worker)
```

> `ai-worker/` todavía no está en `main`: es un prototipo FastAPI con agentes **simulados** (diseño, viabilidad, alertas de stock y tendencias). No habla con Gemini ni con la API de inventario. Ver su README en la rama.

## Empezar

1. Clona el repositorio:
```bash
   git clone https://github.com/RV-Leo/proyecto-atelier.git
```
2. Entra en la carpeta del módulo en el que vas a trabajar y sigue las instrucciones de su guía.
3. Crea un archivo `.env` a partir de `.env.example`. No subas tu `.env` a GitHub.

## Web: catálogo e inventario desde la API

La web ya no lee prendas ni materiales de su propia base. `web/catalogo/views.py` usa `web/catalogo/api_cliente.py`, que llama a la API del backend con HTTP Basic. Django conserva SQLite **solo para sus tablas internas** (`auth_user`, sesiones, admin).

1. Levanta el backend en `http://localhost:8080` (ver su guía).
2. Copia `web/.env.example` a `web/.env` y ajusta:

```env
API_BASE_URL=http://localhost:8080/api/v1
API_USER=atelier
API_PASSWORD=atelier-dev
API_TIMEOUT=120
```

3. Instala dependencias y prepara las tablas internas:

```bash
pip install -r web/requirements.txt
python manage.py migrate
python manage.py runserver
```

Qué consume la web:

| Vista | Endpoint |
|---|---|
| Portada (`/`) | `GET /prendas?pagina=1&tamano=12` |
| Catálogo (`/catalogo/`) | `GET /prendas?pagina=&tamano=12&q=&tipo=&estilo=` |
| Detalle (`/catalogo/producto/<slug>/`) | `GET /prendas/slug/<slug>` |
| Inventario (`/catalogo/inventario/`) | `GET /materiales?q=&estado=` |
| Informes (admin) | `GET /prendas` y `GET /materiales` |

Notas:

- `pagina` es **base 1**; `tamano` por defecto 20 y máximo 100.
- Django llama a la API desde el servidor, así que **no hace falta CORS**.
- Si la API no responde, las vistas muestran un aviso y no un error 500.
- Los campos que la API todavía no expone (precio, stock, imagen, favoritos) no se inventan: se ocultan en la interfaz.
- Pruebas sin backend ni PostgreSQL: `python manage.py test catalogo core disenador`.

### Asistente de diseño con IA real

`web/disenador/` genera imágenes reales cuando `ATELIER_AI_PROVEEDOR=backend`. En ese modo `web/disenador/servicios.py` (`ProveedorBackend`) crea la prenda en la API, pide las propuestas, descarga cada imagen y la guarda en `media/propuestas/`.

| Variable (en `web/.env`) | Valor | Uso |
|---|---|---|
| `ATELIER_AI_PROVEEDOR` | `backend` o `demo` | `backend` = IA real; `demo` = propuestas simuladas con Pillow |
| `ATELIER_AI_MAX_IMAGENES` | `1` | Imágenes por envío (límite de Pollinations) |
| `API_TIMEOUT` | `120` | Pollinations tarda ~12 s por imagen |

Si el backend devuelve `503` (límite de Pollinations) o no responde, el asistente muestra un aviso en el formulario y conserva lo que ya habías escrito; no lanza un error 500.

### Móvil (Kotlin) — plan futuro

Hoy el catálogo móvil usa `DatosFake.productos`. Retrofit ya está en el proyecto, pero solo se usa para el chat de IA (`ClienteApiIa`). Para conectarlo al backend:

1. Crear `data/network/ServicioApiAtelier.kt` con Retrofit y Basic auth (`atelier` / `atelier-dev`).
2. Reemplazar `DatosFake.productos` en `HomeScreen` y `DetalleProductoScreen` por llamadas a la API.
3. Para desarrollo local con HTTP sin TLS, agregar `android:usesCleartextTraffic="true"` en el `AndroidManifest.xml` (solo debug).

### Pruebas: quién necesita qué

| Prueba | ¿PostgreSQL? | ¿Backend? |
|---|---|---|
| Web con mocks (`responses`) | No | No |
| Móvil con `MockWebServer` | No | No |
| Integración real web/móvil | No en tu máquina | Sí, local o compartido |
| Tests del backend | Sí | No |

### Lo que no se debe hacer

- Crear tablas de `Producto`/`Material` en SQLite o en `atelier_web` para el catálogo.
- Conectar la web o el móvil directo a PostgreSQL.
- Copiar datos del backend a la fuerza: si falta un campo, se agrega al DTO de la API.

## Cómo colaboramos

- Usamos `main` como rama estable y `develop` para el trabajo diario.
- Para cada tarea, crea una rama con el prefijo correspondiente: `feature/web-...`, `feature/mobile-...` o `feature/backend-...`.
- En los commits usamos estos prefijos: `web:`, `mobile:`, `backend:` y `docs:`.

## Equipo

| Integrante | Rol |
|---|---|
| Leonardo Ronda | *Rol* |
| Yamil Ochoa | *Rol* |
| Antonella Quispe | *Rol* |

<div align="center">


</div>