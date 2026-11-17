"""Tests for sequential task execution and error status handling."""

import asyncio

import pytest

from ai_worker.agents.design import DesignAgent
from ai_worker.agents.base import AgentInput
from ai_worker.schemas.agent_outputs import AgentOutput
from ai_worker.orchestrator import Orchestrator
from ai_worker.schemas.design import DesignInput
from ai_worker.schemas.tasks import DesignTaskRequest
from ai_worker.task_queue import TaskQueue


class SlowDesignAgent(DesignAgent):
    """Track concurrent calls while delaying each mock generation."""

    def __init__(self) -> None:
        self.active = 0
        self.maximum_active = 0

    async def execute(self, payload: AgentInput) -> AgentOutput:
        self.active += 1
        self.maximum_active = max(self.maximum_active, self.active)
        try:
            await asyncio.sleep(0.01)
            return await super().execute(payload)
        finally:
            self.active -= 1


def design_request() -> DesignTaskRequest:
    """Build a valid design task request for queue tests."""
    return DesignTaskRequest(
        agent="design",
        payload=DesignInput(
            garment_type="falda",
            gender="mujer",
            season="verano",
            style="casual",
            palette=["azul"],
            fabric="algodón",
            variants=1,
        ),
    )


@pytest.mark.asyncio
async def test_queue_executes_tasks_one_at_a_time() -> None:
    agent = SlowDesignAgent()
    queue = TaskQueue(Orchestrator([agent]))
    await queue.start()
    try:
        first = await queue.submit(design_request())
        second = await queue.submit(design_request())
        await queue.wait_until_idle()

        assert queue.get(first.task_id).status.value == "listo"
        assert queue.get(second.task_id).status.value == "listo"
        assert agent.maximum_active == 1
    finally:
        await queue.stop()


@pytest.mark.asyncio
async def test_queue_exposes_agent_errors_as_error_status() -> None:
    queue = TaskQueue(Orchestrator([]))
    await queue.start()
    try:
        submitted = await queue.submit(design_request())
        await queue.wait_until_idle()

        status = queue.get(submitted.task_id)
        assert status.status.value == "error"
        assert "No hay agente registrado" in (status.error or "")
    finally:
        await queue.stop()
