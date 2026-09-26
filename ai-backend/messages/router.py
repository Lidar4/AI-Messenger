import uuid
import datetime
from fastapi import APIRouter, HTTPException, Depends, Header
from sqlalchemy.orm import Session
from pydantic import BaseModel, Field
from typing import Optional
from database import get_db
from db_models import UserModel, SessionModel, ConversationModel, ConversationMemberModel, MessageModel, MessageReceiptModel
from users.router import get_current_user
from websocket_manager import manager

router = APIRouter(prefix="/v1/conversations", tags=["Messages & Conversations"])

class SendMessageRequest(BaseModel):
    recipientId: Optional[str] = None # For private chats
    groupId: Optional[str] = None     # For group chats
    content: str = Field(..., min_length=1)
    messageType: str = Field(default="TEXT")

@router.post("")
async def send_message(req: SendMessageRequest, db: Session = Depends(get_db), current_user: UserModel = Depends(get_current_user)):
    # Determine conversation
    conversation_id = None
    recipient_ids = set()

    if req.recipientId:
        # Private chat between current_user and recipientId
        target = db.query(UserModel).filter(UserModel.userId == req.recipientId).first()
        if not target:
            raise HTTPException(status_code=404, detail="Recipient not found")
        
        # Find existing conversation between these two
        # Check conversation members
        convs_a = db.query(ConversationMemberModel.conversationId).filter(ConversationMemberModel.userId == current_user.userId).subquery()
        convs_b = db.query(ConversationMemberModel.conversationId).filter(ConversationMemberModel.userId == req.recipientId).subquery()
        
        shared_conv = db.query(ConversationModel).join(convs_a, ConversationModel.conversationId == convs_a.c.conversationId)\
                                                 .join(convs_b, ConversationModel.conversationId == convs_b.c.conversationId)\
                                                 .filter(ConversationModel.isGroup == False).first()
        if shared_conv:
            conversation_id = shared_conv.conversationId
        else:
            conversation_id = f"conv_{uuid.uuid4().hex[:12]}"
            conv = ConversationModel(conversationId=conversation_id, isGroup=False)
            db.add(conv)
            db.add(ConversationMemberModel(conversationId=conversation_id, userId=current_user.userId))
            db.add(ConversationMemberModel(conversationId=conversation_id, userId=req.recipientId))
            db.commit()
        recipient_ids.add(req.recipientId)

    elif req.groupId:
        conversation_id = req.groupId
        members = db.query(ConversationMemberModel).filter(ConversationMemberModel.conversationId == conversation_id).all()
        for m in members:
            if m.userId != current_user.userId:
                recipient_ids.add(m.userId)
    else:
        raise HTTPException(status_code=400, detail="Must specify recipientId or groupId")

    message_id = f"msg_{uuid.uuid4().hex[:12]}"
    message = MessageModel(
        messageId=message_id,
        conversationId=conversation_id,
        senderId=current_user.userId,
        content=req.content,
        messageType=req.messageType,
        status="SENT"
    )
    db.add(message)
    db.commit()

    # Broadcast via WebSocket to recipients
    payload = {
        "event": "message.new",
        "messageId": message_id,
        "conversationId": conversation_id,
        "senderId": current_user.userId,
        "senderName": current_user.displayName,
        "content": req.content,
        "messageType": req.messageType,
        "status": "SENT",
        "createdAt": message.createdAt.isoformat()
    }

    await manager.broadcast_to_users(recipient_ids, payload)

    return {
        "messageId": message_id,
        "conversationId": conversation_id,
        "status": "SENT",
        "createdAt": message.createdAt.isoformat()
    }

@router.get("/{conversationId}/messages")
def get_messages(conversationId: str, limit: int = 50, db: Session = Depends(get_db), current_user: UserModel = Depends(get_current_user)):
    # Verify membership authorization
    member = db.query(ConversationMemberModel).filter(
        ConversationMemberModel.conversationId == conversationId,
        ConversationMemberModel.userId == current_user.userId
    ).first()
    if not member:
        raise HTTPException(status_code=403, detail="Unauthorized access to conversation")

    messages = db.query(MessageModel).filter(MessageModel.conversationId == conversationId)\
                                     .order_by(MessageModel.createdAt.desc())\
                                     .limit(limit).all()
    return [
        {
            "messageId": m.messageId,
            "conversationId": m.conversationId,
            "senderId": m.senderId,
            "content": m.content,
            "messageType": m.messageType,
            "status": m.status,
            "createdAt": m.createdAt.isoformat()
        } for m in reversed(messages)
    ]
