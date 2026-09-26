import os
import logging
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from database import engine, Base
from db_models import * # Load all models
from auth.router import router as auth_router
from users.router import router as users_router
from messages.router import router as messages_router
from ai.router import router as ai_router
from groups.router import router as group_router
from websocket_router import router as websocket_router

# Create database tables
Base.metadata.create_all(bind=engine)

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("ai_backend.main")

app = FastAPI(
    title="Self-Hosted AI & Messaging Backend for AI Messenger",
    version="1.0.0",
    description="Production multi-user messaging backend with real-time WebSockets and self-hosted AI."
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Include all modular routers
app.include_router(auth_router)
app.include_router(users_router)
app.include_router(messages_router)
app.include_router(ai_router)
app.include_router(group_router)
app.include_router(websocket_router)

@app.get("/health")
async def health_check():
    return {
        "status": "ok",
        "runtime": "self-hosted-messenger-and-ai",
        "database": "postgresql/sqlite-persistent"
    }
