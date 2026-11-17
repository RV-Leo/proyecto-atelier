"""Mock stock-threshold scanner."""

from ai_worker.agents.base import AgentInput, BaseAgent
from ai_worker.services.inventory_client import InventoryReader
from ai_worker.schemas.common import AgentType
from ai_worker.schemas.inventory import InventoryMaterial
from ai_worker.schemas.stock_alerts import (
    StockAlert,
    StockAlertsInput,
    StockAlertsOutput,
)


class StockAlertsAgent(BaseAgent):
    """Report inventory rows at or below their configured minimum."""

    agent_type = AgentType.STOCK_ALERTS

    def __init__(self, inventory: InventoryReader) -> None:
        self._inventory = inventory

    async def execute(self, payload: AgentInput) -> StockAlertsOutput:
        """Return mock stock alerts using a deterministic threshold check."""
        self.require_payload(payload, StockAlertsInput)
        materials: list[InventoryMaterial] = await self._inventory.list_materials()
        return StockAlertsOutput(
            alerts=[
                StockAlert(
                    material_id=item.id,
                    material_name=item.name,
                    color=item.color,
                    stock=item.stock,
                    minimum_stock=item.minimum_stock,
                    unit=item.unit,
                )
                for item in materials
                if item.stock <= item.minimum_stock
            ]
        )
