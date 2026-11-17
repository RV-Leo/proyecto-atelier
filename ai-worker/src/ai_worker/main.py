"""FastAPI application factory and worker lifecycle."""

import logging
from contextlib import asynccontextmanager
from collections.abc import AsyncIterator

from fastapi import FastAPI

from ai_worker.agents.design import DesignAgent
from ai_worker.agents.feasibility import FeasibilityAgent
from ai_worker.agents.stock_alerts import StockAlertsAgent
from ai_worker.agents.trends import TrendsAgent
from ai_worker.api.routes.tasks import router as tasks_router
from ai_worker.config import Settings, get_settings
from ai_worker.orchestrator import Orchestrator
from ai_worker.services.inventory_client import MockInventoryClient
from ai_worker.task_queue import TaskQueue


def create_app(settings: Settings | None = None) -> FastAPI:
    """Create the API and wire mock agents to one task queue."""
    runtime_settings = settings or get_settings()
    logging.basicConfig(level=runtime_settings.log_level.upper())
    inventory = MockInventoryClient()
    orchestrator = Orchestrator(
        agents=[
            DesignAgent(),
            TrendsAgent(),
            FeasibilityAgent(inventory),
            StockAlertsAgent(inventory),
        ]
    )
    queue = TaskQueue(orchestrator)

    @asynccontextmanager
    async def lifespan(_: FastAPI) -> AsyncIterator[None]:
        await queue.start()
        try:
            yield
        finally:
            await queue.stop()

    app = FastAPI(title="Atelier AI Worker", version="0.1.0", lifespan=lifespan)
    app.state.task_queue = queue
    app.include_router(tasks_router)
    return app


app = create_app()
