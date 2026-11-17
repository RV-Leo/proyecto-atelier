"""Contracts for low-stock alerts."""

from decimal import Decimal
from typing import Literal

from pydantic import Field, field_serializer

from ai_worker.schemas.common import StrictModel


class StockAlertsInput(StrictModel):
    """Request a scan for materials at or below their reorder threshold."""

    agent: Literal["stock_alerts"] = "stock_alerts"


class StockAlert(StrictModel):
    """One low-stock inventory alert."""

    material_id: int
    material_name: str
    color: str
    stock: Decimal
    minimum_stock: Decimal
    unit: str

    @field_serializer("stock", "minimum_stock", when_used="json")
    def serialize_quantity(self, value: Decimal) -> float:
        """Keep decimal quantities numeric in JSON responses."""
        return float(value)


class StockAlertsOutput(StrictModel):
    """Alerts produced by the stock monitor."""

    agent: Literal["stock_alerts"] = "stock_alerts"
    alerts: list[StockAlert]
