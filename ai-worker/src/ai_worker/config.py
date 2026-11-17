"""Environment-backed settings for the AI worker."""

from functools import lru_cache
import os
from pathlib import Path

from dotenv import load_dotenv
from pydantic import AnyHttpUrl, BaseModel, SecretStr


class Settings(BaseModel):
    """Validated runtime configuration."""

    qwen_api_base_url: AnyHttpUrl
    qwen_api_key: SecretStr = SecretStr("")
    qwen_text_model: str = "Qwen3.6-35B-A3B"
    qwen_image_model: str = "Qwen-Image"
    spring_boot_base_url: AnyHttpUrl
    spring_boot_api_user: str = ""
    spring_boot_api_password: SecretStr = SecretStr("")
    log_level: str = "INFO"


@lru_cache
def get_settings() -> Settings:
    """Load settings from the worker's .env file and process environment."""
    env_file = Path(__file__).resolve().parents[2] / ".env"
    load_dotenv(env_file, override=False)
    return Settings(
        qwen_api_base_url=os.getenv(
            "QWEN_API_BASE_URL", "http://localhost:8000/v1"
        ),
        qwen_api_key=SecretStr(os.getenv("QWEN_API_KEY", "")),
        qwen_text_model=os.getenv("QWEN_TEXT_MODEL", "Qwen3.6-35B-A3B"),
        qwen_image_model=os.getenv("QWEN_IMAGE_MODEL", "Qwen-Image"),
        spring_boot_base_url=os.getenv(
            "SPRING_BOOT_BASE_URL", "http://localhost:8080"
        ),
        spring_boot_api_user=os.getenv("SPRING_BOOT_API_USER", ""),
        spring_boot_api_password=SecretStr(
            os.getenv("SPRING_BOOT_API_PASSWORD", "")
        ),
        log_level=os.getenv("LOG_LEVEL", "INFO"),
    )
