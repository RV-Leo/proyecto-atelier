# Worker de IA

Servicio FastAPI con una cola en memoria de un solo consumidor. Los agentes
validan sus entradas y salidas con Pydantic. En esta versión, las respuestas
son simuladas: todavía no se conecta a Gemini ni al inventario de Spring Boot,
y no usa una base de datos.

## Instalar y ejecutar

Desde `ai-worker`, con Python 3.11 o superior:

```powershell
py -3.11 -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install -e ".[test]"
Copy-Item .env.example .env
uvicorn ai_worker.main:app --app-dir src --reload
```

La API acepta tareas en `POST /tasks` y consulta su estado en
`GET /tasks/{task_id}` (`cargando`, `listo` o `error`). Los resultados se
pierden al reiniciar el proceso.

## Pruebas

```powershell
python -m pytest
```
