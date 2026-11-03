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
| **Diseño generativo y asesoría IA** | Crear propuestas de prendas y conversar sobre outfits, telas, calidad y combinaciones. |
| **Tendencias** | Consultar tendencias de Europa y América a partir de Pinterest y tiendas de marca. |
| **Viabilidad e inventario** | Revisar si hay materiales para producir una prenda y buscar alternativas cuando falte stock. |

## ¿Cómo funciona?

```
App móvil (Kotlin / Android) ──┐
                               ├──> API REST (Spring Boot + puente IA) ──> Gemini temporal / Qwen
Aplicación web (Django) ───────┘                   │
                                                  v
                                    Base de datos PostgreSQL (requiere una instancia)
```

El backend ya está configurado para PostgreSQL y Flyway; se necesita una instancia local o remota para ejecutarlo.
La web y la app móvil usan el mismo endpoint de chat del backend; la clave del proveedor solo se configura en el servidor.

## Modelos de IA

Temporalmente, el puente usa **Gemini** para generar texto. La integración está aislada en el backend para poder cambiar a **Qwen** sin modificar la aplicación web ni la móvil. Gemini envía las consultas a un servicio externo; no se debe incluir su clave en un cliente.

| Módulo | Modelo | Uso |
|---|---|---|
| Diseño generativo | Qwen-Image / Qwen-Image-Edit | Crear y editar imágenes de prendas |
| Tendencias | Qwen3.6-35B-A3B | Identificar colores, cortes y telas en imágenes |
| Viabilidad e inventario | Qwen3.6-35B-A3B | Comparar una idea con el stock y sugerir alternativas |

En [`docs/modelos-ia.md`](docs/modelos-ia.md) contamos el uso temporal de Gemini, la transición planeada a Qwen y el papel de cada modelo.

### Configurar el puente

En el entorno del backend, configura `IA_URL`, `IA_API_KEY` y `IA_MODELO_TEXTO`. Para Gemini, usa la URL compatible con OpenAI de Google y el modelo `gemini-3.8-flash`. Para pasar a Qwen, apunta `IA_URL` a su servidor compatible y cambia `IA_MODELO_TEXTO`; las aplicaciones cliente conservan el mismo contrato `POST /api/v1/ia/chat`.

La aplicación web se conecta al backend con `ATELIER_BACKEND_URL`, `ATELIER_BACKEND_USER` y `ATELIER_BACKEND_PASSWORD`. Para Android local, configura `atelier.api.baseUrl`, `atelier.api.user` y `atelier.api.password` en `mobile/local.properties` (ese archivo no se versiona). Usa las credenciales de API definidas en el entorno del backend (`API_USER` y `API_PASSWORD`). En el emulador, la URL por defecto apunta a `http://10.0.2.2:8080/`.

El HTTP sin cifrar solo está habilitado en el manifiesto de depuración de Android. Usa HTTPS y autenticación de usuario adecuada antes de desplegar; no incluyas credenciales compartidas del backend en una versión distribuida de la app.

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