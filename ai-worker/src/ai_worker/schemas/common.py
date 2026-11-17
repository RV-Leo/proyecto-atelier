"""Shared Pydantic types for the worker contracts."""

from enum import StrEnum

from pydantic import BaseModel, ConfigDict


class StrictModel(BaseModel):
    """Base contract that rejects undeclared JSON fields."""

    model_config = ConfigDict(extra="forbid")


class AgentType(StrEnum):
    """Supported task kinds."""

    DESIGN = "design"
    TRENDS = "trends"
    FEASIBILITY = "feasibility"
    STOCK_ALERTS = "stock_alerts"


class TaskStatus(StrEnum):
    """Lifecycle state of an asynchronous task."""

    LOADING = "cargando"
    READY = "listo"
    ERROR = "error"
