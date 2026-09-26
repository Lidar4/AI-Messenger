import logging
from fastapi import APIRouter, HTTPException
from models import GroupChatRequest
from ai.adapter import SelfHostedModelProvider

router = APIRouter(prefix="/v1/group", tags=["Group AI"])
logger = logging.getLogger("ai_backend.groups.router")

model_provider = SelfHostedModelProvider()

@router.post("/chat")
async def group_chat_endpoint(req: GroupChatRequest):
    try:
        context_str = "\n".join(req.recent_messages[-10:])
        full_prompt = f"Recent Group Conversation Context:\n{context_str}\n\nGroup Member Request (@AI): {req.prompt}"
        system_prompt = "You are an AI participant in a group chat. Be conversational, helpful, and concise."
        response = await model_provider.chat(prompt=full_prompt, system_prompt=system_prompt, personality=req.personality or "helpful")
        return {"response": response}
    except Exception as e:
        logger.error(f"Group chat error: {e}")
        raise HTTPException(status_code=503, detail=str(e))
