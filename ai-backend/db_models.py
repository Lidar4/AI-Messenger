import datetime
from sqlalchemy import Column, String, Integer, Boolean, Text, DateTime, ForeignKey, Enum
from database import Base

class UserModel(Base):
    __tablename__ = "users"
    userId = Column(String, primary_key=True, index=True)
    username = Column(String, unique=True, index=True, nullable=False)
    displayName = Column(String, nullable=False)
    passwordHash = Column(String, nullable=False)
    profilePhoto = Column(String, nullable=True)
    about = Column(String, nullable=True)
    createdAt = Column(DateTime, default=datetime.datetime.utcnow)
    isOnline = Column(Boolean, default=False)
    lastSeen = Column(DateTime, default=datetime.datetime.utcnow)

class SessionModel(Base):
    __tablename__ = "sessions"
    token = Column(String, primary_key=True, index=True)
    userId = Column(String, ForeignKey("users.userId"), nullable=False)
    createdAt = Column(DateTime, default=datetime.datetime.utcnow)

class ContactModel(Base):
    __tablename__ = "contacts"
    id = Column(Integer, primary_key=True, autoincrement=True)
    userId = Column(String, ForeignKey("users.userId"), nullable=False)
    contactUserId = Column(String, ForeignKey("users.userId"), nullable=False)
    createdAt = Column(DateTime, default=datetime.datetime.utcnow)

class ConversationModel(Base):
    __tablename__ = "conversations"
    conversationId = Column(String, primary_key=True, index=True)
    isGroup = Column(Boolean, default=False)
    name = Column(String, nullable=True)
    createdAt = Column(DateTime, default=datetime.datetime.utcnow)

class ConversationMemberModel(Base):
    __tablename__ = "conversation_members"
    id = Column(Integer, primary_key=True, autoincrement=True)
    conversationId = Column(String, ForeignKey("conversations.conversationId"), nullable=False)
    userId = Column(String, ForeignKey("users.userId"), nullable=False)
    role = Column(String, default="MEMBER") # OWNER, ADMIN, MEMBER
    joinedAt = Column(DateTime, default=datetime.datetime.utcnow)

class MessageModel(Base):
    __tablename__ = "messages"
    messageId = Column(String, primary_key=True, index=True)
    conversationId = Column(String, ForeignKey("conversations.conversationId"), nullable=False)
    senderId = Column(String, ForeignKey("users.userId"), nullable=False)
    content = Column(Text, nullable=False)
    messageType = Column(String, default="TEXT") # TEXT, IMAGE, AUDIO, VOICE, FILE, LOCATION, AI_ARTIFACT
    status = Column(String, default="SENT") # SENDING, SENT, DELIVERED, READ, FAILED
    createdAt = Column(DateTime, default=datetime.datetime.utcnow)
    editedAt = Column(DateTime, nullable=True)
    deletedAt = Column(DateTime, nullable=True)

class MessageReceiptModel(Base):
    __tablename__ = "message_receipts"
    id = Column(Integer, primary_key=True, autoincrement=True)
    messageId = Column(String, ForeignKey("messages.messageId"), nullable=False)
    userId = Column(String, ForeignKey("users.userId"), nullable=False)
    status = Column(String, default="DELIVERED") # DELIVERED, READ
    updatedAt = Column(DateTime, default=datetime.datetime.utcnow)

class GroupModel(Base):
    __tablename__ = "groups"
    groupId = Column(String, primary_key=True, index=True)
    name = Column(String, nullable=False)
    photo = Column(String, nullable=True)
    description = Column(String, nullable=True)
    creatorId = Column(String, ForeignKey("users.userId"), nullable=False)
    createdAt = Column(DateTime, default=datetime.datetime.utcnow)

class GroupMemberModel(Base):
    __tablename__ = "group_members"
    id = Column(Integer, primary_key=True, autoincrement=True)
    groupId = Column(String, ForeignKey("groups.groupId"), nullable=False)
    userId = Column(String, ForeignKey("users.userId"), nullable=False)
    role = Column(String, default="MEMBER") # OWNER, ADMIN, MEMBER
    joinedAt = Column(DateTime, default=datetime.datetime.utcnow)

class NotificationModel(Base):
    __tablename__ = "notifications"
    notificationId = Column(String, primary_key=True, index=True)
    userId = Column(String, ForeignKey("users.userId"), nullable=False)
    title = Column(String, nullable=False)
    body = Column(String, nullable=False)
    isRead = Column(Boolean, default=False)
    createdAt = Column(DateTime, default=datetime.datetime.utcnow)
