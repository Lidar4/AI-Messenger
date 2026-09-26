import json
import logging
from fastapi import APIRouter, WebSocket, WebSocketDisconnect, Depends
from sqlalchemy.orm import Session
from database import get_db
from db_models import SessionModel, UserModel, MessageModel
from websocket_manager import manager

router = APIRouter(tags=["WebSocket Real-Time"])
logger = logging.getLogger("ai_backend.websocket_router")

@router.websocket("/ws/{token}")
async def websocket_endpoint(websocket: WebSocket, token: str, db: Session = Depends(get_db)):
    # Authenticate via token
    session = db.query(SessionModel).filter(SessionModel.token == token).first()
    if not session:
        await websocket.close(code=4001, reason="Invalid authentication token")
        return

    userId = session.userId
    user = db.query(UserModel).filter(UserModel.userId == userId).first()
    if not user:
        await websocket.close(code=4004, reason="User not found")
        return

    await manager.connect(userId, websocket)
    user.isOnline = True
    db.commit()

    try:
        while True:
            data = await websocket.receive_text()
            event = json.loads(data)
            event_type = event.get("event")

            if event_type == "message.delivered":
                msg_id = event.get("messageId")
                msg = db.query(MessageModel).filter(MessageModel.messageId == msg_id).first()
                if msg:
                    msg.status = "DELIVERED"
                    db.commit()
                    # Notify sender
                    await manager.send_personal_message(msg.senderId, {
                        "event": "message.delivered",
                        "messageId": msg_id,
                        "status": "DELIVERED"
                    })

            elif event_type == "message.read":
                msg_id = event.get("messageId")
                msg = db.query(MessageModel).filter(MessageModel.messageId == msg_id).first()
                if msg:
                    msg.status = "READ"
                    db.commit()
                    # Notify sender
                    await manager.send_personal_message(msg.senderId, {
                        "event": "message.read",
                        "messageId": msg_id,
                        "status": "READ"
                    })

            elif event_type in ["typing.start", "typing.stop"]:
                recipient_id = event.get("recipientId")
                if recipient_id:
                    await manager.send_personal_message(recipient_id, {
                        "event": event_type,
                        "senderId": userId
                    })

    except WebSocketDisconnect:
        manager.disconnect(userId, websocket)
        user.isOnline = False
        db.commit()
        logger.info(f"WebSocket disconnected for user {userId}")
    except Exception as e:
        logger.error(f"WebSocket error for user {userId}: {e}")
        manager.disconnect(userId, websocket)
        user.isOnline = False
        db.commit()
