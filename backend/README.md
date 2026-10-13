# backend — API REST y puente de IA

API REST del proyecto Atelier construida con Spring Boot. Es el punto único de
acceso a los datos: la web (Django) y la app móvil consumen esta API en lugar de
conectarse directamente a la base de datos.

Además, hace de **puente de IA**: habla con el servidor de modelos (Qwen) para
redactar el comentario de viabilidad y generar las propuestas de diseño. Ver
[Puente de IA](#puente-de-ia).

## Decisión de integración: la web consume esta API

El equipo decidió que la web de Django **consume esta API al 100%** para todo
dato de negocio: materiales, prendas, viabilidad, tendencias y diseño. Django
conserva sus propias tablas solo para lo que es suyo (usuarios, sesiones,
perfiles y colecciones).

Lo que eso implica del lado del backend:

- La API es la **única** fuente de verdad del inventario y el catálogo. Django
  no vuelve a leer `material` ni `prenda` de la base de datos.
- Los listados **pagina** con un envoltorio que calca el paginador de Django, y
  los filtros de la web (`q`, `estado`, `tipo`, `estilo`) existen como
  parámetros de consulta.
- Cada prenda lleva un **slug**, porque la web la enlaza por slug y no por id.

Aun así, **ninguna de estas tablas es de Django**: si mañana la web se sustituye,
la API y el esquema no cambian.

## Requisitos

- Java 21
- Maven 3.9+ (o usar el wrapper: `./mvnw`)
- PostgreSQL 14+ corriendo en `localhost:5432`

## Configuración de la base de datos

Crear el rol y la base de datos una sola vez (solo para Postgres local):

```sql
CREATE ROLE atelier WITH LOGIN PASSWORD '<tu-password>';
CREATE DATABASE atelier OWNER atelier ENCODING 'UTF8';
```

Las credenciales **no** se suben al repositorio. Hay dos formas:

- **Local (recomendada para desarrollo):** copiar `application-local.properties`
  con tus credenciales reales. Ese archivo está en `.gitignore`.
- **Producción / CI:** definir variables de entorno.

| Variable | Descripción | Ejemplo |
| --- | --- | --- |
| `POSTGRES_URL` | JDBC URL | `jdbc:postgresql://localhost:5432/atelier` |
| `POSTGRES_USER` | Usuario de PostgreSQL | `atelier` |
| `POSTGRES_PASSWORD` | Contraseña | `secret` |

### Supabase

El proyecto usa **Supabase** como PostgreSQL administrado. La conexión se
configura con las mismas tres variables; solo cambian sus valores.

Supabase ofrece tres formas de conectarse y **no son intercambiables**:

| Modo | Host : Puerto | Usuario | Útil para |
| --- | --- | --- | --- |
| Directo | `db.<ref>.supabase.co` : **5432** | `postgres` | Flyway, backends largos. **IPv6** en plan gratis |
| Pooler session | `aws-0-<region>.pooler.supabase.com` : **5432** | `postgres.<ref>` | Backends en redes IPv4 |
| Pooler transaction | `aws-0-<region>.pooler.supabase.com` : **6543** | `postgres.<ref>` | Serverless — **no usar aquí** |

**Reglas:**

1. **Usar siempre el puerto 5432.** El 6543 (transaction mode) no soporta
   *prepared statements*, que son justo lo que usa Hibernate. Falla con
   errores crípticos de sintaxis en las queries. El propio Supabase recomienda
   5432 para Spring Boot.
2. **Añadir `sslmode=require` a la URL a mano.** El driver JDBC por defecto usa
   `prefer` y, si SSL falla, **silenciosamente** sigue en texto plano.
3. **Flyway necesita session mode o conexión directa**, porque las migraciones
   usan *advisory locks*. Con 6543 las migraciones pueden quedarse a medias.

Ejemplo con pooler session (IPv4, funciona en cualquier red):

```bash
export POSTGRES_URL='jdbc:postgresql://aws-0-ap-northeast-1.pooler.supabase.com:5432/postgres?sslmode=require'
export POSTGRES_USER='postgres.abcdefghijklmnopqrst'
export POSTGRES_PASSWORD='<tu-password-de-supabase>'
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

> Las comillas simples del `export` importan: sin ellas la shell se traga los
> `&` de la URL y lanza el comando en segundo plano.

La contraseña se toma del dashboard de Supabase → **Connect** → **Database** →
**Session pooler** → pestaña **JDBC**. Cualquier carácter reservado de la
contraseña (`&`, `#`, `?`, espacio`) debe ir codificado como `%26`, `%23`,
`%3F` o `%20`.

Si la base de datos está vacía, el esquema se crea solo al arrancar: Flyway
aplica `V1`…`V4` contra Supabase igual que contra Postgres local.

## Ejecutar

```bash
# Perfil local (lee application-local.properties)
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# O con variables de entorno (local)
export POSTGRES_URL=jdbc:postgresql://localhost:5432/atelier
export POSTGRES_USER=atelier
export POSTGRES_PASSWORD=secret
./mvnw spring-boot:run
```

El servicio queda en `http://localhost:8080`.

## Esquema de base de datos

El esquema **no** lo gestiona Hibernate. Se gestiona con **Flyway**, con
archivos SQL en `src/main/resources/db/migration/`:

| Archivo | Contenido |
| --- | --- |
| `V1__diseno_generativo.sql` | `prenda`, `propuesta_diseno` |
| `V2__tendencias.sql` | `tendencia` |
| `V3__inventario_viabilidad.sql` | `material`, `prenda_material`, `analisis_viabilidad`, `alternativa_stock` |
| `V4__mayusculas_enums.sql` | Ajusta los `CHECK` de enums a mayúsculas |
| `V5__prenda_slug.sql` | Añade `prenda.slug`, con índice único |

Al arrancar, Spring Boot aplica las migraciones pendientes automáticamente
(`spring.flyway.enabled=true`).

`spring.jpa.hibernate.ddl-auto=validate` está activo a propósito: Hibernate
**nunca** altera el esquema, solo lo valida contra las entidades. Si cambias una
entidad, tienes que crear una migración nueva.

**Regla:** si agregas o modificas una columna, crea un archivo
`V<n>__descripcion.sql`. Nunca edites una migración que ya corrió (Flyway guarda
un checksum y fallará al arrancar).

## Autenticación

Todos los endpoints requieren autenticación HTTP Basic, excepto los de
`/actuator` y las imágenes en `/imagenes/**`.

- Usuario: `spring.security.user.name` (por defecto `user`)
- Contraseña: `spring.security.user.password` (configúrala en
  `application-local.properties` o en la variable de entorno correspondiente)

CSRF está deshabilitado y no se usan sesiones, porque los consumidores son
clientes de API.

```bash
curl -u user:tu-password http://localhost:8080/api/v1/salud
```

## Endpoints

Base: `/api/v1`

### Salud

| Método | Ruta | Descripción |
| --- | --- | --- |
| GET | `/salud` | Comprobación de que el servicio responde |

### Inventario

| Método | Ruta | Descripción |
| --- | --- | --- |
| GET | `/materiales` | Lista el inventario, con filtros y paginación opcionales |
| GET | `/materiales/activos` | Lista solo los activos |
| GET | `/materiales/{id}` | Detalle de un material |
| POST | `/materiales` | Crea un material |
| PUT | `/materiales/{id}` | Actualiza un material |
| DELETE | `/materiales/{id}` | Elimina un material |

Parámetros de `GET /materiales`:

| Parámetro | Descripción |
| --- | --- |
| `q` | Búsqueda libre en nombre, tipo y color |
| `estado` | `AGOTADO`, `BAJO` o `DISPONIBLE` |
| `activo` | `true` / `false` |
| `pagina` | Número de página, **base 1** (como en Django) |
| `tamano` | Tamaño de página. Por defecto 20, máximo 100 |

`estado` se calcula con la misma regla que ya usaba la web: `AGOTADO` si
`stock <= 0`, `BAJO` si `0 < stock <= stockMinimo`, `DISPONIBLE` en el resto.

Ejemplo:

```bash
curl -u user:tu-password -X POST http://localhost:8080/api/v1/materiales \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "Algodón peinado",
    "tipo": "tela",
    "color": "crudo",
    "unidad": "METRO",
    "stock": "120",
    "stockMinimo": "20",
    "proveedor": "Textiles SAC",
    "precioUnitario": "18.50",
    "activo": true
  }'
```

`unidad` acepta: `METRO`, `UNIDAD`, `KILOGRAMO`, `ROLLO`.

### Prendas

| Método | Ruta | Descripción |
| --- | --- | --- |
| GET | `/prendas` | Lista las prendas, con filtros y paginación opcionales |
| GET | `/prendas/slug/{slug}` | Detalle por slug, como lo abre la web |
| GET | `/prendas/{id}` | Detalle de una prenda |
| POST | `/prendas` | Crea una prenda (estado inicial `BORRADOR`) |
| GET | `/prendas/{id}/materiales` | Ficha técnica con stock disponible |
| POST | `/prendas/{id}/materiales` | Asigna un material a la prenda |

Parámetros de `GET /prendas`: `q` (nombre o descripción), `tipo`, `estilo`,
`estado`, `pagina`, `tamano`.

`slug` se genera solo a partir del nombre (`"Vestido de verano"` →
`vestido-de-verano`). Si dos prendas se llaman igual, la segunda recibe el
sufijo `-2`. La web enlaza cada prenda por su slug, no por su id.

`estado` acepta: `BORRADOR`, `GENERANDO`, `GENERADA`, `ANALIZADA`.

### Paginación

`/materiales`, `/prendas` y `/tendencias` devuelven un envoltorio común:

```json
{
  "items": [ { "...": "..." } ],
  "pagina": 1,
  "tamano": 20,
  "total": 118,
  "totalPaginas": 6,
  "primera": true,
  "ultima": false
}
```

Los nombres calcan el paginador de Django (`count`, `num_pages`, `page.number`)
a propósito, para que la vista no tenga que traducir nada.

**La paginación es opcional.** Si no se mandan `pagina` ni `tamano`, se devuelve
todo en una sola página (`totalPaginas: 1`). Así el catálogo pagina a 12 por
página mientras el inventario y los informes siguen recibiendo la lista
completa.

### Viabilidad

| Método | Ruta | Descripción |
| --- | --- | --- |
| POST | `/viabilidad/{prendaId}` | Compara la ficha técnica con el stock y guarda el veredicto |
| GET | `/viabilidad/{prendaId}/materiales` | Ficha técnica de la prenda |

```bash
curl -u user:tu-password -X POST http://localhost:8080/api/v1/viabilidad/1
```

Respuesta:

```json
{
  "analisisId": 1,
  "prendaId": 1,
  "prenda": "Vestido de verano",
  "viable": false,
  "puntaje": 50,
  "comentario": "No se puede producir Vestido de verano aun: faltan 1 material. Cobertura actual del 50%. Revisa las alternativas sugeridas.",
  "detalles": [
    { "materialId": 1, "material": "Algodón peinado", "unidad": "METRO",
      "requerido": "15.000", "disponible": "120.000", "suficiente": true },
    { "materialId": 2, "material": "Viscosa", "unidad": "METRO",
      "requerido": "10.000", "disponible": "4.000", "suficiente": false }
  ],
  "alternativas": [
    { "materialId": 3, "material": "Viscosa premium", "unidad": "METRO",
      "stockDisponible": "80.000",
      "motivo": "sustituye a Viscosa (stock insuficiente: faltan 6)" }
  ]
}
```

El análisis devuelve **200 aunque el veredicto sea `viable: false`**: "no se
puede producir" es una respuesta válida, no un error.

### Tendencias

| Método | Ruta | Descripción |
| --- | --- | --- |
| GET | `/tendencias` | Lista las tendencias, con paginación opcional |
| GET | `/tendencias/region/{region}` | Filtra por región |
| POST | `/tendencias` | Registra una tendencia capturada |

Ambos `GET` aceptan `pagina` y `tamano`.

`region` acepta: `EUROPA`, `AMERICA`, `GLOBAL`.
`fuente` acepta: `PINTEREST`, `TIENDA_MARCA`, `OTRO`.

`urlOrigem` es única: si la URL ya fue registrada la API responde `409 Conflict`
en lugar de duplicar el registro.

### Diseño

| Método | Ruta | Descripción |
| --- | --- | --- |
| GET | `/prendas/{id}/diseno/prompt` | Prompt de imagen listo para un modelo (lo redacta la IA o cae en plantilla) |
| POST | `/prendas/{id}/diseno/propuestas` | Genera imágenes de diseño y las guarda en disco |
| GET | `/prendas/{id}/diseno/propuestas` | Lista las propuestas generadas |
| PUT | `/prendas/{id}/diseno/propuestas/{propuestaId}/seleccion` | Marca una propuesta como la elegida |

Al generar, la prenda pasa de `BORRADOR` a `GENERADA`. Cada propuesta devuelve
`imagenUrl` bajo `/imagenes/...`, que es un recurso estático **sin
autenticación** (un `<img>` no puede mandar cabeceras Basic Auth).

```bash
# Ver el prompt que se enviaría al modelo
curl -u user:tu-password http://localhost:8080/api/v1/prendas/1/diseno/prompt

# Generar 2 propuestas
curl -u user:tu-password -X POST http://localhost:8080/api/v1/prendas/1/diseno/propuestas \
  -H "Content-Type: application/json" -d '{"cantidad": 2}'
```

Respuesta del prompt:

```json
{ "prompt": "Vestido de verano...", "fuente": "MODELO" }
```

`fuente` es `MODELO` cuando la IA respondió y `PLANTILLA` cuando no estaba
disponible (ver más abajo). **El prompt nunca queda vacío.**

`cantidad` se limita a 4 por petición.

## Puente de IA

El backend **decide**, el modelo **narra**. El cálculo de viabilidad (stock,
puntaje, alternativas) es determinístico y vive en Java; el modelo solo redacta
el comentario y las instrucciones de imagen. Si la IA falla o no está
configurada, la API sigue funcionando igual, con texto de plantilla.

Esto sigue `docs/modelos-ia.md` (pasos 4 y 5): la API no depende de Django para
responder, y el `ClienteIa` abstrae el servidor de inferencia para poder apuntar
a vLLM, Ollama, LM Studio o el gateway de Django sin tocar la lógica.

### Configuración

| Propiedad | Variable de entorno | Por defecto | Descripción |
| --- | --- | --- | --- |
| `atelier.ia.url` | `IA_URL` | *(vacío)* | URL base del servidor compatible con OpenAI, p. ej. `http://localhost:8000/v1`. **Vacío = IA deshabilitada.** |
| `atelier.ia.api-key` | `IA_API_KEY` | *(vacío)* | Bearer opcional |
| `atelier.ia.modelo-texto` | `IA_MODELO_TEXTO` | `Qwen3.6-35B-A3B` | Modelo para chat (comentario, prompt) |
| `atelier.ia.modelo-imagen` | `IA_MODELO_IMAGEN` | `Qwen-Image` | Modelo para imágenes |
| `atelier.ia.timeout-segundos` | `IA_TIMEOUT_SEGUNDOS` | `120` | Límite de espera. `<= 0` = sin límite |
| `atelier.ia.directorio-imagenes` | `IA_DIRECTORIO_IMAGENES` | `imagenes-diseno` | Dónde se guardan los PNG en base64 |

### Comportamiento

| Situación | Texto / imágenes | HTTP |
| --- | --- | --- |
| `IA_URL` vacía (por defecto) | Plantilla local | `200` |
| IA responde | Texto del modelo (`fuente: MODELO`) | `200` |
| IA falla o no responde | Plantilla (texto) / error (imágenes) | `200` / `503` |

Las imágenes **no** tienen respaldo: si el servidor de modelos no genera
imágenes, el endpoint devuelve `503 Service Unavailable`, porque una imagen de
plantilla sería un engaño.

Para habilitar la IA:

```bash
export IA_URL=http://localhost:8000/v1
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

## Errores

Todos los errores devuelven el mismo cuerpo:

```json
{
  "timestamp": "2026-10-13T10:00:00-05:00",
  "status": 400,
  "error": "Bad Request",
  "mensaje": "Los datos enviados no son validos",
  "campos": { "nombre": "el nombre es obligatorio" }
}
```

| Código | Cuándo |
| --- | --- |
| `400` | Fallo de validación o JSON mal formado |
| `401` | Sin autenticación o credenciales incorrectas |
| `404` | El recurso no existe |
| `409` | Conflicto de unicidad (URL de tendencia repetida) |
| `503` | El servidor de IA no respondió al generar imágenes |

## Tests

```bash
./mvnw test
```

## Notas de diseño

- **DTOs en `api/dto/`**: las entidades JPA tienen relaciones lazy; devolverlas
  directamente provocaría `LazyInitializationException` o ciclos en Jackson. Se
  mapean a DTOs dentro de la transacción.
- **Capa de servicio en `servicio/`**: toda la lógica de negocio vive ahí, los
  controladores solo traducen HTTP.
- **`ddl-auto=validate`**: el esquema es responsabilidad de Flyway.
