"""Structured JSON audit logging for agent executions."""

import json
import logging
from typing import Any

from ai_worker.schemas.common import AgentType, StrictModel


class AuditEntry(StrictModel):
    """Input, output, or error recorded for one agent execution."""

    agent: AgentType
    input: dict[str, Any]
    output: dict[str, Any] | None = None
    error: str | None = None


class JsonAuditLogger:
    """Write one structured audit record to the application log."""

    def __init__(self, logger: logging.Logger | None = None) -> None:
        self._logger = logger or logging.getLogger("ai_worker.audit")

    def record(self, entry: AuditEntry) -> None:
        """Emit an audit entry as JSON."""
        self._logger.info(
            "agent_audit %s",
            json.dumps(entry.model_dump(mode="json"), ensure_ascii=False),
        )
