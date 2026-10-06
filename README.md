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

PostgreSQL es una propuesta para la base de datos; todavía no hay un motor configurado en el repositorio.

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