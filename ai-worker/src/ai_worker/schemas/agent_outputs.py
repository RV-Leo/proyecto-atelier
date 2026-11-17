"""Discriminated union of agent result contracts."""

from typing import Annotated, Union

from pydantic import Field

from ai_worker.schemas.design import DesignOutput
from ai_worker.schemas.feasibility import FeasibilityOutput
from ai_worker.schemas.stock_alerts import StockAlertsOutput
from ai_worker.schemas.trends import TrendOutput

AgentOutput = Annotated[
    Union[DesignOutput, TrendOutput, FeasibilityOutput, StockAlertsOutput],
    Field(discriminator="agent"),
]
