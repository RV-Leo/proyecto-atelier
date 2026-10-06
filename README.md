<div align="center">

# 🧵 Atelier

### Del hilo a la idea. De la idea a la prenda.

*Diseña con IA, descubre tendencias y sabe si tu prenda se puede producir, todo en un solo lugar.*

![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Django](https://img.shields.io/badge/Django-092E20?style=for-the-badge&logo=django&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![IA](https://img.shields.io/badge/IA-Generativa-FF6F61?style=for-the-badge)

</div>

---

## ✨ ¿Qué es Atelier?

Atelier es un sistema para el **rubro textil** que une tres inteligencias artificiales en un solo flujo de trabajo:

| | Módulo | ¿Qué hace? |
|---|---|---|
| 🎨 | **Diseño generativo** | Crea imágenes de prendas a partir de una descripción: tipo, estilo, colores y tela. |
| 📈 | **Tendencias** | Recopila lo que se lleva en Europa y América desde Pinterest y tiendas de marca. |
| 🧪 | **Viabilidad e inventario** | Analiza si la prenda se puede producir con el stock disponible y propone alternativas. |

## 🧭 ¿Cómo funciona?

```
 📱 App móvil ─┐
               ├──▶ ⚙️ Spring Boot ──▶ 🌐 Django (gateway IA) ──▶ 🤖 Modelos en servidor
 💻 Web ───────┘          │
                          ▼
                    🗄️ Base de datos
```

## 📁 Estructura del proyecto

```
atelier/
├── 📂 backend/   → API REST, seguridad y base de datos (Spring Boot)
├── 📂 web/       → Aplicación web y conexión con la IA (Django)
├── 📂 mobile/    → App Android (Kotlin)
└── 📂 docs/      → Contrato de la API, diagramas e informes
```

## 🚀 Empezar

1. Clona el repositorio:
```bash
   git clone https://github.com/RV-Leo/proyecto-atelier.git
```
2. Entra a la carpeta del módulo que vas a trabajar y sigue su guía.
3. Crea tu archivo `.env` a partir de `.env.example` *(nunca lo subas a GitHub)*.

## 🤝 Cómo colaboramos

- 🌿 `main` es la rama estable; el trabajo diario va en `develop`.
- 🔀 Cada tarea tiene su rama: `feature/web-...`, `feature/mobile-...`, `feature/backend-...`.
- 💬 Commits con prefijo: `web:`, `mobile:`, `backend:`, `docs:`.
- 🔒 Nada de claves ni contraseñas en el código.

## 👥 Equipo

| Integrante | Rol |
|---|---|
| *Nombre* | *Rol* |

<div align="center">

Hecho con 🧵 y mucho ☕ en **Tecsup**

</div>