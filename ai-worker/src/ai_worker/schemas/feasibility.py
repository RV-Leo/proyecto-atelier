"""Contracts for technical sheets and production feasibility."""

from decimal import Decimal
from typing import Literal

from pydantic import Field, field_serializer

from ai_worker.schemas.common import StrictModel


class MaterialRequirement(StrictModel):
    """A material and quantity required by a garment's technical sheet."""

    material_name: str = Field(min_length=1, max_length=120)
    required_quantity: Decimal = Field(gt=0)
    unit: str = Field(min_length=1, max_length=40)
    color: str | None = Field(default=None, max_length=80)

    @field_serializer("required_quantity", when_used="json")
    def serialize_quantity(self, value: Decimal) -> float:
        """Keep decimal quantities numeric in JSON responses."""
        return float(value)


class FeasibilityInput(StrictModel):
    """Prenda details that the feasibility agent compares with inventory."""

    agent: Literal["feasibility"] = "feasibility"
    garment_name: str = Field(min_length=1, max_length=120)
    desired_color: str | None = Field(default=None, max_length=80)
    materials: list[MaterialRequirement] = Field(min_length=1, max_length=30)


class MaterialAlternative(StrictModel):
    """A deterministic substitute found in the mock inventory."""

    material_id: int
    material_name: str
    color: str
    available_quantity: Decimal
    unit: str
    kind: Literal["tela", "color"]

    @field_serializer("available_quantity", when_used="json")
    def serialize_quantity(self, value: Decimal) -> float:
        """Keep decimal quantities numeric in JSON responses."""
        return float(value)


class MaterialCheck(StrictModel):
    """Inventory comparison for one material requirement."""

    material_name: str
    required_quantity: Decimal
    available_quantity: Decimal
    unit: str
    sufficient: bool
    alternatives: list[MaterialAlternative]

    @field_serializer("required_quantity", "available_quantity", when_used="json")
    def serialize_quantity(self, value: Decimal) -> float:
        """Keep decimal quantities numeric in JSON responses."""
        return float(value)


class FeasibilityOutput(StrictModel):
    """Production verdict based on deterministic stock comparisons."""

    agent: Literal["feasibility"] = "feasibility"
    result: Literal["se_puede", "con_ajustes", "no_se_puede"]
    score: int = Field(ge=0, le=100)
    explanation: str
    details: list[MaterialCheck]
