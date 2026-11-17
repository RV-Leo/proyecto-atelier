"""JSON contracts for task creation and status polling."""

from datetime import datetime
from typing import Annotated, Literal, Union
from uuid import UUID

from pydantic import Field

from ai_worker.schemas.agent_outputs import AgentOutput
from ai_worker.schemas.common import AgentType, StrictModel, TaskStatus
from ai_worker.schemas.design import DesignInput
from ai_worker.schemas.feasibility import FeasibilityInput
from ai_worker.schemas.stock_alerts import StockAlertsInput
from ai_worker.schemas.trends import TrendInput


class DesignTaskRequest(StrictModel):
    """Request a garment design task."""

    agent: Literal["design"]
    payload: DesignInput


class TrendTaskRequest(StrictModel):
    """Request trend image analysis."""

    agent: Literal["trends"]
    payload: TrendInput


class FeasibilityTaskRequest(StrictModel):
    """Request a deterministic production feasibility comparison."""

    agent: Literal["feasibility"]
    payload: FeasibilityInput


class StockAlertsTaskRequest(StrictModel):
    """Request a low-stock scan."""

    agent: Literal["stock_alerts"]
    payload: StockAlertsInput


TaskRequest = Annotated[
    Union[
        DesignTaskRequest,
        TrendTaskRequest,
        FeasibilityTaskRequest,
        StockAlertsTaskRequest,
    ],
    Field(discriminator="agent"),
]


class TaskStatusResponse(StrictModel):
    """Task state and optional validated agent result."""

    task_id: UUID
    agent: AgentType
    status: TaskStatus
    result: AgentOutput | None = None
    error: str | None = None
    created_at: datetime
    updated_at: datetime
