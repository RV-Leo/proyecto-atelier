"""Contracts for fashion trend labeling and classification."""

from enum import StrEnum
from typing import Literal

from pydantic import AnyHttpUrl, Field

from ai_worker.schemas.common import StrictModel


class TrendLevel(StrEnum):
    """Coarse trend strength classification."""

    LOW = "bajo"
    MEDIUM = "medio"
    HIGH = "alto"


class TrendInput(StrictModel):
    """Image and context to classify as a fashion trend."""

    agent: Literal["trends"] = "trends"
    image_url: AnyHttpUrl
    region: str = Field(min_length=1, max_length=40)
    source: str | None = Field(default=None, max_length=200)


class TrendOutput(StrictModel):
    """Vision labels and trend classification."""

    agent: Literal["trends"] = "trends"
    colors: list[str] = Field(min_length=1, max_length=12)
    cut: str = Field(min_length=1, max_length=120)
    fabric: str = Field(min_length=1, max_length=120)
    trend_level: TrendLevel
    confidence: float = Field(ge=0, le=1)
