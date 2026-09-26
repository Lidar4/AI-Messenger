import logging
from fastapi import APIRouter, HTTPException, Depends
from models import ChatRequest, TranslateRequest, SummarizeRequest, CreativeRequest
from ai.adapter import SelfHostedModelProvider

router = APIRouter(prefix="/v1", tags=["AI"])
logger = logging.getLogger("ai_backend.ai.router")

model_provider = SelfHostedModelProvider()

@router.post("/chat")
async def chat_endpoint(req: ChatRequest):
    try:
        response = await model_provider.chat(
            prompt=req.prompt,
            system_prompt=req.system_prompt,
            personality=req.personality or "helpful",
            response_length=req.response_length or "balanced"
        )
        return {"response": response}
    except Exception as e:
        logger.error(f"Chat error: {e}")
        raise HTTPException(status_code=503, detail=str(e))

@router.post("/translate")
async def translate_endpoint(req: TranslateRequest):
    try:
        translated = await model_provider.translate(req.text, req.target_language)
        return {"translation": translated}
    except Exception as e:
        logger.error(f"Translation error: {e}")
        raise HTTPException(status_code=503, detail=str(e))

@router.post("/summarize")
async def summarize_endpoint(req: SummarizeRequest):
    try:
        summary = await model_provider.summarize(req.text)
        return {"summary": summary}
    except Exception as e:
        logger.error(f"Summarization error: {e}")
        raise HTTPException(status_code=503, detail=str(e))

@router.post("/creative")
async def creative_endpoint(req: CreativeRequest):
    try:
        artifact = await model_provider.generate_creative(req.request_type, req.prompt)
        return artifact
    except Exception as e:
        logger.error(f"Creative generation error: {e}")
        raise HTTPException(status_code=503, detail=str(e))
