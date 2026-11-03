<div align="center">

<img src="docs/atelier-banner.svg" alt="Atelier" width="100%">

### Del hilo a la idea. De la idea a la prenda.

*Diseña prendas, explora tendencias y revisa si puedes producir tus ideas.*

![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Django](https://img.shields.io/badge/Django-092E20?style=for-the-badge&logo=django&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
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

## ¿Cómo funciona?

```
App móvil (Kotlin / Android) ──┐
                               ├──> API REST (Spring Boot) ──> Gateway IA (Django) ──> Modelos Qwen
Aplicación web (Django) ───────┘                │                 
                                                v
                                  Base de datos (PostgreSQL propuesto)
```

PostgreSQL es la base de datos del backend (rama `feature/backend-api-inventario`). La web y el móvil **no se conectan a ella**: consumen la API por HTTP. Ver [`docs/base-de-datos.md`](docs/base-de-datos.md).

## Modelos de IA

Para las funciones de IA elegimos modelos de la familia **Qwen**. Se ejecutan en un servidor local y nos ayudan a crear diseños, revisar tendencias y comprobar si hay materiales para producir una prenda.

| Módulo | Modelo | Uso |
|---|---|---|
| Diseño generativo | Qwen-Image / Qwen-Image-Edit | Crear y editar imágenes de prendas |
| Tendencias | Qwen3.6-35B-A3B | Identificar colores, cortes y telas en imágenes |
| Viabilidad e inventario | Qwen3.6-35B-A3B | Comparar una idea con el stock y sugerir alternativas |

En [`docs/modelos-ia.md`](docs/modelos-ia.md) contamos por qué elegimos estos modelos y qué papel cumplen en el proyecto.

## Estructura del proyecto

```
atelier/
├── backend/   → API REST, seguridad y base de datos (Spring Boot)
├── web/       → Aplicación web y conexión con la IA (Django)
├── mobile/    → App Android (Kotlin)
└── docs/      → Documentación del proyecto
```

## Empezar

1. Clona el repositorio:
```bash
   git clone https://github.com/RV-Leo/proyecto-atelier.git
```
2. Entra en la carpeta del módulo en el que vas a trabajar y sigue las instrucciones de su guía.
3. Crea un archivo `.env` a partir de `.env.example`. No subas tu `.env` a GitHub.

## Integración con la API (web y móvil)

> **Regla:** los datos de negocio (prendas, materiales, tendencias) viven en PostgreSQL `atelier` y solo los escribe el backend. La web y el móvil **no se conectan a la base de datos ni crean tablas de producto propias**: piden todo por HTTP.

### Contrato de la API

- Base local: `http://localhost:8080/api/v1`
- Autenticación: HTTP Basic (`atelier` / `atelier-dev` en desarrollo)
- `pagina` es **base 1**; si se omite, la API devuelve todo en una sola página.
- `tamano` por defecto 20, máximo 100.

| Método | Endpoint | Uso |
|---|---|---|
| `GET` | `/prendas?pagina=1&tamano=12&q=&tipo=&estilo=&estado=` | Catálogo con búsqueda y filtros |
| `GET` | `/prendas/slug/{slug}` | Detalle de una prenda |
| `GET` | `/materiales?pagina=1&tamano=12&q=&estado=&activo=` | Inventario |
| `GET` | `/tendencias?pagina=1&tamano=12` | Tendencias |
| `POST` | `/prendas` | Crear una prenda |

Respuesta paginada:

```json
{
  "items": [ ... ],
  "pagina": 1,
  "tamano": 12,
  "total": 3,
  "totalPaginas": 1,
  "primera": true,
  "ultima": true
}
```

### Web (Django) — qué cambiar

Hoy `web/catalogo/views.py` lee `Producto.objects.all()` y `models.py` define `Producto`, `Categoria` y `Material` propios. Eso duplica los datos. Pasos:

1. Agregar al `.env`:

```env
API_BASE_URL=http://localhost:8080/api/v1
API_USER=atelier
API_PASSWORD=atelier-dev
```

2. Crear `web/catalogo/api_cliente.py`:

```python
import os
import requests

API = os.environ.get("API_BASE_URL", "http://localhost:8080/api/v1")
AUTH = (os.environ.get("API_USER", "atelier"),
        os.environ.get("API_PASSWORD", "atelier-dev"))

def listar_prendas(pagina=1, tamano=12, **filtros):
    params = {"pagina": pagina, "tamano": tamano, **filtros}
    r = requests.get(f"{API}/prendas", params=params, auth=AUTH, timeout=5)
    r.raise_for_status()
    return r.json()          # {items, pagina, tamano, total, totalPaginas, ...}

def prenda_por_slug(slug):
    r = requests.get(f"{API}/prendas/slug/{slug}", auth=AUTH, timeout=5)
    r.raise_for_status()
    return r.json()
```

3. En `views.py`, reemplazar el ORM por el cliente:

```python
from . import api_cliente

def lista(request):
    datos = api_cliente.listar_prendas(
        pagina=request.GET.get("pagina", 1),
        q=request.GET.get("q", ""),
        tipo=request.GET.get("tipo", ""),
    )
    return render(request, "catalogo/lista.html", {"pagina": datos})

def detalle(request, slug):
    return render(request, "catalogo/detalle.html",
                  {"prenda": api_cliente.prenda_por_slug(slug)})
```

4. Dejar de usar los modelos `Producto`/`Categoria`/`Material` para el catálogo. Django puede conservar su base **solo para tablas internas** (`auth_user`, sesiones, admin), en `atelier_web`.
5. Probar con `responses` o `requests-mock`: no hace falta backend ni PostgreSQL.

> Como Django llama a la API desde el servidor, **no se necesita CORS**. Solo haría falta si el navegador llamara a la API directamente.

### Móvil (Kotlin) — qué cambiar

Hoy el catálogo usa `DatosFake.productos`. Retrofit ya está en el proyecto, pero solo se usa para el chat de IA (`ClienteApiIa`). Pasos:

1. Crear `data/network/ServicioApiAtelier.kt`:

```kotlin
interface ServicioApiAtelier {
    @GET("api/v1/prendas")
    suspend fun listarPrendas(
        @Query("pagina") pagina: Int = 1,
        @Query("tamano") tamano: Int = 12,
        @Query("q") q: String? = null,
        @Query("tipo") tipo: String? = null
    ): PaginaPrendas
}
```

2. Crear el cliente con Basic auth:

```kotlin
object ClienteApiAtelier {
    // En el emulador, 10.0.2.2 es el localhost de la PC.
    private const val URL_BASE = "http://10.0.2.2:8080/"
    private const val USUARIO = "atelier"
    private const val CLAVE = "atelier-dev"

    val servicio: ServicioApiAtelier by lazy {
        Retrofit.Builder()
            .baseUrl(URL_BASE)
            .addConverterFactory(GsonConverterFactory.create())
            .client(OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val credencial = Credentials.basic(USUARIO, CLAVE)
                    chain.proceed(chain.request().newBuilder()
                        .header("Authorization", credencial)
                        .build())
                }.build())
            .build()
            .create(ServicioApiAtelier::class.java)
    }
}
```

3. Modelar la respuesta (los nombres deben coincidir con el JSON):

```kotlin
data class PaginaPrendas(
    val items: List<PrendaDto>,
    val pagina: Int,
    val total: Int,
    val totalPaginas: Int
)

data class PrendaDto(
    val id: Long,
    val nombre: String,
    val slug: String,
    val tipoPrenda: String,
    val estilo: String?,
    val colores: String?,
    val tela: String?,
    val descripcion: String?,
    val estado: String
)
```

4. Reemplazar `DatosFake.productos` en `HomeScreen` y `DetalleProductoScreen` por llamadas a `ClienteApiAtelier.servicio`.
5. Para desarrollo local con HTTP sin TLS, agregar `android:usesCleartextTraffic="true"` en el `AndroidManifest.xml` (solo debug).
6. Probar con `MockWebServer`: no hace falta backend ni PostgreSQL.

### Pruebas: quién necesita qué

| Prueba | ¿PostgreSQL? | ¿Backend? |
|---|---|---|
| Web con mocks (`responses`) | No | No |
| Móvil con `MockWebServer` | No | No |
| Integración real web/móvil | No en tu máquina | Sí, local o compartido |
| Tests del backend (37) | Sí | No |

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