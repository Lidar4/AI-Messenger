from pydantic import BaseModel, Field
from typing import Optional, List, Dict, Any

class ChatRequest(BaseModel):
    prompt: str = Field(..., min_length=1, max_length=10000)
    system_prompt: Optional[str] = None
    personality: Optional[str] = Field("helpful", pattern="^(helpful|funny|professional|study|planning|creative)$")
    response_length: Optional[str] = Field("balanced", pattern="^(short|balanced|detailed)$")

class TranslateRequest(BaseModel):
    text: str = Field(..., min_length=1, max_length=5000)
    target_language: str = Field(..., min_length=2, max_length=50)

class SummarizeRequest(BaseModel):
    text: str = Field(..., min_length=1, max_length=20000)

class GroupChatRequest(BaseModel):
    prompt: str = Field(..., min_length=1, max_length=5000)
    recent_messages: List[str] = Field(default_factory=list, max_length=20)
    personality: Optional[str] = "helpful"

class CreativeRequest(BaseModel):
    request_type: str = Field(..., min_length=1, max_length=50)
    prompt: str = Field(..., min_length=1, max_length=5000)
