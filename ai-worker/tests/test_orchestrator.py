"""Focused tests for agent dispatch and deterministic feasibility."""

from decimal import Decimal

import pytest
from pydantic import ValidationError

from ai_worker.agents.design import DesignAgent
from ai_worker.agents.feasibility import FeasibilityAgent
from ai_worker.orchestrator import Orchestrator
from ai_worker.schemas.design import DesignInput, DesignOutput
from ai_worker.schemas.feasibility import FeasibilityInput, MaterialRequirement
from ai_worker.schemas.tasks import DesignTaskRequest, FeasibilityTaskRequest
from ai_worker.services.audit import AuditEntry, JsonAuditLogger
from ai_worker.services.inventory_client import MockInventoryClient


class RecordingAuditLogger(JsonAuditLogger):
    """Capture audit payloads for assertions."""

    def __init__(self) -> None:
        self.entries: list[AuditEntry] = []

    def record(self, entry: AuditEntry) -> None:
        self.entries.append(entry)


@pytest.mark.asyncio
async def test_orchestrator_dispatches_design_and_audits_json() -> None:
    audit = RecordingAuditLogger()
    orchestrator = Orchestrator([DesignAgent()], audit)
    request = DesignTaskRequest(
        agent="design",
        payload=DesignInput(
            garment_type="chaqueta",
            gender="unisex",
            season="invierno",
            style="urbano",
            palette=["negro", "gris"],
            fabric="denim",
            description="cierres metálicos",
            variants=3,
        ),
    )

    result = await orchestrator.execute(request)

    assert isinstance(result, DesignOutput)
    assert len(result.variants) == 3
    assert "cierres metálicos" in result.prompt
    assert audit.entries[0].agent.value == "design"
    assert audit.entries[0].output["agent"] == "design"


def test_design_contract_rejects_more_than_four_variants() -> None:
    with pytest.raises(ValidationError):
        DesignInput(
            garment_type="chaqueta",
            gender="unisex",
            season="invierno",
            style="urbano",
            palette=["negro"],
            fabric="denim",
            variants=5,
        )


@pytest.mark.asyncio
async def test_feasibility_comparison_is_deterministic_and_suggests_alternatives() -> None:
    orchestrator = Orchestrator([FeasibilityAgent(MockInventoryClient())])
    request = FeasibilityTaskRequest(
        agent="feasibility",
        payload=FeasibilityInput(
            garment_name="camisa",
            desired_color="negro",
            materials=[
                MaterialRequirement(
                    material_name="algodón",
                    required_quantity=Decimal("10"),
                    unit="METRO",
                )
            ],
        ),
    )

    result = await orchestrator.execute(request)

    assert result.result == "con_ajustes"
    assert result.details[0].available_quantity == Decimal("8")
    assert result.details[0].sufficient is False
    assert any(option.kind == "color" for option in result.details[0].alternatives)
