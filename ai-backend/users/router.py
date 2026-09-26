from fastapi import APIRouter, HTTPException, Depends, Header
from sqlalchemy.orm import Session
from database import get_db
from db_models import UserModel, SessionModel, ContactModel

router = APIRouter(prefix="/v1/users", tags=["Users"])

def get_current_user(authorization: str = Header(...), db: Session = Depends(get_db)) -> UserModel:
    if not authorization.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="Invalid authorization header")
    token = authorization.split(" ")[1]
    session = db.query(SessionModel).filter(SessionModel.token == token).first()
    if not session:
        raise HTTPException(status_code=401, detail="Invalid or expired session token")
    user = db.query(UserModel).filter(UserModel.userId == session.userId).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    return user

@router.get("/search")
def search_users(username: str, db: Session = Depends(get_db), current_user: UserModel = Depends(get_current_user)):
    users = db.query(UserModel).filter(UserModel.username.like(f"%{username}%")).all()
    return [
        {
            "userId": u.userId,
            "username": u.username,
            "displayName": u.displayName,
            "about": u.about,
            "isOnline": u.isOnline
        } for u in users if u.userId != current_user.userId
    ]

@router.post("/contacts/{contactUserId}")
def add_contact(contactUserId: str, db: Session = Depends(get_db), current_user: UserModel = Depends(get_current_user)):
    target = db.query(UserModel).filter(UserModel.userId == contactUserId).first()
    if not target:
        raise HTTPException(status_code=404, detail="User not found")
    
    existing = db.query(ContactModel).filter(
        ContactModel.userId == current_user.userId,
        ContactModel.contactUserId == contactUserId
    ).first()
    if existing:
        return {"status": "already added"}

    contact = ContactModel(userId=current_user.userId, contactUserId=contactUserId)
    db.add(contact)
    db.commit()
    return {"status": "success"}

@router.get("/contacts")
def get_contacts(db: Session = Depends(get_db), current_user: UserModel = Depends(get_current_user)):
    contacts = db.query(ContactModel).filter(ContactModel.userId == current_user.userId).all()
    result = []
    for c in contacts:
        u = db.query(UserModel).filter(UserModel.userId == c.contactUserId).first()
        if u:
            result.append({
                "userId": u.userId,
                "username": u.username,
                "displayName": u.displayName,
                "about": u.about,
                "isOnline": u.isOnline
            })
    return result
