# proyecto-atelier — Backend

## Rama actual: `feature/backend-api-inventario`

El historial local tiene 5 commits:

1. **`51bf3f0`** — backend: API REST inicial con Spring Boot, seguridad y PostgreSQL
2. **`8c064a9`** — backend: esquema de base de datos con Flyway (7 tablas, 3 módulos)
3. **`1f26a8a`** — backend: API REST de inventario, prendas, viabilidad y tendencias
4. **`5051a51`** — backend: puente de IA para diseño y narración de viabilidad
5. **`014a7a0`** — backend: paginación, filtros y slug para que la web consuma la API

---

## 1. ¿Qué hay este commit?

### API REST + Puente de IA

El backend cumple los dos roles que pide el README:

- **API REST**: CRUD materiales, prendas, viabilidad, tendencias, diseño.
- **Puente de IA**: hace de intermediario entre el backend y los modelos Qwen, según `docs/modelos-ia.md` pasos 4-5. La API **decide** stock/viabilidad determinísticamente; el modelo solo **narra**.

#### Endpoints principales (base `/api/v1`)

| Recurso | Métodos | Notas |
|---|---|---|
| `materiales` | GET/POST/PUT/DELETE | Filtros: `q`, `estado`, `activo`. Paginación opcional |
| `prendas` | GET/POST + `/materiales` | Slug nuevo (`/prendas/slug/{slug}`) |
| `viabilidad/{prendaId}` | POST | Stock real vs. ficha técnica. `viable: false` es 200, no error |
| `tendencias` | GET/POST + `/region/{region}` | Paginación opcional |
| `diseno/prompt` | GET | `fuente: MODELO` o `PLANTILLA` |
| `diseno/propuestas` | POST|GET | 503 si la IA no está |
| `diseno/propuestas/{id}/seleccion` | PUT | Marca la elegida |

#### Puente de IA (detalle)

- `ClienteIa` (interfaz) → `ClienteIaHttp` (cliente HTTP) o `ClienteIaNoDisponible` (fallback).
- `ConfigIa` selecciona el cliente según `propiedades.habilitada()`, no `@ConditionalOnProperty`.
- Si `atelier.ia.url` está vacío (por defecto): IA fuera, API funciona igual con plantilla.
- Si la URL tiene valor: se llama al modelo, se guarda el comentario y las imágenes.
- `GuardadorImagenes` escribe PNG en disco bajo `atelier.ia.directorio-imagenes`, servible en `/imagenes/**` **sin autenticación** (crucial: un `<img>` no puede mandar Basic Auth).
- `IaNoDisponibleException` → HTTP 503.

#### Configuración (variables de entorno)

| Variable | Por defecto | Descripción |
|---|---|---|
| `POSTGRES_URL` | `jdbc:postgresql://127.0.0.1:5432/atelier` | JDBC URL. Supabase usa `sslmode=require` a mano. |
| `POSTGRES_USER` | `atelier` | Usuario. |
| `POSTGRES_PASSWORD` | (vacío) | Contraseña. |
| `IA_URL` | *(vacío)* | URL base del servidor compatible con OpenAI. Vacío = IA fuera. |
| `IA_API_KEY` | (vacío) | Bearer opcional. |
| `IA_MODELO_TEXTO` | `Qwen3.6-35B-A3B` | Modelo para chat (prompt, comentario viabilidad). |
| `IA_MODELO_IMAGEN` | `Qwen-Image` | Modelo para imágenes. |
| `IA_TIMEOUT_SEGUNDOS` | `120` | Límite de espera. `<= 0` = sin límite. |
| `IA_DIRECTORIO_IMAGENES` | `imagenes-diseno` | Donde se guardan los PNG. |

#### Migraciones (Flyway)

- 5 archivos `V1`..`V5__*.sql` en `src/main/resources/db/migration/`.
- `spring.jpa.hibernate.ddl-auto=validate`: Hibernate **nunca** altera el esquema.
- `spring.flyway.schemas=public`: fija el esquema para que coincida local/Supabase.
- **Regla**: si cambias una entidad, crea una migración nueva. No edites una que ya corrió.

#### Autenticación

- HTTP Basic en **todos** los endpoints, **menos** `/actuator/health` e `/actuator/info`.
- `/imagenes/**` es **publico** (no hay Basic Auth), porque un `<img>` no puede mandar cabeceras.
- Usuario por defecto: `atelier`, password: `atelier-dev` (en `application-local.properties`, `.gitignore`).

#### Seguridad

- CSRF deshabilitado, sin sesiones (clientes puros de API).
- Errores: `400` (validación), `401` (sin auth), `404` (no existe), `409` (unicidad URL tendencia), `503` (servidor IA caído).

#### Tests: 37 en verde

- `ApiFlujoTests` — 9 integración contra PostgreSQL real.
- `PuenteIaTests` — 7 pruebas del estado sin IA (mock + infra).
- `ClienteIaHttpTests` — 11 pruebas unitarias del cliente HTTP con `MockRestServiceServer`.
- `ApiCatalogoTests` — 9 pruebas nuevas de paginación, filtros y slug.

---

## 2. Decisión de integración: la web consume la API (Opción A)

### Contexto

Se exploró la rama `origin/web`. El hallazgo central es contundente:

**Django no consume ninguna API hoy.** No hay `serializers.py`, ni `rest_framework`, ni una sola llamada HTTP (`requests`/`httpx` ausentes). Todo se lee de **SQLite por ORM**.

