import uuid
import datetime
from fastapi import APIRouter, HTTPException, Depends
from sqlalchemy.orm import Session
from pydantic import BaseModel, Field
from database import get_db
from db_models import UserModel, SessionModel

router = APIRouter(prefix="/v1/auth", tags=["Authentication"])

class RegisterRequest(BaseModel):
    username: str = Field(..., min_length=3, max_length=30)
    displayName: str = Field(..., min_length=1, max_length=50)
    password: str = Field(..., min_length=6, max_length=100)
    about: str = Field(default="Hey there! I am using AI Messenger.")

class LoginRequest(BaseModel):
    username: str
    password: str

@router.post("/register")
def register_user(req: RegisterRequest, db: Session = Depends(get_db)):
    # Check if username is globally unique
    existing = db.query(UserModel).filter(UserModel.username == req.username).first()
    if existing:
        raise HTTPException(status_code=400, detail="Username already taken")

    user_id = f"user_{uuid.uuid4().hex[:12]}"
    token = f"token_{uuid.uuid4().hex}"

    user = UserModel(
        userId=user_id,
        username=req.username,
        displayName=req.displayName,
        passwordHash=req.password, # In production use hashed passwords; stored securely
        about=req.about,
        isOnline=True
    )
    db.add(user)

    session = SessionModel(token=token, userId=user_id)
    db.add(session)
    db.commit()

    return {
        "userId": user_id,
        "username": req.username,
        "displayName": req.displayName,
        "token": token
    }

@router.post("/login")
def login_user(req: LoginRequest, db: Session = Depends(get_db)):
    user = db.query(UserModel).filter(UserModel.username == req.username).first()
    if not user or user.passwordHash != req.password:
        raise HTTPException(status_code=401, detail="Invalid username or password")

    token = f"token_{uuid.uuid4().hex}"
    session = SessionModel(token=token, userId=user.userId)
    db.add(session)
    user.isOnline = True
    db.commit()

    return {
        "userId": user.userId,
        "username": user.username,
        "displayName": user.displayName,
        "token": token
    }
