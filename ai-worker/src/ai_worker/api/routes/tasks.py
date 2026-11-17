"""Create agent tasks and retrieve their status."""

from typing import Annotated
from uuid import UUID

from fastapi import APIRouter, Depends, HTTPException, Request, status

from ai_worker.schemas.tasks import TaskRequest, TaskStatusResponse
from ai_worker.task_queue import TaskQueue

router = APIRouter(prefix="/tasks", tags=["tasks"])


def get_task_queue(request: Request) -> TaskQueue:
    """Resolve the application-scoped task queue."""
    return request.app.state.task_queue


@router.post("", response_model=TaskStatusResponse, status_code=status.HTTP_202_ACCEPTED)
async def create_task(
    payload: TaskRequest,
    queue: Annotated[TaskQueue, Depends(get_task_queue)],
) -> TaskStatusResponse:
    """Validate and enqueue an agent task."""
    return await queue.submit(payload)


@router.get("/{task_id}", response_model=TaskStatusResponse)
async def get_task(
    task_id: UUID,
    queue: Annotated[TaskQueue, Depends(get_task_queue)],
) -> TaskStatusResponse:
    """Retrieve the latest status of a queued task."""
    try:
        return queue.get(task_id)
    except KeyError as exc:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Tarea no encontrada",
        ) from exc
