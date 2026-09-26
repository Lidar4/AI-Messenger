import logging
from typing import Dict, Set
from fastapi import WebSocket

logger = logging.getLogger("ai_backend.websocket_manager")

class ConnectionManager:
    def __init__(self):
        # Maps userId to active WebSocket connections
        self.active_connections: Dict[str, Set[WebSocket]] = {}

    async def connect(self, userId: str, websocket: WebSocket):
        await websocket.accept()
        if userId not in self.active_connections:
            self.active_connections[userId] = set()
        self.active_connections[userId].add(websocket)
        logger.info(f"User {userId} connected via WebSocket.")

    def disconnect(self, userId: str, websocket: WebSocket):
        if userId in self.active_connections:
            self.active_connections[userId].discard(websocket)
            if not self.active_connections[userId]:
                del self.active_connections[userId]
        logger.info(f"User {userId} disconnected from WebSocket.")

    async def send_personal_message(self, userId: str, message: dict):
        if userId in self.active_connections:
            dead_sockets = set()
            for connection in self.active_connections[userId]:
                try:
                    await connection.send_json(message)
                except Exception as e:
                    logger.error(f"Error sending WebSocket message to user {userId}: {e}")
                    dead_sockets.add(connection)
            for ds in dead_sockets:
                self.active_connections[userId].discard(ds)

    async def broadcast_to_users(self, userIds: Set[str], message: dict):
        for uid in userIds:
            await self.send_personal_message(uid, message)

manager = ConnectionManager()
