from fastapi import FastAPI, BackgroundTasks
from pydantic import BaseModel, EmailStr
from typing import List, Optional
from datetime import datetime
import smtplib
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart
import asyncio
from kafka import KafkaConsumer
import json
import threading
import logging

app = FastAPI(title="Notification Service", port=8004)

# Configuration email
SMTP_CONFIG = {
    "host": "smtp.gmail.com",
    "port": 587,
    "user": "your-email@gmail.com",
    "password": "your-password"
}

class EmailRequest(BaseModel):
    to: EmailStr
    subject: str
    body: str
    is_html: bool = False

class SMSRequest(BaseModel):
    phone_number: str
    message: str

class NotificationTemplate(BaseModel):
    name: str
    subject_template: str
    body_template: str

# Templates de notification
TEMPLATES = {
    "order_confirmation": NotificationTemplate(
        name="order_confirmation",
        subject_template="Confirmation de commande #{order_id}",
        body_template="Merci pour votre commande de {total_amount}€. Elle sera traitée prochainement."
    ),
    "order_shipped": NotificationTemplate(
        name="order_shipped",
        subject_template="Votre commande #{order_id} a été expédiée",
        body_template="Votre commande est en route ! Numéro de suivi: {tracking_number}"
    )
}

class NotificationService:
    def __init__(self):
        self.consumer = None
        self.running = False
    
    async def send_email(self, email_data: EmailRequest):
        """Envoie un email"""
        try:
            msg = MIMEMultipart()
            msg['From'] = SMTP_CONFIG['user']
            msg['To'] = email_data.to
            msg['Subject'] = email_data.subject
            
            if email_data.is_html:
                msg.attach(MIMEText(email_data.body, 'html'))
            else:
                msg.attach(MIMEText(email_data.body, 'plain'))
            
            with smtplib.SMTP(SMTP_CONFIG['host'], SMTP_CONFIG['port']) as server:
                server.starttls()
                server.login(SMTP_CONFIG['user'], SMTP_CONFIG['password'])
                server.send_message(msg)
            
            logging.info(f"Email envoyé à {email_data.to}")
            return True
        except Exception as e:
            logging.error(f"Erreur envoi email: {str(e)}")
            return False
    
    async def send_sms(self, sms_data: SMSRequest):
        """Envoie un SMS (simulation)"""
        # Ici, intégration avec un service comme Twilio
        logging.info(f"SMS envoyé à {sms_data.phone_number}: {sms_data.message}")
        return True
    
    async def send_notification(self, user_id: int, notification_type: str, data: dict):
        """Envoie une notification basée sur un template"""
        template = TEMPLATES.get(notification_type)
        if not template:
            return False
        
        # Formatage du message
        subject = template.subject_template.format(**data)
        body = template.body_template.format(**data)
        
        # Récupération email utilisateur (via API)
        async with httpx.AsyncClient() as client:
            response = await client.get(f"http://user-service:8001/api/users/{user_id}")
            if response.status_code == 200:
                user = response.json()
                email_data = EmailRequest(
                    to=user['email'],
                    subject=subject,
                    body=body
                )
                await self.send_email(email_data)
        
        return True
    
    async def consume_order_events(self):
        """Consomme les événements Kafka pour envoyer des notifications"""
        self.consumer = KafkaConsumer(
            'order-events',
            bootstrap_servers=['kafka:9092'],
            auto_offset_reset='earliest',
            enable_auto_commit=True,
            value_deserializer=lambda x: pickle.loads(x)
        )
        
        self.running = True
        for message in self.consumer:
            if not self.running:
                break
            
            event = message.value
            order = event['order']
            
            if event['type'] == 'order_created':
                await self.send_notification(
                    order['user_id'],
                    'order_confirmation',
                    {'order_id': order['id'], 'total_amount': order['total_amount']}
                )
            elif event['type'] == 'status_updated':
                if order['status'] == 'shipped':
                    await self.send_notification(
                        order['user_id'],
                        'order_shipped',
                        {'order_id': order['id'], 'tracking_number': 'TRACK123'}
                    )

notification_service = NotificationService()

@app.on_event("startup")
async def startup_event():
    """Démarre le consommateur Kafka au lancement"""
    asyncio.create_task(notification_service.consume_order_events())

@app.post("/api/notifications/email")
async def send_email(background_tasks: BackgroundTasks, email_data: EmailRequest):
    """Envoie un email"""
    background_tasks.add_task(notification_service.send_email, email_data)
    return {"message": "Email en file d'attente"}

@app.post("/api/notifications/sms")
async def send_sms(background_tasks: BackgroundTasks, sms_data: SMSRequest):
    """Envoie un SMS"""
    background_tasks.add_task(notification_service.send_sms, sms_data)
    return {"message": "SMS en file d'attente"}

@app.post("/api/notifications/template/{template_name}")
async def send_template_notification(user_id: int, template_name: str, data: dict):
    """Envoie une notification avec template"""
    result = await notification_service.send_notification(user_id, template_name, data)
    if result:
        return {"message": "Notification envoyée"}
    else:
        return {"message": "Template non trouvé"}, 404
