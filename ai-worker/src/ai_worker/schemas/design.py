"""Contracts for garment design and variant editing."""

from typing import Literal

from pydantic import Field, model_validator

from ai_worker.schemas.common import StrictModel


class DesignInput(StrictModel):
    """Input for generating or editing garment design variants."""

    agent: Literal["design"] = "design"
    operation: Literal["generate", "edit"] = "generate"
    garment_type: str = Field(min_length=1, max_length=80)
    gender: str = Field(min_length=1, max_length=40)
    season: str = Field(min_length=1, max_length=40)
    style: str = Field(min_length=1, max_length=80)
    palette: list[str] = Field(min_length=1, max_length=8)
    fabric: str = Field(min_length=1, max_length=80)
    description: str = Field(default="", max_length=2000)
    variants: int = Field(default=1, ge=1, le=4)
    variant_id: str | None = Field(default=None, min_length=1, max_length=80)
    edit_instruction: str | None = Field(default=None, min_length=1, max_length=1000)

    @model_validator(mode="after")
    def validate_edit_fields(self) -> "DesignInput":
        """Require a target and instruction for edits, but not for generation."""
        if self.operation == "edit":
            if self.variant_id is None or self.edit_instruction is None:
                raise ValueError(
                    "Las ediciones requieren variant_id y edit_instruction"
                )
            if self.variants != 1:
                raise ValueError("Una edición modifica exactamente una variante")
        elif self.variant_id is not None or self.edit_instruction is not None:
            raise ValueError(
                "variant_id y edit_instruction solo aplican a la edición"
            )
        return self


class DesignVariant(StrictModel):
    """One generated or edited garment proposal."""

    variant_id: str
    image_url: str
    description: str


class DesignOutput(StrictModel):
    """Validated result returned by the design agent."""

    agent: Literal["design"] = "design"
    prompt: str
    variants: list[DesignVariant] = Field(min_length=1, max_length=4)
