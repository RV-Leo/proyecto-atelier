"""Mock read-only Spring Boot inventory client."""

from decimal import Decimal
from typing import Protocol

from ai_worker.schemas.inventory import InventoryMaterial


class InventoryReader(Protocol):
    """Read-only inventory interface implemented by the Spring Boot client."""

    async def list_materials(self) -> list[InventoryMaterial]:
        """Return material records without modifying the source of truth."""
        ...


class MockInventoryClient:
    """Provide deterministic local inventory fixtures without a database."""

    async def list_materials(self) -> list[InventoryMaterial]:
        """Return sample materials; replace with GET /api/v1/materiales later."""
        return [
            InventoryMaterial(
                id=1,
                name="algodón",
                color="blanco",
                unit="METRO",
                stock=Decimal("120"),
                minimum_stock=Decimal("15"),
            ),
            InventoryMaterial(
                id=2,
                name="algodón",
                color="negro",
                unit="METRO",
                stock=Decimal("8"),
                minimum_stock=Decimal("12"),
            ),
            InventoryMaterial(
                id=3,
                name="lino",
                color="natural",
                unit="METRO",
                stock=Decimal("30"),
                minimum_stock=Decimal("5"),
            ),
            InventoryMaterial(
                id=4,
                name="denim",
                color="azul",
                unit="METRO",
                stock=Decimal("3"),
                minimum_stock=Decimal("10"),
            ),
        ]
