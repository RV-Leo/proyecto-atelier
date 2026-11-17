"""Dispatch task requests to the matching agent and audit execution."""

import logging

from ai_worker.agents.base import BaseAgent
from ai_worker.schemas.common import AgentType
from ai_worker.schemas.tasks import TaskRequest
from ai_worker.schemas.agent_outputs import AgentOutput
from ai_worker.services.audit import AuditEntry, JsonAuditLogger

logger = logging.getLogger(__name__)


class Orchestrator:
    """Select an agent, validate its result, and record the execution."""

    def __init__(
        self,
        agents: list[BaseAgent],
        audit_logger: JsonAuditLogger | None = None,
    ) -> None:
        registry: dict[AgentType, BaseAgent] = {}
        for agent in agents:
            if agent.agent_type in registry:
                raise ValueError(f"Agente duplicado: {agent.agent_type.value}")
            registry[agent.agent_type] = agent
        self._agents = registry
        self._audit = audit_logger or JsonAuditLogger()

    async def execute(self, request: TaskRequest) -> AgentOutput:
        """Execute one validated task request with an audited result."""
        agent_type = AgentType(request.agent)
        agent = self._agents.get(agent_type)
        if agent is None:
            raise ValueError(f"No hay agente registrado para {agent_type.value}")

        input_data = request.payload.model_dump(mode="json")
        try:
            output = await agent.execute(request.payload)
            if output.agent != agent_type.value:
                raise ValueError(
                    f"El agente {agent_type.value} devolvió {output.agent}"
                )
        except Exception as exc:
            logger.exception("Agent %s failed", agent_type.value)
            self._audit.record(
                AuditEntry(
                    agent=agent_type,
                    input=input_data,
                    error=f"{type(exc).__name__}: {exc}",
                )
            )
            raise

        self._audit.record(
            AuditEntry(
                agent=agent_type,
                input=input_data,
                output=output.model_dump(mode="json"),
            )
        )
        return output
