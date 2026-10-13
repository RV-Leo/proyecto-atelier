# backend — API REST

API REST del proyecto Atelier construida con Spring Boot. Es el punto único de
acceso a los datos: la web (Django) y la app móvil consumen esta API en lugar de
conectarse directamente a la base de datos.

## Requisitos

- Java 21
- Maven 3.9+ (o usar el wrapper: `./mvnw`)
- PostgreSQL 14+ corriendo en `localhost:5432`

## Configuración de la base de datos

Crear el rol y la base de datos una sola vez:

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
| `DB_URL` | JDBC URL | `jdbc:postgresql://localhost:5432/atelier` |
| `DB_USER` | Usuario de PostgreSQL | `atelier` |
| `DB_PASSWORD` | Contraseña | `secret` |

## Ejecutar

```bash
# Perfil local (lee application-local.properties)
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# O con variables de entorno
export DB_URL=jdbc:postgresql://localhost:5432/atelier
export DB_USER=atelier
export DB_PASSWORD=secret
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
`/actuator`.

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
| GET | `/materiales` | Lista todos los materiales |
| GET | `/materiales/activos` | Lista solo los activos |
| GET | `/materiales/{id}` | Detalle de un material |
| POST | `/materiales` | Crea un material |
| PUT | `/materiales/{id}` | Actualiza un material |
| DELETE | `/materiales/{id}` | Elimina un material |

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
| GET | `/prendas` | Lista las prendas |
| GET | `/prendas/{id}` | Detalle de una prenda |
| POST | `/prendas` | Crea una prenda (estado inicial `BORRADOR`) |
| GET | `/prendas/{id}/materiales` | Ficha técnica con stock disponible |
| POST | `/prendas/{id}/materiales` | Asigna un material a la prenda |

`estado` acepta: `BORRADOR`, `GENERANDO`, `GENERADA`, `ANALIZADA`.

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
| GET | `/tendencias` | Lista las tendencias |
| GET | `/tendencias/region/{region}` | Filtra por región |
| POST | `/tendencias` | Registra una tendencia capturada |

`region` acepta: `EUROPA`, `AMERICA`, `GLOBAL`.
`fuente` acepta: `PINTEREST`, `TIENDA_MARCA`, `OTRO`.

`urlOrigem` es única: si la URL ya fue registrada la API responde `409 Conflict`
en lugar de duplicar el registro.

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
