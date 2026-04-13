from fastapi import FastAPI, HTTPException, Depends, BackgroundTasks
from sqlalchemy import create_engine, Column, Integer, String, Float, DateTime, JSON, Enum
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker, Session
from pydantic import BaseModel
from typing import List, Optional
from datetime import datetime
import enum
import httpx
import asyncio
from kafka import KafkaProducer, KafkaConsumer
import json
import pickle

app = FastAPI(title="Order Service", port=8003)

# Configuration
DATABASE_URL = "postgresql://user:password@postgres-db:5432/orders_db"
KAFKA_BOOTSTRAP = "kafka:9092"

engine = create_engine(DATABASE_URL)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
Base = declarative_base()

# Kafka
producer = KafkaProducer(
    bootstrap_servers=KAFKA_BOOTSTRAP,
    value_serializer=lambda v: pickle.dumps(v)
)

class OrderStatus(str, enum.Enum):
    PENDING = "pending"
    CONFIRMED = "confirmed"
    PROCESSING = "processing"
    SHIPPED = "shipped"
    DELIVERED = "delivered"
    CANCELLED = "cancelled"

class Order(Base):
    __tablename__ = "orders"
    
    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, nullable=False)
    items = Column(JSON, nullable=False)
    total_amount = Column(Float, nullable=False)
    status = Column(Enum(OrderStatus), default=OrderStatus.PENDING)
    created_at = Column(DateTime, default=datetime.utcnow)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)
    shipping_address = Column(JSON)
    payment_status = Column(String, default="pending")

class OrderItem(BaseModel):
    product_id: int
    quantity: int
    price: float

class OrderCreate(BaseModel):
    items: List[OrderItem]
    shipping_address: dict

class OrderResponse(BaseModel):
    id: int
    user_id: int
    items: List[dict]
    total_amount: float
    status: OrderStatus
    created_at: datetime

Base.metadata.create_all(bind=engine)

def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

async def validate_products_stock(items: List[OrderItem]) -> tuple:
    """Vérifie les stocks et calcule le total"""
    async with httpx.AsyncClient() as client:
        total = 0
        for item in items:
            response = await client.get(f"http://product-service:8002/api/products/{item.product_id}")
            if response.status_code != 200:
                raise HTTPException(status_code=404, detail=f"Produit {item.product_id} non trouvé")
            
            product = response.json()
            if product["stock"] < item.quantity:
                raise HTTPException(status_code=400, detail=f"Stock insuffisant pour {product['name']}")
            
            total += product["price"] * item.quantity
        
        return total

async def send_order_event(event_type: str, order_data: dict):
    """Envoie événement commande via Kafka"""
    event = {
        "type": event_type,
        "order": order_data,
        "timestamp": datetime.utcnow().isoformat()
    }
    producer.send("order-events", value=event)

@app.post("/api/orders", response_model=OrderResponse)
async def create_order(order_data: OrderCreate, user_id: int, background_tasks: BackgroundTasks, db: Session = Depends(get_db)):
    """Crée une nouvelle commande"""
    # Validation des stocks
    total_amount = await validate_products_stock(order_data.items)
    
    # Création commande
    new_order = Order(
        user_id=user_id,
        items=[item.dict() for item in order_data.items],
        total_amount=total_amount,
        shipping_address=order_data.shipping_address
    )
    
    db.add(new_order)
    db.commit()
    db.refresh(new_order)
    
    # Tâches en arrière-plan
    background_tasks.add_task(process_order_async, new_order.id)
    background_tasks.add_task(send_order_event, "order_created", new_order.__dict__)
    
    return new_order

async def process_order_async(order_id: int):
    """Traitement asynchrone de la commande"""
    await asyncio.sleep(2)  # Simulation traitement
    
    async with httpx.AsyncClient() as client:
        # Mise à jour des stocks
        db = next(get_db())
        order = db.query(Order).filter(Order.id == order_id).first()
        
        for item in order.items:
            await client.patch(
                f"http://product-service:8002/api/products/{item['product_id']}/stock",
                params={"quantity": item['quantity'], "operation": "decrement"}
            )
        
        # Mise à jour statut
        order.status = OrderStatus.CONFIRMED
        db.commit()

@app.get("/api/orders/user/{user_id}")
async def get_user_orders(user_id: int, skip: int = 0, limit: int = 100, db: Session = Depends(get_db)):
    """Récupère les commandes d'un utilisateur"""
    orders = db.query(Order).filter(Order.user_id == user_id).offset(skip).limit(limit).all()
    return orders

@app.get("/api/orders/{order_id}", response_model=OrderResponse)
async def get_order(order_id: int, db: Session = Depends(get_db)):
    """Récupère une commande spécifique"""
    order = db.query(Order).filter(Order.id == order_id).first()
    if not order:
        raise HTTPException(status_code=404, detail="Commande non trouvée")
    return order

@app.patch("/api/orders/{order_id}/status")
async def update_order_status(order_id: int, status: OrderStatus, db: Session = Depends(get_db)):
    """Met à jour le statut d'une commande"""
    order = db.query(Order).filter(Order.id == order_id).first()
    if not order:
        raise HTTPException(status_code=404, detail="Commande non trouvée")
    
    order.status = status
    db.commit()
    
    await send_order_event("status_updated", {"order_id": order_id, "status": status})
    
    return {"order_id": order_id, "status": status}
