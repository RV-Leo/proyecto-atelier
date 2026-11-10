# Base de datos de Atelier

## Regla de oro

**Existe una sola fuente de verdad para los datos del negocio: PostgreSQL `atelier`, y solo el backend de Spring Boot la escribe.**

Django (la web) **no guarda prendas, materiales ni tendencias en su propia base**. Cuando necesita esos datos, los pide a la API REST del backend.

```
App móvil (Kotlin) ──┐
                     ├──> API REST (Spring Boot) ──> PostgreSQL: atelier   ← datos de negocio
Web (Django) ────────┘                                    │
                                                          └──> Gateway IA (Django)
```

Django sí necesita una base, pero **solo para sus tablas internas** (usuarios, sesiones, admin, migraciones). Para eso usamos **otra base PostgreSQL**: `atelier_web`. Nunca SQLite.

| Base | Contenido | Quién escribe | Quién lee |
|---|---|---|---|
| `atelier` | `prenda`, `material`, `tendencia`, `propuesta_diseno`, `analisis_viabilidad`, `alternativa_stock`, `prenda_material` | Solo Spring Boot (Flyway + JPA) | Spring Boot; la web y el móvil **vía API** |
| `atelier_web` | `auth_user`, `django_session`, `django_admin_log`, migraciones de Django | Solo Django | Solo Django |

> El backend real vive en la rama `feature/backend-api-inventario` y su esquema lo gestiona **Flyway**. `spring.jpa.hibernate.ddl-auto=validate`: Hibernate nunca crea ni altera columnas. Si cambias una entidad, se agrega una migración nueva.

---

## 1. Crear las bases de datos

El script está en [`docs/sql/init-postgres.sql`](sql/init-postgres.sql).

Ejecutarlo como superusuario `postgres` (pide su contraseña):

```powershell
& "C:\Program Files\PostgreSQL\18\bin\psql.exe" -U postgres -f docs\sql\init-postgres.sql
```

Crea:

- rol `atelier` (contraseña `atelier`, **solo desarrollo local**)
- base `atelier` (negocio)
- base `atelier_web` (tablas internas de Django)

Es idempotente: si ya existen, no falla.

### Verificar

```powershell
$env:PGPASSWORD="atelier"
& "C:\Program Files\PostgreSQL\18\bin\psql.exe" -U atelier -d atelier -c "SELECT current_database(), current_user;"
```

---

## 2. Levantar el backend real (Spring Boot)

El backend de `feature/backend-api-inventario` usa **Maven** y **Flyway**. Configuración por variables de entorno (valores por defecto entre paréntesis):

| Variable | Por defecto | Descripción |
|---|---|---|
| `POSTGRES_URL` | `jdbc:postgresql://127.0.0.1:5432/atelier` | URL JDBC |
| `POSTGRES_USER` | `atelier` | Usuario |
| `POSTGRES_PASSWORD` | *(vacío)* | Contraseña |
| `API_USER` | `atelier` | Usuario HTTP Basic |
| `API_PASSWORD` | *(vacío)* | Contraseña HTTP Basic |
| `IA_URL` | *(vacío)* | Servidor de IA; vacío = IA desactivada |

Opción recomendada: crear `backend/src/main/resources/application-local.properties` (está en `.gitignore`, no se sube):

```properties
spring.datasource.password=atelier
spring.security.user.password=atelier-dev
```

Y arrancar:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Si prefieres no crear el archivo, usa variables de entorno:

```powershell
$env:POSTGRES_PASSWORD="atelier"
$env:API_PASSWORD="atelier-dev"
cd backend
.\mvnw.cmd spring-boot:run
```

### Verificar que arrancó y que Flyway validó el esquema

```powershell
curl -u atelier:atelier-dev http://localhost:8080/actuator/health
curl -u atelier:atelier-dev "http://localhost:8080/api/v1/prendas?pagina=1&tamano=5"
```

La API está bajo `/api/v1` y **todos** los endpoints piden HTTP Basic, salvo `/actuator/health`, `/actuator/info` e `/imagenes/**`.

### Ver las tablas y el historial de Flyway

```powershell
$env:PGPASSWORD="atelier"
& "C:\Program Files\PostgreSQL\18\bin\psql.exe" -U atelier -d atelier -c "\dt"
& "C:\Program Files\PostgreSQL\18\bin\psql.exe" -U atelier -d atelier -c "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;"
```

