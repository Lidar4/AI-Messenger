import os
import httpx
import json
import logging
from abc import ABC, abstractmethod
from typing import Optional, Dict, Any

logger = logging.getLogger("ai_backend.model_adapter")

class AIModelProvider(ABC):
    @abstractmethod
    async def chat(self, prompt: str, system_prompt: Optional[str] = None, personality: str = "helpful", response_length: str = "balanced") -> str:
        pass

    @abstractmethod
    async def translate(self, text: str, target_language: str) -> str:
        pass

    @abstractmethod
    async def summarize(self, text: str) -> str:
        pass

    @abstractmethod
    async def generate_creative(self, request_type: str, prompt: str) -> Dict[str, Any]:
        pass

class SelfHostedModelProvider(AIModelProvider):
    def __init__(self):
        self.runtime_url = os.getenv("MODEL_RUNTIME_URL", "http://localhost:11434")
        self.model_name = os.getenv("MODEL_NAME", "llama3")
        self.timeout = float(os.getenv("MODEL_TIMEOUT_SECONDS", "45.0"))
        logger.info(f"Initialized SelfHostedModelProvider with runtime: {self.runtime_url}, model: {self.model_name}")

    async def _call_runtime(self, prompt: str, system_prompt: Optional[str] = None) -> str:
        url = f"{self.runtime_url}/api/generate"
        full_prompt = prompt
        if system_prompt:
            full_prompt = f"System: {system_prompt}\n\nUser: {prompt}"

        payload = {
            "model": self.model_name,
            "prompt": full_prompt,
            "stream": False
        }

        async with httpx.AsyncClient(timeout=self.timeout) as client:
            try:
                response = await client.post(url, json=payload)
                if response.status_code != 200:
                    raise Exception(f"Model runtime returned status {response.status_code}: {response.text}")
                data = response.json()
                return data.get("response", data.get("text", ""))
            except httpx.RequestError as e:
                logger.error(f"Failed to connect to model runtime at {url}: {e}")
                raise Exception(f"Self-hosted model runtime unreachable at {self.runtime_url}: {e}")

    async def chat(self, prompt: str, system_prompt: Optional[str] = None, personality: str = "helpful", response_length: str = "balanced") -> str:
        sys_p = system_prompt or "You are a helpful AI assistant in a secure messenger."
        if personality == "funny":
            sys_p += " Be humorous and witty."
        elif personality == "professional":
            sys_p += " Maintain a highly professional and formal tone."
        elif personality == "study":
            sys_p += " Act as an expert tutor, explaining concepts clearly."
        elif personality == "planning":
            sys_p += " Provide structured, step-by-step action plans."
        elif personality == "creative":
            sys_p += " Be highly creative and expressive."

        if response_length == "short":
            sys_p += " Keep your response concise and brief."
        elif response_length == "detailed":
            sys_p += " Provide a comprehensive and detailed explanation."

        return await self._call_runtime(prompt, sys_p)

    async def translate(self, text: str, target_language: str) -> str:
        prompt = f"Translate the following text accurately into {target_language}. Preserve all names, formatting, and Bengali/English Unicode characters correctly:\n\n\"{text}\""
        sys_prompt = f"You are an expert multilingual translator. Output only the translated text in {target_language}."
        return await self._call_runtime(prompt, sys_prompt)

    async def summarize(self, text: str) -> str:
        prompt = f"Provide a clear and concise summary of the following text:\n\n\"{text}\""
        sys_prompt = "You are a summarization assistant. Provide key points and core insights."
        return await self._call_runtime(prompt, sys_prompt)

    async def generate_creative(self, request_type: str, prompt: str) -> Dict[str, Any]:
        sys_prompt = (
            "You are a creative content generator. You must return your response STRICTLY as a valid JSON object "
            "with the following keys: 'type', 'title', 'message', 'theme', 'html'. "
            "Do not wrap the JSON in markdown code blocks. Output raw JSON only."
        )
        full_prompt = f"Generate a creative artifact of type '{request_type}' based on this prompt: '{prompt}'."
        raw_response = await self._call_runtime(full_prompt, sys_prompt)
        
        try:
            # Clean potential markdown wrappers
            cleaned = raw_response.strip()
            if cleaned.startswith("```json"):
                cleaned = cleaned[7:]
            if cleaned.endswith("```"):
                cleaned = cleaned[:-3]
            cleaned = cleaned.strip()
            
            parsed = json.loads(cleaned)
            return parsed
        except Exception as e:
            logger.warning(f"Failed to parse JSON creative output, falling back to structured dict: {e}")
            return {
                "type": request_type,
                "title": "Creative Generated Asset",
                "message": raw_response,
                "theme": "modern",
                "html": f"<div style='padding:20px; font-family:sans-serif;'><h2>Generated Content</h2><p>{raw_response}</p></div>"
            }
