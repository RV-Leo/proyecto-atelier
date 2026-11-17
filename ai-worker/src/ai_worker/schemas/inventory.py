"""Read-only inventory contract used by feasibility and stock alerts."""

from decimal import Decimal

from pydantic import Field, field_serializer

from ai_worker.schemas.common import StrictModel


class InventoryMaterial(StrictModel):
    """Inventory material projection returned by the Spring Boot API."""

    id: int
    name: str = Field(min_length=1)
    color: str = ""
    unit: str = Field(min_length=1)
    stock: Decimal = Field(ge=0)
    minimum_stock: Decimal = Field(ge=0)

    @field_serializer("stock", "minimum_stock", when_used="json")
    def serialize_quantity(self, value: Decimal) -> float:
        """Keep decimal quantities numeric in JSON responses."""
        return float(value)
