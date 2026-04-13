from fastapi import FastAPI, Request, HTTPException
from fastapi.middleware.cors import CORSMiddleware
import httpx
from typing import Dict, Any

app = FastAPI(title="API Gateway", version="1.0.0")

# Configuration CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:3000"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Configuration des services
SERVICES = {
    "user": "http://user-service:8001/api",  # Note: /api à la fin
    "product": "http://product-service:8002",
    "order": "http://order-service:8003",
    "notification": "http://notification-service:8004",
    "chatbot": "http://chatbot-service:8005"
}

class ServiceRegistry:
    def __init__(self):
        self.services = SERVICES
        self.client = httpx.AsyncClient(timeout=30.0)
    
    async def route_request(self, service_name: str, path: str, 
                           method: str, data: Dict = None, 
                           headers: Dict = None) -> Dict[str, Any]:
        if service_name not in self.services:
            raise HTTPException(status_code=404, detail=f"Service '{service_name}' non trouvé")
        
        # Construction de l'URL
        base_url = self.services[service_name]
        url = f"{base_url}/{path.lstrip('/')}"
        
        print(f"🔄 {method} {url}")
        
        try:
            # Supprimer Content-Length pour éviter les conflits
            if headers and 'content-length' in headers:
                del headers['content-length']
            
            response = await self.client.request(
                method=method,
                url=url,
                json=data if data else None,
                headers=headers or {}
            )
            
            try:
                response_data = response.json()
            except:
                response_data = response.text
            
            return {"status": response.status_code, "data": response_data}
        except httpx.TimeoutException:
            raise HTTPException(status_code=504, detail="Service timeout")
        except Exception as e:
            print(f"❌ Erreur: {e}")
            raise HTTPException(status_code=500, detail=str(e))

service_registry = ServiceRegistry()

# Route générique pour tous les services
@app.api_route("/api/{service}/{path:path}", methods=["GET", "POST", "PUT", "DELETE", "PATCH"])
async def gateway(service: str, path: str, request: Request):
    """Point d'entrée principal du gateway"""
    data = None
    if request.method in ["POST", "PUT", "PATCH"]:
        try:
            data = await request.json()
        except:
            data = None
    
    headers = dict(request.headers)
    
    result = await service_registry.route_request(
        service_name=service,
        path=path,
        method=request.method,
        data=data,
        headers=headers
    )
    
    return result["data"]

@app.get("/health")
async def health():
    return {"status": "healthy", "gateway": "running"}

print("✅ API Gateway démarré sur le port 8000")
