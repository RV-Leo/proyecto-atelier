"""Typed interface shared by the worker agents."""

from abc import ABC, abstractmethod

from pydantic import BaseModel

from ai_worker.schemas.agent_outputs import AgentOutput
from ai_worker.schemas.common import AgentType
from ai_worker.schemas.design import DesignInput
from ai_worker.schemas.feasibility import FeasibilityInput
from ai_worker.schemas.stock_alerts import StockAlertsInput
from ai_worker.schemas.trends import TrendInput

AgentInput = DesignInput | TrendInput | FeasibilityInput | StockAlertsInput


class BaseAgent(ABC):
    """Base interface for a JSON-in/JSON-out agent."""

    agent_type: AgentType

    @abstractmethod
    async def execute(self, payload: AgentInput) -> AgentOutput:
        """Validate and process one agent-specific input."""

    @staticmethod
    def require_payload(payload: AgentInput, expected_type: type[BaseModel]) -> None:
        """Reject a payload that does not match the receiving agent."""
        if not isinstance(payload, expected_type):
            raise TypeError(
                f"El agente recibió {type(payload).__name__}; "
                f"esperaba {expected_type.__name__}"
            )
