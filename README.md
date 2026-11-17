<div align="center">

<img src="docs/atelier-banner.svg" alt="Atelier" width="100%">

### Del hilo a la idea. De la idea a la prenda.

Diseña prendas, explora tendencias y comprueba si puedes producirlas.

</div>

## Módulos

| Módulo | Función |
|---|---|
| `backend/` | API REST, inventario, viabilidad y puente de IA (Spring Boot) |
| `web/` | Aplicación web (Django) |
| `mobile/` | Aplicación Android (Kotlin) |
| `ai-worker/` | Prototipo de agentes asíncronos; por ahora usa datos simulados |

La web y la app móvil consultan el backend. Spring Boot es la fuente de verdad
para el inventario y los cálculos de viabilidad. Las credenciales de IA se
configuran únicamente en el servidor; nunca las incluyas en los clientes.

## Empezar

Clona el repositorio y sigue la guía del módulo que vayas a ejecutar:

```bash
git clone https://github.com/RV-Leo/proyecto-atelier.git
```

- [Backend: configuración y pruebas](backend/README.md)
- [Worker de IA: instalación y ejecución](ai-worker/README.md)

Para desarrollo local, crea los archivos de entorno a partir de sus ejemplos.
No subas claves ni contraseñas a Git.

## IA

El backend permite configurar un proveedor de texto compatible con OpenAI y
mantiene la clave del lado del servidor. El worker Python aún no está conectado
a modelos reales ni a la API de inventario.
