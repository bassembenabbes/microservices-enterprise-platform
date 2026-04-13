import React, { useState, useEffect } from 'react';
import axios from 'axios';
import './App.css';

const API_GATEWAY = 'http://localhost:8000/api';

function App() {
  // État des services
  const [services, setServices] = useState([
    { name: 'user', displayName: 'User Service', port: 8001, icon: '👤', status: 'checking', description: 'Gestion des utilisateurs et authentification' },
    { name: 'product', displayName: 'Product Service', port: 8002, icon: '📦', status: 'checking', description: 'Catalogue de produits' },
    { name: 'order', displayName: 'Order Service', port: 8003, icon: '🛒', status: 'checking', description: 'Gestion des commandes' },
    { name: 'notification', displayName: 'Notification Service', port: 8004, icon: '📧', status: 'checking', description: 'Notifications email/SMS' },
    { name: 'chatbot', displayName: 'Chatbot Service', port: 8005, icon: '🤖', status: 'checking', description: 'Assistant IA' }
  ]);
  
  // État pour l'API tester
  const [selectedService, setSelectedService] = useState('user');
  const [method, setMethod] = useState('GET');
  const [endpoint, setEndpoint] = useState('');
  const [requestBody, setRequestBody] = useState('');
  const [response, setResponse] = useState('');
  const [loading, setLoading] = useState(false);
  const [activeTab, setActiveTab] = useState('dashboard');
  
  // État pour l'authentification
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [token, setToken] = useState(localStorage.getItem('token') || '');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [email, setEmail] = useState('');
  const [fullName, setFullName] = useState('');
  const [users, setUsers] = useState([]);
  const [currentUser, setCurrentUser] = useState(null);
  
  // État pour le chatbot
  const [chatMessages, setChatMessages] = useState([]);
  const [chatInput, setChatInput] = useState('');
  const [chatLoading, setChatLoading] = useState(false);

  // Effet au chargement
  useEffect(() => {
    checkAllServices();
    if (token) {
      setIsAuthenticated(true);
      fetchCurrentUser();
      fetchUsers();
    }
  }, [token]);

  // Vérifier tous les services
  const checkAllServices = async () => {
    const updatedServices = [...services];
    for (let i = 0; i < updatedServices.length; i++) {
      try {
        const res = await axios.get(`http://localhost:${updatedServices[i].port}/health`, { timeout: 5000 });
        updatedServices[i].status = res.status === 200 ? 'healthy' : 'unhealthy';
      } catch (error) {
        updatedServices[i].status = 'unhealthy';
      }
    }
    setServices(updatedServices);
  };

  // Tester l'API
  const testAPI = async () => {
    setLoading(true);
    setResponse('Chargement...');
    try {
      let url = `${API_GATEWAY}/${selectedService}/${endpoint}`;
      const config = {
        method: method,
        url: url,
        headers: token ? { Authorization: `Bearer ${token}` } : {}
      };
      
      if (method === 'POST' || method === 'PUT') {
        try {
          config.data = requestBody ? JSON.parse(requestBody) : {};
        } catch {
          config.data = { data: requestBody };
        }
      }
      
      const res = await axios(config);
      setResponse(JSON.stringify(res.data, null, 2));
    } catch (error) {
      setResponse(`❌ Erreur: ${error.message}\n\n${error.response?.data ? JSON.stringify(error.response.data, null, 2) : ''}`);
    }
    setLoading(false);
  };

  // Inscription - Endpoint: POST /api/user/users/register
  const register = async () => {
    if (!username || !password || !email) {
      alert('Veuillez remplir tous les champs');
      return;
    }
    
    try {
      const data = {
        username: username.trim(),
        email: email.trim(),
        password: password,
        full_name: fullName.trim() || username
      };
      
      console.log('📝 Inscription:', data);
      
      const response = await axios.post(`${API_GATEWAY}/user/users/register`, data, {
        headers: { 'Content-Type': 'application/json' }
      });
      
      console.log('✅ Réponse:', response.data);
      alert('✅ Utilisateur créé avec succès!');
      setResponse(JSON.stringify(response.data, null, 2));
      setActiveTab('login');
      setPassword('');
      setEmail('');
    } catch (error) {
      console.error('❌ Erreur:', error);
      const errorMsg = error.response?.data?.detail || error.message;
      alert(`❌ Erreur: ${errorMsg}`);
      setResponse(`❌ Erreur: ${errorMsg}`);
    }
  };

  // Connexion - Endpoint: POST /api/user/users/login
  const login = async () => {
    if (!username || !password) {
      alert('Veuillez entrer username et mot de passe');
      return;
    }
    
    try {
      const data = {
        username: username.trim(),
        password: password
      };
      
      console.log('🔐 Login:', data);
      
      const response = await axios.post(`${API_GATEWAY}/user/users/login`, data, {
        headers: { 'Content-Type': 'application/json' }
      });
      
      const newToken = response.data.access_token;
      setToken(newToken);
      localStorage.setItem('token', newToken);
       // Vérifier que le token est bien stocké
    console.log('Token dans localStorage:', localStorage.getItem('token'));
    
      setIsAuthenticated(true);
      alert('✅ Connexion réussie!');
      await fetchCurrentUser(newToken);
      await fetchUsers(newToken);
      setActiveTab('dashboard');
      setResponse(JSON.stringify(response.data, null, 2));
    } catch (error) {
      console.error('❌ Erreur login:', error);
      alert(`❌ Erreur: ${error.response?.data?.detail || error.message}`);
    }
  };

  // Déconnexion
  const logout = () => {
    setToken('');
    localStorage.removeItem('token');
    setIsAuthenticated(false);
    setCurrentUser(null);
    setUsers([]);
    setUsername('');
    setPassword('');
    alert('👋 Déconnecté');
    setActiveTab('dashboard');
  };

  // Récupérer l'utilisateur courant - Endpoint: GET /api/user/users/me
const fetchCurrentUser = async (authToken = token) => {
  if (!authToken) {
    console.log('❌ Pas de token');
    return;
  }
  
  console.log('🔑 Token utilisé:', authToken.substring(0, 50) + '...');
  console.log('📡 Appel à:', `${API_GATEWAY}/user/users/me`);
  
  try {
    const res = await axios.get(`${API_GATEWAY}/user/users/me`, {
      headers: { 
        'Authorization': `Bearer ${authToken}`,
        'Content-Type': 'application/json'
      }
    });
    console.log('✅ Réponse:', res.data);
    setCurrentUser(res.data);
  } catch (error) {
    console.error('❌ Erreur détaillée:', {
      status: error.response?.status,
      data: error.response?.data,
      message: error.message
    });
  }
};

  // Récupérer tous les utilisateurs - Endpoint: GET /api/user
  const fetchUsers = async (authToken = token) => {
    if (!authToken) return;
    try {
      const res = await axios.get(`${API_GATEWAY}/user/users`, {
        headers: { Authorization: `Bearer ${authToken}` }
      });
      setUsers(res.data.users || []);
    } catch (error) {
      console.error('Erreur fetch users:', error);
    }
  };

  // Chatbot - Endpoint: POST /api/chatbot/chat
  const sendChatMessage = async () => {
    if (!chatInput.trim()) return;
    
    const userMessage = { 
      role: 'user', 
      content: chatInput, 
      timestamp: new Date().toISOString() 
    };
    setChatMessages(prev => [...prev, userMessage]);
    setChatInput('');
    setChatLoading(true);
    
    try {
      const res = await axios.post(`${API_GATEWAY}/chatbot/chat`, {
        user_id: currentUser?.id || 'anonymous',
        message: chatInput
      });
      
      const botMessage = { 
        role: 'assistant', 
        content: res.data.response || res.data.message || 'Message reçu!',
        timestamp: new Date().toISOString()
      };
      setChatMessages(prev => [...prev, botMessage]);
    } catch (error) {
      setChatMessages(prev => [...prev, { 
        role: 'assistant', 
        content: `❌ Erreur: ${error.message}`,
        timestamp: new Date().toISOString()
      }]);
    }
    setChatLoading(false);
  };

  return (
    <div className="App">
      {/* Navigation */}
      <nav className="navbar">
        <div className="nav-brand">
          <span className="logo">🚀</span>
          <span className="brand-text">Microservices Platform</span>
        </div>
        <div className="nav-links">
          <button className={activeTab === 'dashboard' ? 'active' : ''} onClick={() => setActiveTab('dashboard')}>
            📊 Dashboard
          </button>
          <button className={activeTab === 'tester' ? 'active' : ''} onClick={() => setActiveTab('tester')}>
            🧪 API Tester
          </button>
          <button className={activeTab === 'chatbot' ? 'active' : ''} onClick={() => setActiveTab('chatbot')}>
            🤖 Chatbot
          </button>
          {!isAuthenticated ? (
            <>
              <button className={activeTab === 'login' ? 'active' : ''} onClick={() => setActiveTab('login')}>
                🔐 Login
              </button>
              <button className={activeTab === 'register' ? 'active' : ''} onClick={() => setActiveTab('register')}>
                📝 Register
              </button>
            </>
          ) : (
            <button onClick={logout} className="logout-btn">
              👋 Logout ({currentUser?.username})
            </button>
          )}
        </div>
      </nav>

      <div className="container">
        {/* Dashboard */}
        {activeTab === 'dashboard' && (
          <div className="dashboard">
            <div className="services-status">
              <h2>📡 État des Services</h2>
              <div className="services-grid">
                {services.map((service) => (
                  <div key={service.name} className={`service-card ${service.status}`}>
                    <div className="service-icon">{service.icon}</div>
                    <div className="service-info">
                      <h3>{service.displayName}</h3>
                      <p>{service.description}</p>
                      <div className="service-meta">
                        <span className="service-port">Port: {service.port}</span>
                        <span className={`status-badge ${service.status}`}>
                          {service.status === 'healthy' ? '✅ Opérationnel' : 
                           service.status === 'checking' ? '🔄 Vérification...' : '❌ Indisponible'}
                        </span>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
            
            {isAuthenticated && currentUser && (
              <div className="user-profile">
                <h2>👤 Profil Utilisateur</h2>
                <div className="profile-card">
                  <p><strong>ID:</strong> {currentUser.id}</p>
                  <p><strong>Username:</strong> {currentUser.username}</p>
                  <p><strong>Email:</strong> {currentUser.email}</p>
                  <p><strong>Nom complet:</strong> {currentUser.full_name}</p>
                  <p><strong>Membre depuis:</strong> {new Date(currentUser.created_at).toLocaleDateString()}</p>
                </div>
                
                {users.length > 0 && (
                  <div className="users-list">
                    <h3>📋 Liste des utilisateurs ({users.length})</h3>
                    <div className="users-grid">
                      {users.map((user, idx) => (
                        <div key={idx} className="user-card">
                          <strong>{user.username}</strong>
                          <small>{user.email}</small>
                        </div>
                      ))}
                    </div>
                  </div>
                )}
              </div>
            )}
          </div>
        )}

        {/* API Tester */}
        {activeTab === 'tester' && (
          <div className="api-tester">
            <h2>🧪 API Tester</h2>
            <div className="api-controls">
              <select value={selectedService} onChange={(e) => setSelectedService(e.target.value)}>
                {services.map(s => (
                  <option key={s.name} value={s.name}>
                    {s.icon} {s.displayName}
                  </option>
                ))}
              </select>
              <select value={method} onChange={(e) => setMethod(e.target.value)}>
                <option value="GET">GET</option>
                <option value="POST">POST</option>
                <option value="PUT">PUT</option>
                <option value="DELETE">DELETE</option>
              </select>
              <input 
                type="text" 
                value={endpoint} 
                onChange={(e) => setEndpoint(e.target.value)}
                placeholder="Endpoint (ex: users, health, users/me)"
                className="endpoint-input"
              />
              <button onClick={testAPI} disabled={loading}>
                {loading ? '⏳ Test en cours...' : '🚀 Tester'}
              </button>
            </div>
            
            {(method === 'POST' || method === 'PUT') && (
              <div className="request-body">
                <label>Corps de la requête (JSON):</label>
                <textarea
                  value={requestBody}
                  onChange={(e) => setRequestBody(e.target.value)}
                  placeholder='{"key": "value"}'
                  rows={5}
                />
              </div>
            )}
            
            {response && (
              <div className="response">
                <div className="response-header">
                  <strong>📋 Réponse:</strong>
                  <button onClick={() => setResponse('')} className="clear-btn">Effacer</button>
                </div>
                <pre>{response}</pre>
              </div>
            )}
          </div>
        )}

        {/* Chatbot */}
        {activeTab === 'chatbot' && (
          <div className="chatbot">
            <h2>🤖 Assistant IA</h2>
            <div className="chat-container">
              <div className="chat-messages">
                {chatMessages.length === 0 && (
                  <div className="welcome-message">
                    <p>👋 Bonjour! Je suis votre assistant virtuel.</p>
                    <p>Posez-moi des questions sur:</p>
                    <ul>
                      <li>📦 Les produits et le catalogue</li>
                      <li>🛒 L'état de vos commandes</li>
                      <li>📧 Les notifications</li>
                      <li>🔧 L'assistance technique</li>
                    </ul>
                  </div>
                )}
                {chatMessages.map((msg, idx) => (
                  <div key={idx} className={`chat-message ${msg.role}`}>
                    <div className="message-header">
                      <strong>{msg.role === 'user' ? '👤 Vous' : '🤖 Assistant'}</strong>
                      <small>{new Date(msg.timestamp).toLocaleTimeString()}</small>
                    </div>
                    <div className="message-content">{msg.content}</div>
                  </div>
                ))}
                {chatLoading && (
                  <div className="typing-indicator">
                    <span></span><span></span><span></span>
                  </div>
                )}
              </div>
              <div className="chat-input-area">
                <input
                  type="text"
                  value={chatInput}
                  onChange={(e) => setChatInput(e.target.value)}
                  onKeyPress={(e) => e.key === 'Enter' && sendChatMessage()}
                  placeholder="Posez votre question..."
                  disabled={chatLoading}
                />
                <button onClick={sendChatMessage} disabled={chatLoading}>
                  {chatLoading ? '...' : 'Envoyer'}
                </button>
              </div>
            </div>
          </div>
        )}

        {/* Formulaire de connexion */}
        {activeTab === 'login' && !isAuthenticated && (
          <div className="auth-form">
            <h2>🔐 Connexion</h2>
            <div className="form-group">
              <input
                type="text"
                placeholder="Nom d'utilisateur"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
              />
              <input
                type="password"
                placeholder="Mot de passe"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                onKeyPress={(e) => e.key === 'Enter' && login()}
              />
              <button onClick={login}>Se connecter</button>
            </div>
            <p className="form-footer">
              Pas encore de compte? <button onClick={() => setActiveTab('register')}>S'inscrire</button>
            </p>
          </div>
        )}

        {/* Formulaire d'inscription */}
        {activeTab === 'register' && !isAuthenticated && (
          <div className="auth-form">
            <h2>📝 Inscription</h2>
            <div className="form-group">
              <input
                type="text"
                placeholder="Nom d'utilisateur *"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
              />
              <input
                type="email"
                placeholder="Email *"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
              />
              <input
                type="text"
                placeholder="Nom complet"
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
              />
              <input
                type="password"
                placeholder="Mot de passe *"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
              />
              <button onClick={register}>S'inscrire</button>
            </div>
            <p className="form-footer">
              Déjà inscrit? <button onClick={() => setActiveTab('login')}>Se connecter</button>
            </p>
          </div>
        )}
      </div>

      {/* Footer */}
      <footer className="footer">
        <div className="footer-content">
          <p>🚀 Microservices Enterprise Platform v1.0</p>
          <p>🐳 Docker | ☸️ Kubernetes | 📊 Prometheus + Grafana | 🔐 JWT</p>
          <p className="footer-status">
            {services.filter(s => s.status === 'healthy').length}/{services.length} services actifs
          </p>
        </div>
      </footer>
    </div>
  );
}

export default App;
