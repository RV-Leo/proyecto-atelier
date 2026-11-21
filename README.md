<div align="center">

<img src="docs/atelier-banner.svg" alt="Atelier" width="100%">

### Del hilo a la idea. De la idea a la prenda.

*Diseña prendas, explora tendencias y revisa si puedes producir tus ideas.*

![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Django](https://img.shields.io/badge/Django-092E20?style=for-the-badge&logo=django&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)
![Google Gemini](https://img.shields.io/badge/Gemini-Texto-4285F4?style=for-the-badge&logo=google)
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
| IA conversacional | **Google Gemini Interactions API** | `gemini-3.5-flash-lite` |
| Generación y edición de imágenes | **Pollinations AI** | `microsoft/mai-image-2.6-flash` |
| Agentes (prototipo, rama) | FastAPI + Pydantic | 0.115 / 2.9 |

## ¿Cómo funciona?

```
App móvil (Kotlin / Android) ──┐
                               ├──> API REST (Spring Boot) ──┬──> Gemini (chat/texto)
Aplicación web (Django) ───────┘
                                                              └──> Pollinations (imágenes)
                                               │
                                               v
                                 Base de datos PostgreSQL (atelier)
```

La web y el móvil **no se conectan a PostgreSQL**: piden todo a la API por HTTP. Ver [`docs/base-de-datos.md`](docs/base-de-datos.md).

## IA utilizada

El chat y las consultas de catálogo usan **Gemini**. La creación y edición de
imágenes se envía aparte a **Pollinations**, con su propia clave. Así las
solicitudes de imagen no consumen el uso de Gemini; el chat sí utiliza el plan o
la cuota de Gemini que tenga configurada la cuenta. Las dos claves se guardan
solo en el backend.

## Generación de imágenes de diseño (implementado)

El cliente conversa desde una sola interfaz. El backend consulta el modelo de
texto, entrega el contexto disponible y genera o edita la imagen solicitada.

```
web: chat unificado (/disenador/)
  ├── consulta de telas y prendas del catálogo
  │  POST /api/v1/prendas                                  (crea la prenda)
  │  POST /api/v1/prendas/{id}/diseno/propuestas {"cantidad":1}
  ├── POST /api/v1/ia/chat                                  (consulta de catálogo)
  └── POST /api/v1/ia/images/edits                          (cambio sobre imagen previa)
  ▼
backend: DisenoServicio
  ├── Gemini: conversación y contexto del catálogo
  └── Pollinations: una imagen por pedido; los cambios usan la imagen anterior
  ▼
web: descarga la imagen y la guarda en media/propuestas/
  ▼
se muestra en /disenador/ e /disenador/historial/
```

### Clientes de IA del backend

| Archivo | Responsabilidad |
|---|---|
| `ia/GeneradorImagenes.java` | Contrato de un proveedor de imágenes (independiente del de texto). |
| `api/IaController.java` | Expone los endpoints de chat y edición de imágenes. |
| `ia/ClienteGemini.java` | Envía conversaciones e historial a la API Interactions de Gemini. |
| `ia/ClienteImagenPollinations.java` | Genera y edita imágenes con la API de Pollinations. |
| `ia/ClienteIaCompuesto.java` | Enruta texto a Gemini e imágenes a Pollinations. |
| `ia/GuardadorImagenes.java` | Guarda los bytes y elige la extensión por firma mágica (jpg/png/gif/webp). |
| `ia/ConfigIa.java` | Registra el cliente compuesto y sirve `/imagenes/**` sin autenticación. |

### Generación y edición

- La web solicita **una imagen por pedido**.
- El chat puede consultar prendas y telas del catálogo; si la API no ofrece un
  dato como el precio minorista de una prenda, el asistente debe indicarlo sin
  inventar una cifra.
- Las ediciones envían la última imagen como referencia y guardan el resultado
  como una versión nueva de la conversación.

### Variables de entorno del backend

| Variable | Por defecto | Uso |
|---|---|---|
| `GEMINI_URL` | `https://generativelanguage.googleapis.com/v1beta` en configuración local | API de texto Gemini. |
| `GEMINI_API_KEY` | clave privada (solo backend) | Autenticación de Gemini. No se sube a Git. |
| `GEMINI_MODELO_TEXTO` | `gemini-3.5-flash-lite` | Modelo para el chat y las consultas. |
| `POLLINATIONS_URL` | `https://gen.pollinations.ai/v1` | API de imágenes Pollinations. |
| `POLLINATIONS_API_KEY` | clave privada (solo backend) | Autenticación de Pollinations, separada de Gemini. |
| `POLLINATIONS_MODELO_IMAGEN` | `microsoft/mai-image-2.6-flash` | Modelo para crear y editar imágenes. |
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

> `ai-worker/` todavía no está en `main`: es un prototipo FastAPI con agentes **simulados** (diseño, viabilidad, alertas de stock y tendencias). Aún no se conecta a proveedores externos de IA ni a la API de inventario. Ver su README en la rama.

## Empezar

1. Clona el repositorio:
```bash
   git clone https://github.com/RV-Leo/proyecto-atelier.git
```
2. Entra en la carpeta del módulo en el que vas a trabajar y sigue las instrucciones de su guía.
3. Crea un archivo `.env` a partir de `.env.example`. No subas tu `.env` a GitHub.

## Aplicación web

La interfaz web usa Django y conserva en SQLite sus datos de catálogo, inventario,
cuentas, colecciones e historial del diseñador. Para las funciones de IA, Django
se comunica con Spring Boot; las claves del proveedor se quedan en el backend.

1. Copia `web/.env.example` como `web/.env`.
2. Instala dependencias y prepara la base de datos:

```bash
cd web
pip install -r requirements.txt
python manage.py migrate
python manage.py cargar_demo
```

3. Inicia el backend y la web. La web queda disponible en `http://127.0.0.1:8000`.

### Chat unificado de Atelier IA

En `/disenador/`, el cliente puede consultar prendas y telas del catálogo, pedir
una imagen de diseño y solicitar cambios sobre la última imagen en la misma
conversación. El chat consulta datos del catálogo, solicita **una imagen por
pedido** y conserva cada edición como una nueva versión. Se requiere iniciar
sesión para guardar y retomar conversaciones.

El archivo `web/.env` configura el proveedor de Django como `backend` y la URL y
credenciales Basic del backend local. Las claves de Gemini y Pollinations se
configuran por separado en `backend/src/main/resources/application-local.properties`;
no se guardan en la web ni se suben a GitHub.

### Preparar el proyecto en otro equipo

El push publica el código, pero no copia las claves de IA, las bases de datos ni
las imágenes generadas localmente. En el nuevo equipo:

1. Instala Java 21 y Python, clona el repositorio y conserva las claves de Gemini
   y Pollinations en un gestor seguro.
2. En PowerShell, configura las claves para la sesión antes de iniciar el
   backend (reemplaza los marcadores localmente; nunca los pegues en Git):

   ```powershell
   $env:GEMINI_API_KEY = "<clave Gemini>"
   $env:POLLINATIONS_API_KEY = "<clave Pollinations>"
   Copy-Item backend/src/main/resources/application-local.properties.example backend/src/main/resources/application-local.properties
   Set-Location backend
   .\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local
   ```

3. En otra terminal, prepara y ejecuta Django:

   ```powershell
   Copy-Item web/.env.example web/.env
   Set-Location web
   py -m venv .venv
   .\.venv\Scripts\Activate.ps1
   pip install -r requirements.txt
   py manage.py migrate
   py manage.py cargar_demo
   py manage.py runserver
   ```

   Abre `http://127.0.0.1:8000`. `web/.env` mantiene el backend en
   `http://localhost:8080`; ambos procesos deben estar activos en ese equipo.
4. Para compartir las mismas cuentas, conversaciones, productos e imágenes entre
   dispositivos se necesita desplegar la web y el backend en servidores
   accesibles y configurar almacenamiento/base de datos compartidos. Los datos
   locales SQLite/H2 y las claves privadas no viajan con GitHub.

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
