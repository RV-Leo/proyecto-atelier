# backend — API REST y puente de IA

API REST del proyecto Atelier construida con Spring Boot. Es el punto único de
acceso a los datos: la web (Django) y la app móvil consumen esta API en lugar de
conectarse directamente a la base de datos.

Además, hace de **puente de IA**: habla con el servidor de modelos (Qwen) para
redactar el comentario de viabilidad y generar las propuestas de diseño. Ver
[Puente de IA](#puente-de-ia).

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
