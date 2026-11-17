"""Deterministic stock comparison with a mock read-only inventory client."""

from decimal import Decimal
from unicodedata import normalize

from ai_worker.agents.base import AgentInput, BaseAgent
from ai_worker.schemas.common import AgentType
from ai_worker.schemas.feasibility import (
    FeasibilityInput,
    FeasibilityOutput,
    MaterialAlternative,
    MaterialCheck,
)
from ai_worker.schemas.inventory import InventoryMaterial
from ai_worker.services.inventory_client import InventoryReader


class FeasibilityAgent(BaseAgent):
    """Compare technical-sheet quantities with inventory in ordinary code."""

    agent_type = AgentType.FEASIBILITY

    def __init__(self, inventory: InventoryReader) -> None:
        self._inventory = inventory

    async def execute(self, payload: AgentInput) -> FeasibilityOutput:
        """Evaluate requirements without asking an LLM to decide stock."""
        self.require_payload(payload, FeasibilityInput)
        materials = await self._inventory.list_materials()
        checks: list[MaterialCheck] = []

        for requirement in payload.materials:
            requested_color = requirement.color or payload.desired_color
            exact = [
                item
                for item in materials
                if self._same(item.name, requirement.material_name)
                and self._same(item.unit, requirement.unit)
                and (
                    requested_color is None
                    or self._same(item.color, requested_color)
                )
            ]
            available = sum((item.stock for item in exact), Decimal("0"))
            sufficient = available >= requirement.required_quantity
            alternatives = (
                []
                if sufficient
                else self._find_alternatives(
                    materials, requirement.material_name, requirement.unit,
                    requested_color, requirement.required_quantity
                )
            )
            checks.append(
                MaterialCheck(
                    material_name=requirement.material_name,
                    required_quantity=requirement.required_quantity,
                    available_quantity=available,
                    unit=requirement.unit,
                    sufficient=sufficient,
                    alternatives=alternatives,
                )
            )

        sufficient_count = sum(check.sufficient for check in checks)
        score = round(100 * sufficient_count / len(checks))
        if sufficient_count == len(checks):
            result = "se_puede"
            explanation = "El inventario simulado cubre todos los materiales."
        elif all(check.sufficient or check.alternatives for check in checks):
            result = "con_ajustes"
            explanation = "Se requieren los ajustes de material o color sugeridos."
        else:
            result = "no_se_puede"
            explanation = "Falta material y no se encontró una alternativa suficiente."
        return FeasibilityOutput(
            result=result,
            score=score,
            explanation=explanation,
            details=checks,
        )

    @classmethod
    def _find_alternatives(
        cls,
        materials: list[InventoryMaterial],
        material_name: str,
        unit: str,
        color: str | None,
        required_quantity: Decimal,
    ) -> list[MaterialAlternative]:
        """Suggest in-stock alternatives with matching units and quantity."""
        options = [
            item
            for item in materials
            if cls._same(item.unit, unit)
            and item.stock >= required_quantity
            and not (
                cls._same(item.name, material_name)
                and (color is None or cls._same(item.color, color))
            )
        ]
        options.sort(key=lambda item: (item.name.casefold(), item.color.casefold(), item.id))
        return [
            MaterialAlternative(
                material_id=item.id,
                material_name=item.name,
                color=item.color,
                available_quantity=item.stock,
                unit=item.unit,
                kind=(
                    "color"
                    if cls._same(item.name, material_name)
                    else "tela"
                ),
            )
            for item in options[:3]
        ]

    @staticmethod
    def _same(left: str, right: str) -> bool:
        """Compare labels without accents or case differences."""
        left_normalized = normalize("NFKD", left).encode("ascii", "ignore").decode()
        right_normalized = normalize("NFKD", right).encode("ascii", "ignore").decode()
        return left_normalized.strip().casefold() == right_normalized.strip().casefold()