El gap es del **100 %** para todos los endpoints listados en el informe.

### ¿Qué significa "Opción A"?

La decisión tomada es que **Django consuma esta API al 100 %** para todo dato de negocio: materiales, prendas, viabilidad, tendencias y diseño. Django conserva sus propias tablas solo para lo que es suyo (usuarios, sesiones, perfiles y colecciones).

### Lo que el backend cambió para que esto sea posible

| Característica | Qué se hizo |
|---|---|
| **Paginación** | `PaginaResponse` con `items`, `pagina`, `tamano`, `total`, `totalPaginas`, `primera`, `ultima`. Sin parámetros = todo en una página. |
| **Filtros** | `q` (búsqueda libre), `estado` (agotado/bajo/disponible en materiales; estado en prendas), `tipo`, `estilo`. Con `Specification`, no métodos derivados. |
| **Estado del material** | Nuevo enum `EstadoMaterial` (AGOTADO, BAJO, DISPONIBLE). El cálculo vive en la API, no en Python. |
| **Slug** | Campo `slug` en `Prenda`, índice único. Generado con `Slug.de` (replica `django.utils.text.slugify`). Endpoint `GET /prendas/slug/{slug}`. |
| **Migración V5** | Añade `slug` a la tabla `prenda`, con datos de respaldo y índice único. |

### ¿Qué falta todavía para que la web pueda consumirla?

Esta lista depende de la rama `web`, pero aquí están las brechas que ya se empezaron a cubrir en el backend:

| Necesidad en Django | Estado actual |
|---|---|
| Listados paginados (`q`, `pagina`, `tamano`) | API ya devuelve `PaginaResponse`. |
| Filtros de materiales (`q`, `estado`) | API ya los tiene. |
| Filtros de prendas (`q`, `tipo`, `estilo`) | API ya los tiene. |
| Detalle por slug (`/catalogo/<slug>/`) | `GET /prendas/slug/{slug}` ya existe. |
| Estado calculado del material | `EstadoMaterial` en el DTO. |
| Agregados (`Sum(stock)`) para informes/panel | Pendiente — próxima mejora. |

### Migración necesaria en Django

- Añadir campo `slug` a `Producto` (o al modelo que corresponda).
- Ajustar `get_absolute_url` para que use slug.
- Añadir cliente HTTP (`requests`/`httpx`) que consuma los endpoints de esta API.
- Migrar vistas que hoy leen `Material.objects.all()` etc. a consumir la API.

**Decisión tomada:** se documenta aquí para que el equipo se ponga de acuerdo. Si en el futuro se decide la opción B o C, el backend ya tiene la base y no hay que desaprender nada.

---

## 3. Supabase (PostgreSQL gestionado)

El proyecto está preparado para conectar contra Supabase. Los tres parámetros `POSTGRES_URL`/`POSTGRES_USER`/`POSTGRES_PASSWORD` ya están en `application.properties` y son sobreescribibles por variables de entorno.

### Reglas críticas (no romper)

| Regla | Por qué |
|---|---|
| **Usar siempre el puerto 5432** | El 6543 (transaction mode) no soporta *prepared statements*, que usa Hibernate. |
| **Añadir `sslmode=require` a la URL** | El driver JDBC por defecto usa `prefer` y, si SSL falla, **silenciosamente** sigue en texto plano. |
| **Flyway en session mode o directo** | Migraciones usan *advisory locks*; con 6543 pueden quedarse a medias. |
| **Las comillas simples importan en `export`** | Sin ellas la shell se traga los `&` de la URL y lanza el comando en segundo plano. |

### Ejemplo de configuración

```bash
export POSTGRES_URL='jdbc:postgresql://aws-0-ap-northeast-1.pooler.supabase.com:5432/postgres?sslmode=require'
export POSTGRES_USER='postgres.abcdefghijklmnopqrst'
export POSTGRES_PASSWORD='<tu-password-de-supabase>'
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

### ¿Qué falta para verificar?

Necesito las credenciales (project ref + región + contraseña) para conectar de verdad y confirmar que Flyway y los 37 tests pasan contra Supabase. Si alguien las tiene, avísame y lo hago.

---

## 4. Historial de cambios recientes

| Commit | Descripción |
|---|---|
| `014a7a0` | Paginación, filtros y slug para que la web consuma la API |
| `5051a51` | Puente de IA para diseño y narración de viabilidad |
| `1f26a8a` | API REST de inventario, prendas, viabilidad y tendencias |
| `8c064a9` | Esquema de base de datos con Flyway (7 tablas, 3 módulos) |
| `51bf3f0` | API REST inicial con Spring Boot, seguridad y PostgreSQL |

---

## 5. ¿Qué sigue?

| Tema | Próximo paso |
|---|---|
| **Supabase** | Pasar las credenciales y verificar conexión real + tests. |
| **Alineación modelo Django vs. API** | Decidir si `Producto` de Django se mapea a `Prenda` de la API o se mantiene aparte. |
| **Endpoints que necesita Django** | Algunos ya están (paginación, filtros, slug); los agregados (`Sum(stock)`) quedan para después. |
| **Push** | Los 5 commits ya están en la rama remota `feature/backend-api-inventario`. |

---

*Última actualización: `014a7a0` — backend: paginación, filtros y slug para que la web consuma la API*