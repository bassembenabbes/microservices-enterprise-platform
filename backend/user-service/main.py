from fastapi import FastAPI, HTTPException, Depends, Header
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy import create_engine, Column, String, Boolean, DateTime
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker, Session
from pydantic import BaseModel, EmailStr
from typing import Optional, List
from datetime import datetime, timedelta
import uuid
import hashlib
import jwt
import os

app = FastAPI(title="User Service")

# CORS - Configuration complète
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:3000", "http://localhost:8000", "*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
    expose_headers=["*"],
)

# Configuration
DATABASE_URL = os.getenv("DATABASE_URL", "postgresql://user:password@postgres-user:5432/users_db")
SECRET_KEY = os.getenv("JWT_SECRET", "my-super-secret-key-change-me")
TOKEN_EXPIRY = 86400

# Base de données
engine = create_engine(DATABASE_URL)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
Base = declarative_base()

# Modèle
class UserDB(Base):
    __tablename__ = "users"
    id = Column(String, primary_key=True, index=True)
    email = Column(String, unique=True, index=True, nullable=False)
    username = Column(String, unique=True, index=True, nullable=False)
    password_hash = Column(String, nullable=False)
    full_name = Column(String)
    is_active = Column(Boolean, default=True)
    created_at = Column(DateTime, default=datetime.utcnow)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)

# Créer les tables
Base.metadata.create_all(bind=engine)

# Modèles Pydantic
class UserRegister(BaseModel):
    email: EmailStr
    username: str
    password: str
    full_name: str

class UserLogin(BaseModel):
    username: str
    password: str

class TokenResponse(BaseModel):
    access_token: str
    token_type: str
    expires_in: int

# Dépendance DB
def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

# Utilitaires
def hash_password(password: str) -> str:
    return hashlib.sha256(password.encode()).hexdigest()

def verify_password(password: str, hashed: str) -> bool:
    return hash_password(password) == hashed

def create_token(user_id: str, username: str) -> str:
    payload = {
        "user_id": user_id,
        "username": username,
        "exp": datetime.utcnow() + timedelta(seconds=TOKEN_EXPIRY)
    }
    return jwt.encode(payload, SECRET_KEY, algorithm="HS256")

def decode_token(token: str) -> dict:
    return jwt.decode(token, SECRET_KEY, algorithms=["HS256"])

# Routes
@app.get("/health")
def health():
    return {"status": "healthy", "service": "user-service"}

@app.post("/api/users/register")
def register(user: UserRegister, db: Session = Depends(get_db)):
    existing = db.query(UserDB).filter(
        (UserDB.username == user.username) | (UserDB.email == user.email)
    ).first()
    if existing:
        raise HTTPException(400, "Username or email already exists")
    
    user_id = str(uuid.uuid4())
    new_user = UserDB(
        id=user_id,
        email=user.email,
        username=user.username,
        password_hash=hash_password(user.password),
        full_name=user.full_name
    )
    db.add(new_user)
    db.commit()
    db.refresh(new_user)
    return {"id": user_id, "username": user.username, "email": user.email, "full_name": user.full_name, "created_at": new_user.created_at}

@app.post("/api/users/login")
def login(user: UserLogin, db: Session = Depends(get_db)):
    db_user = db.query(UserDB).filter(UserDB.username == user.username).first()
    if not db_user or not verify_password(user.password, db_user.password_hash):
        raise HTTPException(401, "Invalid credentials")
    
    token = create_token(db_user.id, db_user.username)
    return {"access_token": token, "token_type": "bearer", "expires_in": TOKEN_EXPIRY}

@app.get("/api/users")
def get_all_users(db: Session = Depends(get_db)):
    users = db.query(UserDB).all()
    return {"total": len(users), "users": [{"id": u.id, "username": u.username, "email": u.email, "full_name": u.full_name} for u in users]}

@app.get("/api/users/me")
def get_me(authorization: str = Header(None), db: Session = Depends(get_db)):
    if not authorization or not authorization.startswith("Bearer "):
        raise HTTPException(401, "Invalid token")
    
    token = authorization[7:]
    try:
        payload = decode_token(token)
        user = db.query(UserDB).filter(UserDB.id == payload["user_id"]).first()
        if not user:
            raise HTTPException(404, "User not found")
        return {"id": user.id, "username": user.username, "email": user.email, "full_name": user.full_name, "created_at": user.created_at}
    except jwt.ExpiredSignatureError:
        raise HTTPException(401, "Token expired")
    except jwt.InvalidTokenError:
        raise HTTPException(401, "Invalid token")

print("✅ User Service démarré sur le port 8001")

