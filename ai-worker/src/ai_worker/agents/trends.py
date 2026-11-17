"""Mock vision tagger and trend classifier."""

from ai_worker.agents.base import AgentInput, BaseAgent
from ai_worker.schemas.common import AgentType
from ai_worker.schemas.trends import TrendInput, TrendLevel, TrendOutput


class TrendsAgent(BaseAgent):
    """Return sample visual labels until the local vision model is connected."""

    agent_type = AgentType.TRENDS

    async def execute(self, payload: AgentInput) -> TrendOutput:
        """Return a schema-validated mock trend classification."""
        self.require_payload(payload, TrendInput)
        return TrendOutput(
            colors=["azul marino", "crudo"],
            cut="silueta recta",
            fabric="algodón",
            trend_level=TrendLevel.MEDIUM,
            confidence=0.5,
        )
