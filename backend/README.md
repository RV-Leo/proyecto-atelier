# Backend de Atelier

API REST de Spring Boot para catálogo, inventario, diseño y viabilidad. Spring
Boot es la fuente de verdad del inventario y calcula la viabilidad; la IA solo
ayuda a redactar y generar contenido.

## Requisitos

- Java 21
- PostgreSQL

## Ejecutar en Windows

Configura las variables de entorno si tus valores difieren de los valores
locales y luego ejecuta:

```powershell
.\mvnw.cmd spring-boot:run
```

Las migraciones de la base de datos se aplican con Flyway.

## Configuración

| Variable | Uso |
|---|---|
| `POSTGRES_URL`, `POSTGRES_USER`, `POSTGRES_PASSWORD` | Conexión a PostgreSQL |
| `API_USER`, `API_PASSWORD` | Autenticación HTTP Basic de la API |
| `IA_URL` | URL base de un proveedor compatible con OpenAI; vacía desactiva la IA |
| `IA_API_KEY` | Clave del proveedor, solo en el servidor |
| `IA_MODELO_TEXTO` | Modelo de chat; por defecto `gemini-3.8-flash` |
| `IA_MODELO_IMAGEN` | Modelo de generación de imágenes |
| `IA_TIMEOUT_SEGUNDOS` | Tiempo máximo para solicitudes al proveedor |

Para el chat con Gemini, usa `IA_URL=https://generativelanguage.googleapis.com/v1beta/openai/`,
`IA_API_KEY` y el modelo habilitado en tu proyecto. No guardes claves en el
código ni las compartas con web o mobile. La generación de imágenes requiere
que el proveedor configurado implemente `/images/generations`.

## Endpoints principales

Base: `/api/v1`

- `/materiales`, `/prendas`: catálogo e inventario.
- `/viabilidad/{prendaId}`: cálculo determinista de disponibilidad.
- `/tendencias`: consulta y registro de tendencias.
- `/ia/chat`: chat de texto.
- `/prendas/{prendaId}/diseno`: prompt y propuestas de diseño.

La API usa HTTP Basic. `/actuator/health` y `/actuator/info` son públicos.

## Pruebas

```powershell
.\mvnw.cmd test
```