### 2.1 Cargar los datos de ejemplo (opcional)

Flyway crea el esquema, pero las tablas nacen **vacías**. Para tener las mismas prendas, materiales y tendencias que el resto del equipo:

```powershell
& "C:\Program Files\PostgreSQL\18\bin\psql.exe" -U atelier -d atelier -f docs\sql\atelier-datos.sql
```

- Es **solo datos**: no crea tablas. Primero hay que arrancar el backend una vez para que Flyway aplique V1–V5.
- Solo funciona sobre una base vacía. Si ya hay prendas, se detiene con un error claro para no duplicar filas ni romper claves.
- Contiene: 3 prendas, 2 materiales, 1 tendencia, 1 propuesta de diseño, 1 análisis de viabilidad y 1 fila de ficha técnica.
- No incluye usuarios ni contraseñas.

> **¿Por qué no se sube el directorio de datos de PostgreSQL?**
> Porque `pgdata` es binario, depende de la versión instalada y de rutas absolutas. Lo que se comparte es el **esquema** (migraciones Flyway) más este **respaldo de datos** en SQL, que cualquiera puede cargar en su propia instalación.

---

## 3. Configurar la web (Django) — **sin SQLite**

Esto le corresponde a la encargada del frontend. En `web/`, los settings **no deben contener** `'ENGINE': 'django.db.backends.sqlite3'`.

Reemplazar el bloque `DATABASES` por:

```python
import os

DATABASES = {
    "default": {
        "ENGINE": "django.db.backends.postgresql",
        "NAME": os.environ.get("DJANGO_DB_NAME", "atelier_web"),
        "USER": os.environ.get("DJANGO_DB_USER", "atelier"),
        "PASSWORD": os.environ.get("DJANGO_DB_PASSWORD", "atelier"),
        "HOST": os.environ.get("DJANGO_DB_HOST", "localhost"),
        "PORT": os.environ.get("DJANGO_DB_PORT", "5432"),
    }
}
```

Instalar el driver (Django 4.2+ usa psycopg 3):

```bash
pip install "psycopg[binary]"
```

Migraciones de Django (solo sus tablas internas):

```bash
python manage.py migrate
```

### Los datos de negocio se piden a la API

En Django **no** se define un modelo `Producto`/`Prenda` propio para el catálogo. Se consume el backend:

```python
import os
import requests

API = os.environ.get("API_BASE_URL", "http://localhost:8080/api/v1")
AUTH = (os.environ.get("API_USER", "atelier"), os.environ.get("API_PASSWORD", "atelier-dev"))

def listar_prendas(pagina=0, tamano=10, q=None):
    params = {"pagina": pagina, "tamano": tamano}
    if q:
        params["q"] = q
    r = requests.get(f"{API}/prendas", params=params, auth=AUTH, timeout=5)
    r.raise_for_status()
    return r.json()          # {items, pagina, tamano, total, totalPaginas, ...}

def prenda_por_slug(slug):
    r = requests.get(f"{API}/prendas/slug/{slug}", auth=AUTH, timeout=5)
    r.raise_for_status()
    return r.json()
```

La API ya devuelve paginación, filtros (`q`, `estado`, `tipo`, `estilo`) y `slug`, justamente para que Django no necesite base de negocio propia.

---

## 4. Si algo falla

| Error | Causa | Solución |
|---|---|---|
| `password authentication failed for user "atelier"` | Contraseña distinta | Reejecutar el script (restablece la contraseña) o ajustar `POSTGRES_PASSWORD` |
| `Connection refused` | PostgreSQL apagado | Revisar el servicio `postgresql-x64-18` |
| `Schema-validation: missing column/table` | Base sin migraciones o entidad desalineada | Arrancar el backend (Flyway aplica V1..V5) y no cambiar entidades sin migración |
| `ModuleNotFoundError: No module named 'psycopg'` | Falta driver en Django | `pip install "psycopg[binary]"` |
| Sigue apareciendo `db.sqlite3` | Settings sin cambiar | Corregir `DATABASES` y no ejecutar `migrate` hasta hacerlo |

El `.gitignore` bloquea `web/db.sqlite3`, pero eso solo evita subirlo: la idea es **no generarlo**.
