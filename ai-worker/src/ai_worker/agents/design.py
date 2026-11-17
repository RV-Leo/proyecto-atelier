"""Mock design prompt builder and variant generator."""

from ai_worker.agents.base import AgentInput, BaseAgent
from ai_worker.schemas.common import AgentType
from ai_worker.schemas.design import DesignInput, DesignOutput, DesignVariant


class DesignAgent(BaseAgent):
    """Build a garment prompt and return mock image variants."""

    agent_type = AgentType.DESIGN

    async def execute(self, payload: AgentInput) -> DesignOutput:
        """Return deterministic mock variants for a design request."""
        self.require_payload(payload, DesignInput)
        prompt = self.build_prompt(payload)
        if payload.operation == "edit":
            variant_id = payload.variant_id or "variant-1"
            return DesignOutput(
                prompt=prompt,
                variants=[
                    DesignVariant(
                        variant_id=variant_id,
                        image_url=f"mock://design/{variant_id}-edited",
                        description=payload.edit_instruction or "",
                    )
                ],
            )

        variants = [
            DesignVariant(
                variant_id=f"variant-{index + 1}",
                image_url=f"mock://design/variant-{index + 1}",
                description=f"Propuesta simulada {index + 1} para {payload.garment_type}",
            )
            for index in range(payload.variants)
        ]
        return DesignOutput(prompt=prompt, variants=variants)

    @staticmethod
    def build_prompt(payload: DesignInput) -> str:
        """Compose a structured Spanish prompt from the garment specification."""
        parts = [
            f"Prenda: {payload.garment_type}",
            f"Género: {payload.gender}",
            f"Temporada: {payload.season}",
            f"Estilo: {payload.style}",
            f"Paleta: {', '.join(payload.palette)}",
            f"Tela: {payload.fabric}",
        ]
        if payload.description:
            parts.append(f"Detalles: {payload.description}")
        if payload.operation == "edit":
            parts.append(f"Editar variante: {payload.edit_instruction}")
        return ". ".join(parts)
