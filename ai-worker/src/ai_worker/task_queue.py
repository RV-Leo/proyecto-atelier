"""Single-consumer in-process queue for GPU-bound agent tasks."""

import asyncio
import logging
from dataclasses import dataclass
from datetime import datetime, timezone
from uuid import UUID, uuid4

from ai_worker.orchestrator import Orchestrator
from ai_worker.schemas.agent_outputs import AgentOutput
from ai_worker.schemas.common import AgentType, TaskStatus
from ai_worker.schemas.tasks import TaskRequest, TaskStatusResponse

logger = logging.getLogger(__name__)


@dataclass
class _Task:
    """Mutable in-memory task state."""

    request: TaskRequest
    status: TaskStatus
    created_at: datetime
    updated_at: datetime
    result: AgentOutput | None = None
    error: str | None = None


class TaskQueue:
    """Queue and execute tasks sequentially through one worker coroutine."""

    def __init__(self, orchestrator: Orchestrator) -> None:
        self._orchestrator = orchestrator
        self._pending: asyncio.Queue[UUID | None] = asyncio.Queue()
        self._tasks: dict[UUID, _Task] = {}
        self._worker: asyncio.Task[None] | None = None

    async def start(self) -> None:
        """Start the single queue consumer."""
        if self._worker is not None and not self._worker.done():
            return
        self._worker = asyncio.create_task(self._run(), name="atelier-ai-worker")

    async def stop(self) -> None:
        """Drain pending work and stop the queue consumer."""
        worker = self._worker
        if worker is None:
            return
        await self._pending.join()
        await self._pending.put(None)
        await worker
        self._worker = None

    async def submit(self, request: TaskRequest) -> TaskStatusResponse:
        """Store a task and enqueue it for sequential execution."""
        if self._worker is None or self._worker.done():
            raise RuntimeError("La cola de tareas no está iniciada")
        task_id = uuid4()
        now = datetime.now(timezone.utc)
        self._tasks[task_id] = _Task(
            request=request,
            status=TaskStatus.LOADING,
            created_at=now,
            updated_at=now,
        )
        await self._pending.put(task_id)
        return self.get(task_id)

    def get(self, task_id: UUID) -> TaskStatusResponse:
        """Return a validated status snapshot or raise KeyError."""
        task = self._tasks.get(task_id)
        if task is None:
            raise KeyError(task_id)
        return TaskStatusResponse(
            task_id=task_id,
            agent=AgentType(task.request.agent),
            status=task.status,
            result=task.result,
            error=task.error,
            created_at=task.created_at,
            updated_at=task.updated_at,
        )

    async def wait_until_idle(self) -> None:
        """Wait until every queued task has completed."""
        await self._pending.join()

    async def _run(self) -> None:
        """Consume queued tasks one at a time."""
        while True:
            task_id = await self._pending.get()
            try:
                if task_id is None:
                    return
                task = self._tasks[task_id]
                try:
                    result = await self._orchestrator.execute(task.request)
                except Exception as exc:
                    task.status = TaskStatus.ERROR
                    task.error = f"{type(exc).__name__}: {exc}"
                    logger.exception("Task %s failed", task_id)
                else:
                    task.status = TaskStatus.READY
                    task.result = result
                task.updated_at = datetime.now(timezone.utc)
            finally:
                self._pending.task_done()
