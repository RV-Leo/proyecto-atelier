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
                                  Base de datos PostgreSQL (atelier)
```

PostgreSQL es la base de datos del backend. La web y el móvil **no se conectan a ella**: consumen la API REST por HTTP.

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

## Web: catálogo e inventario desde la API

La web ya no lee prendas ni materiales de su propia base. `web/catalogo/views.py` usa `web/catalogo/api_cliente.py`, que llama a la API del backend con HTTP Basic. Django conserva SQLite **solo para sus tablas internas** (`auth_user`, sesiones, admin).

1. Levanta el backend en `http://localhost:8080` (ver su guía).
2. Copia `web/.env.example` a `web/.env` y ajusta:

```env
API_BASE_URL=http://localhost:8080/api/v1
API_USER=atelier
API_PASSWORD=atelier-dev
API_TIMEOUT=5
```

3. Instala dependencias y prepara las tablas internas:

```bash
pip install -r web/requirements.txt
python manage.py migrate
python manage.py runserver
```

Qué consume la web:

| Vista | Endpoint |
|---|---|
| Portada (`/`) | `GET /prendas?pagina=1&tamano=12` |
| Catálogo (`/catalogo/`) | `GET /prendas?pagina=&tamano=12&q=&tipo=&estilo=` |
| Detalle (`/catalogo/producto/<slug>/`) | `GET /prendas/slug/<slug>` |
| Inventario (`/catalogo/inventario/`) | `GET /materiales?q=&estado=` |
| Informes (admin) | `GET /prendas` y `GET /materiales` |

Notas:

- `pagina` es **base 1**; `tamano` por defecto 20 y máximo 100.
- Django llama a la API desde el servidor, así que **no hace falta CORS**.
- Si la API no responde, las vistas muestran un aviso y no un error 500.
- Los campos que la API todavía no expone (precio, stock, imagen, favoritos) no se inventan: se ocultan en la interfaz.
- Pruebas sin backend ni PostgreSQL: `python manage.py test catalogo core`.

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