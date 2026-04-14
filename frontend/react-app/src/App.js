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
  
  // État pour les produits
  const [products, setProducts] = useState([]);
  const [productForm, setProductForm] = useState({
    name: '',
    description: '',
    price: '',
    stock: '',
    category: ''
  });
  const [searchQuery, setSearchQuery] = useState('');
  const [searchCategory, setSearchCategory] = useState('');
  const [searchMinPrice, setSearchMinPrice] = useState('');
  const [searchMaxPrice, setSearchMaxPrice] = useState('');
  const [productLoading, setProductLoading] = useState(false);
  const [showProductForm, setShowProductForm] = useState(false);
  const [editingProduct, setEditingProduct] = useState(null);
  
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
      fetchProducts();
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

  // ============================================
  // PRODUCT MANAGEMENT
  // ============================================

  // Récupérer tous les produits
  const fetchProducts = async () => {
    setProductLoading(true);
    try {
      const res = await axios.get(`${API_GATEWAY}/product/products`, { 
        headers: token ? { Authorization: `Bearer ${token}` } : {}
      });
      setProducts(res.data.products || []);
    } catch (error) {
      console.error('Erreur fetch products:', error);
    }
    setProductLoading(false);
  };

  // Rechercher des produits - Version corrigée
// Rechercher des produits - Version finale corrigée
const searchProducts = async () => {
  setProductLoading(true);
  try {
    const params = new URLSearchParams();
    
    if (searchQuery && searchQuery.trim() !== '' && searchQuery !== '*') {
      params.append('q', searchQuery.trim());
    }
    if (searchCategory && searchCategory !== '') {
      params.append('category', searchCategory);
    }
    if (searchMinPrice && searchMinPrice !== '') {
      params.append('min_price', searchMinPrice);
    }
    if (searchMaxPrice && searchMaxPrice !== '') {
      params.append('max_price', searchMaxPrice);
    }
    
    const queryString = params.toString();
    // Correction: encodage correct de l'URL
    const url = queryString 
      ? `${API_GATEWAY}/product/products/search?${queryString}`
      : `${API_GATEWAY}/product/products`;
    
    console.log('🔍 URL encodée:', url);
    console.log('🔍 Paramètres:', {
      searchQuery,
      searchCategory,
      searchMinPrice,
      searchMaxPrice
    });
    
    const res = await axios.get(url, {
      headers: token ? { Authorization: `Bearer ${token}` } : {}
    });
    
    console.log('✅ Résultats trouvés:', res.data.products?.length);
    setProducts(res.data.products || []);
  } catch (error) {
    console.error('❌ Erreur détaillée:', error.response?.data || error.message);
    setProducts([]);
  }
  setProductLoading(false);
};

  // Créer un produit
  const createProduct = async () => {
    if (!productForm.name || !productForm.price || !productForm.stock) {
      alert('Veuillez remplir les champs obligatoires');
      return;
    }
    
    try {
      const res = await axios.post(`${API_GATEWAY}/product/products`, {
        name: productForm.name,
        description: productForm.description || '',
        price: parseFloat(productForm.price),
        stock: parseInt(productForm.stock),
        category: productForm.category || 'General'
      }, {
        headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' }
      });
      
      alert('✅ Produit créé avec succès!');
      setProductForm({ name: '', description: '', price: '', stock: '', category: '' });
      setShowProductForm(false);
      fetchProducts();
    } catch (error) {
      console.error('Erreur création:', error);
      alert(`❌ Erreur: ${error.response?.data?.error || error.message}`);
    }
  };

  // Mettre à jour un produit
  const updateProduct = async () => {
    if (!editingProduct) return;
    
    try {
      const res = await axios.put(`${API_GATEWAY}/product/products/${editingProduct.id}`, {
        name: productForm.name,
        description: productForm.description,
        price: parseFloat(productForm.price),
        stock: parseInt(productForm.stock),
        category: productForm.category
      }, {
        headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' }
      });
      
      alert('✅ Produit mis à jour!');
      setEditingProduct(null);
      setProductForm({ name: '', description: '', price: '', stock: '', category: '' });
      setShowProductForm(false);
      fetchProducts();
    } catch (error) {
      console.error('Erreur mise à jour:', error);
      alert(`❌ Erreur: ${error.response?.data?.error || error.message}`);
    }
  };

  // Supprimer un produit
  const deleteProduct = async (id) => {
    if (!window.confirm('Êtes-vous sûr de vouloir supprimer ce produit ?')) return;
    
    try {
      await axios.delete(`${API_GATEWAY}/product/products/${id}`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      alert('✅ Produit supprimé!');
      fetchProducts();
    } catch (error) {
      console.error('Erreur suppression:', error);
      alert(`❌ Erreur: ${error.response?.data?.error || error.message}`);
    }
  };

  // Mettre à jour le stock
  const updateStock = async (id, quantity, operation) => {
    try {
      const res = await axios.patch(`${API_GATEWAY}/product/products/${id}/stock`, {
        quantity,
        operation
      }, {
        headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' }
      });
      
      alert(`✅ Stock mis à jour! Nouveau stock: ${res.data.new_stock}`);
      fetchProducts();
    } catch (error) {
      console.error('Erreur stock:', error);
      alert(`❌ Erreur: ${error.response?.data?.error || error.message}`);
    }
  };

  // Ouvrir formulaire d'édition
  const editProduct = (product) => {
    setEditingProduct(product);
    setProductForm({
      name: product.name,
      description: product.description || '',
      price: product.price,
      stock: product.stock,
      category: product.category || 'General'
    });
    setShowProductForm(true);
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

  // Inscription
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
      
      const response = await axios.post(`${API_GATEWAY}/user/users/register`, data, {
        headers: { 'Content-Type': 'application/json' }
      });
      
      alert('✅ Utilisateur créé avec succès!');
      setResponse(JSON.stringify(response.data, null, 2));
      setActiveTab('login');
      setPassword('');
      setEmail('');
    } catch (error) {
      const errorMsg = error.response?.data?.detail || error.message;
      alert(`❌ Erreur: ${errorMsg}`);
    }
  };

  // Connexion
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
      
      const response = await axios.post(`${API_GATEWAY}/user/users/login`, data, {
        headers: { 'Content-Type': 'application/json' }
      });
      
      const newToken = response.data.access_token;
      setToken(newToken);
      localStorage.setItem('token', newToken);
      setIsAuthenticated(true);
      alert('✅ Connexion réussie!');
      await fetchCurrentUser(newToken);
      await fetchUsers(newToken);
      await fetchProducts();
      setActiveTab('products');
    } catch (error) {
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
    setProducts([]);
    setUsername('');
    setPassword('');
    alert('👋 Déconnecté');
    setActiveTab('dashboard');
  };

  // Récupérer l'utilisateur courant
  const fetchCurrentUser = async (authToken = token) => {
    if (!authToken) return;
    try {
      const res = await axios.get(`${API_GATEWAY}/user/users/me`, {
        headers: { Authorization: `Bearer ${authToken}` }
      });
      setCurrentUser(res.data);
    } catch (error) {
      console.error('Erreur fetch user:', error);
    }
  };

  // Récupérer tous les utilisateurs
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

  // Chatbot
  const sendChatMessage = async () => {
    if (!chatInput.trim()) return;
    
    const userMessage = { role: 'user', content: chatInput, timestamp: new Date().toISOString() };
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

  // Categories disponibles
  const categories = ['Electronics', 'Clothing', 'Books', 'Home', 'Sports', 'Toys', 'General'];

  return (
    <div className="App">
      <nav className="navbar">
        <div className="nav-brand">
          <span className="logo">🚀</span>
          <span className="brand-text">Microservices Platform</span>
        </div>
        <div className="nav-links">
          <button className={activeTab === 'dashboard' ? 'active' : ''} onClick={() => setActiveTab('dashboard')}>
            📊 Dashboard
          </button>
          <button className={activeTab === 'products' ? 'active' : ''} onClick={() => { setActiveTab('products'); fetchProducts(); }}>
            📦 Products
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
              </div>
            )}
          </div>
        )}

        {/* Products Management */}
        {activeTab === 'products' && isAuthenticated && (
          <div className="products-section">
            <div className="products-header">
              <h2>📦 Gestion des Produits</h2>
              <button className="btn-primary" onClick={() => { setShowProductForm(true); setEditingProduct(null); setProductForm({ name: '', description: '', price: '', stock: '', category: '' }); }}>
                + Nouveau Produit
              </button>
            </div>

            {/* Search Bar */}
            <div className="search-bar">
              <input
                type="text"
                placeholder="Rechercher un produit..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                onKeyPress={(e) => e.key === 'Enter' && searchProducts()}
              />
              <select value={searchCategory} onChange={(e) => setSearchCategory(e.target.value)}>
                <option value="">Toutes catégories</option>
                {categories.map(cat => <option key={cat} value={cat}>{cat}</option>)}
              </select>
              <input
                type="number"
                placeholder="Prix min"
                value={searchMinPrice}
                onChange={(e) => setSearchMinPrice(e.target.value)}
              />
              <input
                type="number"
                placeholder="Prix max"
                value={searchMaxPrice}
                onChange={(e) => setSearchMaxPrice(e.target.value)}
              />
              <button onClick={searchProducts}>🔍 Rechercher</button>
              <button onClick={() => { setSearchQuery(''); setSearchCategory(''); setSearchMinPrice(''); setSearchMaxPrice(''); fetchProducts(); }}>🔄 Réinitialiser</button>
            </div>

            {/* Product Form Modal */}
            {showProductForm && (
              <div className="modal">
                <div className="modal-content">
                  <h3>{editingProduct ? '✏️ Modifier le produit' : '➕ Nouveau produit'}</h3>
                  <input
                    type="text"
                    placeholder="Nom du produit *"
                    value={productForm.name}
                    onChange={(e) => setProductForm({ ...productForm, name: e.target.value })}
                  />
                  <textarea
                    placeholder="Description"
                    value={productForm.description}
                    onChange={(e) => setProductForm({ ...productForm, description: e.target.value })}
                  />
                  <input
                    type="number"
                    placeholder="Prix *"
                    value={productForm.price}
                    onChange={(e) => setProductForm({ ...productForm, price: e.target.value })}
                  />
                  <input
                    type="number"
                    placeholder="Stock *"
                    value={productForm.stock}
                    onChange={(e) => setProductForm({ ...productForm, stock: e.target.value })}
                  />
                  <select value={productForm.category} onChange={(e) => setProductForm({ ...productForm, category: e.target.value })}>
                    <option value="">Sélectionner une catégorie</option>
                    {categories.map(cat => <option key={cat} value={cat}>{cat}</option>)}
                  </select>
                  <div className="modal-buttons">
                    <button onClick={editingProduct ? updateProduct : createProduct}>
                      {editingProduct ? '💾 Mettre à jour' : '✅ Créer'}
                    </button>
                    <button onClick={() => { setShowProductForm(false); setEditingProduct(null); }}>❌ Annuler</button>
                  </div>
                </div>
              </div>
            )}

            {/* Products Grid */}
            {productLoading ? (
              <div className="loading">Chargement des produits...</div>
            ) : (
              <div className="products-grid">
                {products.length === 0 ? (
                  <div className="no-products">Aucun produit trouvé</div>
                ) : (
                  products.map((product) => (
                    <div key={product.id} className="product-card">
                      <div className="product-icon">📦</div>
                      <div className="product-info">
                        <h3>{product.name}</h3>
                        <p className="product-description">{product.description || 'Pas de description'}</p>
                        <div className="product-details">
                          <span className="product-price">{product.price} €</span>
                          <span className={`product-stock ${product.stock > 0 ? 'in-stock' : 'out-of-stock'}`}>
                            Stock: {product.stock}
                          </span>
                          <span className="product-category">{product.category || 'General'}</span>
                        </div>
                        <div className="product-actions">
                          <button onClick={() => updateStock(product.id, 1, 'decrement')}>➖ Vendre</button>
                          <button onClick={() => updateStock(product.id, 1, 'increment')}>➕ Réapprovisionner</button>
                          <button onClick={() => editProduct(product)}>✏️ Modifier</button>
                          <button onClick={() => deleteProduct(product.id)}>🗑️ Supprimer</button>
                        </div>
                      </div>
                    </div>
                  ))
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
                placeholder="Endpoint (ex: products, health)"
              />
              <button onClick={testAPI} disabled={loading}>
                {loading ? '⏳...' : '🚀 Tester'}
              </button>
            </div>
            
            {(method === 'POST' || method === 'PUT') && (
              <textarea
                value={requestBody}
                onChange={(e) => setRequestBody(e.target.value)}
                placeholder='{"key": "value"}'
                rows={4}
              />
            )}
            
            {response && (
              <div className="response">
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
                    <p>Posez-moi des questions sur les produits, commandes, etc.</p>
                  </div>
                )}
                {chatMessages.map((msg, idx) => (
                  <div key={idx} className={`chat-message ${msg.role}`}>
                    <strong>{msg.role === 'user' ? '👤 Vous' : '🤖 Assistant'}:</strong>
                    <p>{msg.content}</p>
                  </div>
                ))}
                {chatLoading && <div className="typing">🤖 L'assistant écrit...</div>}
              </div>
              <div className="chat-input">
                <input
                  type="text"
                  value={chatInput}
                  onChange={(e) => setChatInput(e.target.value)}
                  onKeyPress={(e) => e.key === 'Enter' && sendChatMessage()}
                  placeholder="Posez votre question..."
                />
                <button onClick={sendChatMessage}>Envoyer</button>
              </div>
            </div>
          </div>
        )}

        {/* Login */}
        {activeTab === 'login' && !isAuthenticated && (
          <div className="auth-form">
            <h2>🔐 Connexion</h2>
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
            <p>Pas de compte? <button onClick={() => setActiveTab('register')}>S'inscrire</button></p>
          </div>
        )}

        {/* Register */}
        {activeTab === 'register' && !isAuthenticated && (
          <div className="auth-form">
            <h2>📝 Inscription</h2>
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
            <p>Déjà inscrit? <button onClick={() => setActiveTab('login')}>Se connecter</button></p>
          </div>
        )}
      </div>

      <footer className="footer">
        <p>🐳 Docker | ☸️ Kubernetes | 📊 Prometheus + Grafana | 🔐 JWT</p>
        <p>Microservices Enterprise Platform v2.0 - Product Management</p>
      </footer>
    </div>
  );
}

export default App;
