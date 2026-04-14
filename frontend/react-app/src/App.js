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
  
  // État pour les commandes
  const [orders, setOrders] = useState([]);
  const [selectedOrder, setSelectedOrder] = useState(null);
  const [cart, setCart] = useState([]);
  const [shippingAddress, setShippingAddress] = useState({
    street: '',
    city: '',
    postalCode: '',
    country: 'France'
  });
  const [phoneNumber, setPhoneNumber] = useState('');
  const [orderLoading, setOrderLoading] = useState(false);
  const [selectedStatus, setSelectedStatus] = useState('all');
  const [orderStats, setOrderStats] = useState(null);
  const [couponCode, setCouponCode] = useState('');
  const [discount, setDiscount] = useState(0);
  const [paymentMethod, setPaymentMethod] = useState('card');
  
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
      fetchOrders();
      fetchOrderStats();
    }
  }, [token]);

  // Vérifier tous les services
// Dans checkAllServices, il ne faut pas appeler directement les services
// mais utiliser l'API Gateway ou simplement vérifier via le health endpoint de l'API Gateway

// Version corrigée de checkAllServices
// Vérifier tous les services via l'API Gateway
const checkAllServices = async () => {
  const updatedServices = [...services];
  for (let i = 0; i < updatedServices.length; i++) {
    try {
      // Utiliser l'API Gateway au lieu des ports directs
      const url = `${API_GATEWAY}/${updatedServices[i].name}/health`;
      console.log(`Checking service: ${url}`);
      
      const res = await axios.get(url, { 
        timeout: 5000,
        headers: token ? { Authorization: `Bearer ${token}` } : {}
      });
      updatedServices[i].status = res.status === 200 ? 'healthy' : 'unhealthy';
    } catch (error) {
      console.log(`Service ${updatedServices[i].name} is unavailable`);
      updatedServices[i].status = 'unhealthy';
    }
  }
  setServices(updatedServices);
};

  // ============================================
  // ORDER MANAGEMENT
  // ============================================

  // Récupérer les commandes
  const fetchOrders = async () => {
    if (!currentUser) return;
    setOrderLoading(true);
    try {
      const res = await axios.get(`${API_GATEWAY}/order/orders/user/${currentUser.id}`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      setOrders(res.data || []);
    } catch (error) {
      console.error('Erreur fetch orders:', error);
      setOrders([]);
    }
    setOrderLoading(false);
  };

  // Récupérer une commande spécifique
// Récupérer une commande spécifique - Version corrigée
const fetchOrderDetails = async (orderId) => {
  if (!orderId) return;
  try {
    const res = await axios.get(`${API_GATEWAY}/order/orders/${orderId}`, {
      params: { userId: currentUser.id },
      headers: { Authorization: `Bearer ${token}` }
    });
    if (res.data) {
      setSelectedOrder(res.data);
    }
  } catch (error) {
    console.error('Erreur fetch order details:', error);
  }
};

  // Récupérer les statistiques
  const fetchOrderStats = async () => {
    if (!currentUser) return;
    try {
      const endDate = new Date();
      const startDate = new Date();
      startDate.setDate(startDate.getDate() - 30);
      
      const res = await axios.get(`${API_GATEWAY}/order/orders/statistics`, {
        params: {
          startDate: startDate.toISOString(),
          endDate: endDate.toISOString()
        },
        headers: { Authorization: `Bearer ${token}` }
      });
      setOrderStats(res.data);
    } catch (error) {
      console.error('Erreur fetch stats:', error);
    }
  };

  // Calculer les totaux du panier
  const getCartTotals = () => {
    const subtotal = cart.reduce((sum, item) => sum + (item.price * item.quantity), 0);
    const tax = subtotal * 0.20;
    const shipping = subtotal > 50 ? 0 : 5.99;
    const total = subtotal + tax + shipping - discount;
    return { subtotal, tax, shipping, total };
  };

  // Ajouter au panier
  const addToCart = (product) => {
    const existingItem = cart.find(item => item.id === product.id);
    if (existingItem) {
      setCart(cart.map(item =>
        item.id === product.id
          ? { ...item, quantity: item.quantity + 1 }
          : item
      ));
    } else {
      setCart([...cart, { ...product, quantity: 1 }]);
    }
    alert(`✅ ${product.name} ajouté au panier`);
  };

  // Retirer du panier
  const removeFromCart = (productId) => {
    setCart(cart.filter(item => item.id !== productId));
  };

  // Mettre à jour la quantité
  const updateQuantity = (productId, quantity) => {
    if (quantity <= 0) {
      removeFromCart(productId);
    } else {
      setCart(cart.map(item =>
        item.id === productId ? { ...item, quantity } : item
      ));
    }
  };

  // Appliquer un coupon
  const applyCoupon = () => {
    if (couponCode === 'WELCOME10') {
      const { subtotal } = getCartTotals();
      setDiscount(subtotal * 0.10);
      alert('✅ Code promo WELCOME10 appliqué (-10%)');
    } else if (couponCode === 'SAVE20') {
      const { subtotal } = getCartTotals();
      setDiscount(subtotal * 0.20);
      alert('✅ Code promo SAVE20 appliqué (-20%)');
    } else if (couponCode) {
      alert('❌ Code promo invalide');
    }
  };

  // Créer une commande
  const createOrder = async () => {
    if (cart.length === 0) {
      alert('Votre panier est vide');
      return;
    }
    
    if (!shippingAddress.street || !shippingAddress.city) {
      alert('Veuillez entrer une adresse de livraison complète');
      return;
    }
    
    setOrderLoading(true);
    try {
      const orderData = {
        userId: currentUser.id,
        email: currentUser.email,
        items: cart.map(item => ({
          productId: item.id.toString(),
          productName: item.name,
          quantity: item.quantity,
          unitPrice: item.price
        })),
        shippingAddress: `${shippingAddress.street}, ${shippingAddress.postalCode} ${shippingAddress.city}, ${shippingAddress.country}`,
        phoneNumber: phoneNumber || '',
        paymentMethod: paymentMethod,
        couponCode: couponCode || null
      };
      
      const res = await axios.post(`${API_GATEWAY}/order/orders`, orderData, {
        headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' }
      });
      
      alert(`✅ Commande #${res.data.orderNumber || res.data.id.slice(-8)} créée avec succès!`);
      setCart([]);
      setCouponCode('');
      setDiscount(0);
      setShippingAddress({ street: '', city: '', postalCode: '', country: 'France' });
      setPhoneNumber('');
      await fetchOrders();
      await fetchOrderStats();
      setActiveTab('orders');
    } catch (error) {
      console.error('Erreur création commande:', error);
      alert(`❌ Erreur: ${error.response?.data?.message || error.message}`);
    }
    setOrderLoading(false);
  };

  // Filtrer les commandes par statut
// Filtrer les commandes par statut - Version corrigée
const getFilteredOrders = () => {
  if (!orders || orders.length === 0) return [];
  if (selectedStatus === 'all') return orders;
  return orders.filter(order => order && order.status === selectedStatus);
};

  // Obtenir la classe CSS pour le statut
 // Obtenir la classe CSS pour le statut - Version corrigée
const getStatusClass = (status) => {
  if (!status) return 'status-pending';
  const statusMap = {
    'PENDING': 'status-pending',
    'CONFIRMED': 'status-confirmed',
    'PROCESSING': 'status-processing',
    'SHIPPED': 'status-shipped',
    'DELIVERED': 'status-delivered',
    'CANCELLED': 'status-cancelled'
  };
  return statusMap[status] || 'status-pending';
};

// Obtenir le libellé du statut en français - Version corrigée
const getStatusLabel = (status) => {
  if (!status) return 'En attente';
  const statusMap = {
    'PENDING': 'En attente',
    'CONFIRMED': 'Confirmée',
    'PROCESSING': 'En traitement',
    'SHIPPED': 'Expédiée',
    'DELIVERED': 'Livrée',
    'CANCELLED': 'Annulée'
  };
  return statusMap[status] || status;
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

  // Rechercher des produits
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
      const url = queryString 
        ? `${API_GATEWAY}/product/products/search?${queryString}`
        : `${API_GATEWAY}/product/products`;
      
      const res = await axios.get(url, {
        headers: token ? { Authorization: `Bearer ${token}` } : {}
      });
      
      setProducts(res.data.products || []);
    } catch (error) {
      console.error('❌ Erreur recherche:', error);
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
      await fetchOrders();
      await fetchOrderStats();
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
    setOrders([]);
    setCart([]);
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
  const statusOptions = ['PENDING', 'CONFIRMED', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED'];

  const { subtotal, tax, shipping, total } = getCartTotals();

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
          <button className={activeTab === 'cart' ? 'active' : ''} onClick={() => setActiveTab('cart')}>
            🛒 Cart ({cart.length})
          </button>
          <button className={activeTab === 'orders' ? 'active' : ''} onClick={() => { setActiveTab('orders'); fetchOrders(); }}>
            📋 Orders ({orders.length})
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

            {/* Statistiques des commandes */}
            {orderStats && orderStats.totalOrders !== undefined && (
              <div className="order-stats">
                <h2>📊 Statistiques (30 derniers jours)</h2>
                <div className="stats-grid">
                  <div className="stat-card">
                    <div className="stat-value">{orderStats.totalRevenue?.toFixed(2) || 0} €</div>
                    <div className="stat-label">Chiffre d'affaires</div>
                  </div>
                  <div className="stat-card">
                    <div className="stat-value">{orderStats.totalOrders || 0}</div>
                    <div className="stat-label">Commandes</div>
                  </div>
                  <div className="stat-card">
                    <div className="stat-value">{orderStats.averageOrderValue?.toFixed(2) || 0} €</div>
                    <div className="stat-label">Panier moyen</div>
                  </div>
                </div>
              </div>
            )}
          </div>
        )}

        {/* Products Management */}
        {activeTab === 'products' && isAuthenticated && (
          <div className="products-section">
            <div className="products-header">
              <h2>📦 Catalogue Produits</h2>
              <button className="btn-primary" onClick={() => { setShowProductForm(true); setEditingProduct(null); setProductForm({ name: '', description: '', price: '', stock: '', category: '' }); }}>
                + Nouveau Produit
              </button>
            </div>

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
                          <button onClick={() => addToCart(product)}>🛒 Ajouter au panier</button>
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

        {/* Cart */}
        {activeTab === 'cart' && isAuthenticated && (
          <div className="cart-section">
            <h2>🛒 Mon Panier</h2>
            {cart.length === 0 ? (
              <div className="empty-cart">
                <p>Votre panier est vide</p>
                <button onClick={() => setActiveTab('products')}>🛍️ Voir les produits</button>
              </div>
            ) : (
              <>
                <div className="cart-items">
                  {cart.map((item) => (
                    <div key={item.id} className="cart-item">
                      <div className="cart-item-info">
                        <h3>{item.name}</h3>
                        <p>{item.price} €</p>
                      </div>
                      <div className="cart-item-quantity">
                        <button onClick={() => updateQuantity(item.id, item.quantity - 1)}>-</button>
                        <span>{item.quantity}</span>
                        <button onClick={() => updateQuantity(item.id, item.quantity + 1)}>+</button>
                      </div>
                      <div className="cart-item-total">
                        {(item.price * item.quantity).toFixed(2)} €
                      </div>
                      <button onClick={() => removeFromCart(item.id)}>🗑️</button>
                    </div>
                  ))}
                </div>
                <div className="cart-summary">
                  <div className="cart-totals">
                    <div>Sous-total: <span>{subtotal.toFixed(2)} €</span></div>
                    <div>TVA (20%): <span>{tax.toFixed(2)} €</span></div>
                    <div>Livraison: <span>{shipping === 0 ? 'Gratuite' : shipping.toFixed(2) + ' €'}</span></div>
                    {discount > 0 && <div>Réduction: <span>-{discount.toFixed(2)} €</span></div>}
                    <div className="total">Total: <span>{total.toFixed(2)} €</span></div>
                  </div>
                  
                  <div className="shipping-info">
                    <input
                      type="text"
                      placeholder="Adresse *"
                      value={shippingAddress.street}
                      onChange={(e) => setShippingAddress({ ...shippingAddress, street: e.target.value })}
                    />
                    <input
                      type="text"
                      placeholder="Ville *"
                      value={shippingAddress.city}
                      onChange={(e) => setShippingAddress({ ...shippingAddress, city: e.target.value })}
                    />
                    <input
                      type="text"
                      placeholder="Code postal *"
                      value={shippingAddress.postalCode}
                      onChange={(e) => setShippingAddress({ ...shippingAddress, postalCode: e.target.value })}
                    />
                    <input
                      type="tel"
                      placeholder="Téléphone"
                      value={phoneNumber}
                      onChange={(e) => setPhoneNumber(e.target.value)}
                    />
                  </div>
                  
                  <div className="coupon-section">
                    <input
                      type="text"
                      placeholder="Code promo"
                      value={couponCode}
                      onChange={(e) => setCouponCode(e.target.value)}
                    />
                    <button onClick={applyCoupon}>Appliquer</button>
                  </div>
                  
                  <div className="payment-methods">
                    <label>
                      <input type="radio" value="card" checked={paymentMethod === 'card'} onChange={(e) => setPaymentMethod(e.target.value)} />
                      💳 Carte bancaire
                    </label>
                    <label>
                      <input type="radio" value="paypal" checked={paymentMethod === 'paypal'} onChange={(e) => setPaymentMethod(e.target.value)} />
                      📱 PayPal
                    </label>
                  </div>
                  
                  <button className="btn-primary" onClick={createOrder} disabled={orderLoading}>
                    {orderLoading ? 'Création...' : '✅ Passer la commande'}
                  </button>
                </div>
              </>
            )}
          </div>
        )}

        {/* Orders */}
// Modifie la section d'affichage des commandes dans App.js

{/* Orders */}
{activeTab === 'orders' && isAuthenticated && (
  <div className="orders-section">
    <div className="orders-header">
      <h2>📋 Mes Commandes</h2>
      <select value={selectedStatus} onChange={(e) => setSelectedStatus(e.target.value)}>
        <option value="all">Tous les statuts</option>
        {statusOptions.map(status => <option key={status} value={status}>{status}</option>)}
      </select>
    </div>
    
    {orderLoading ? (
      <div className="loading">Chargement des commandes...</div>
    ) : (
      <div className="orders-list">
        {!getFilteredOrders() || getFilteredOrders().length === 0 ? (
          <div className="no-orders">Aucune commande trouvée</div>
        ) : (
          getFilteredOrders().map((order) => (
            <div key={order?.id || Math.random()} className="order-card">
              <div className="order-header">
                <div>
                  <strong>Commande #{order?.orderNumber || order?.id?.slice(-8) || 'N/A'}</strong>
                  <span className={`order-status ${getStatusClass(order?.status)}`}>
                    {getStatusLabel(order?.status)}
                  </span>
                </div>
                <div className="order-date">
                  {order?.createdAt ? new Date(order.createdAt).toLocaleDateString('fr-FR') : 'Date inconnue'}
                </div>
              </div>
              <div className="order-items">
                {order?.items && order.items.length > 0 ? (
                  order.items.map((item, idx) => (
                    <div key={idx} className="order-item">
                      <span>{item?.productName || 'Produit'} x {item?.quantity || 0}</span>
                      <span>{item?.subtotal?.toFixed(2) || '0.00'} €</span>
                    </div>
                  ))
                ) : (
                  <div className="order-item">Aucun article</div>
                )}
              </div>
              <div className="order-footer">
                <div className="order-total">
                  <strong>Total: {order?.totalAmount?.toFixed(2) || '0.00'} €</strong>
                </div>
                {order?.shippingAddress && (
                  <div className="order-shipping">
                    📍 Livraison: {order.shippingAddress}
                  </div>
                )}
                <button 
                  className="order-details-btn"
                  onClick={() => order?.id && fetchOrderDetails(order.id)}
                >
                  📄 Voir détails
                </button>
              </div>
            </div>
          ))
        )}
      </div>
    )}

    {/* Modal Détails Commande */}
    {selectedOrder && (
      <div className="modal" onClick={() => setSelectedOrder(null)}>
        <div className="modal-content" onClick={(e) => e.stopPropagation()}>
          <div className="modal-header">
            <h3>Détails de la commande</h3>
            <button className="close-btn" onClick={() => setSelectedOrder(null)}>✕</button>
          </div>
          <div className="modal-body">
            <p><strong>Numéro:</strong> {selectedOrder?.orderNumber || 'N/A'}</p>
            <p><strong>Date:</strong> {selectedOrder?.createdAt ? new Date(selectedOrder.createdAt).toLocaleString('fr-FR') : 'Date inconnue'}</p>
            <p><strong>Statut:</strong> <span className={`order-status ${getStatusClass(selectedOrder?.status)}`}>{getStatusLabel(selectedOrder?.status)}</span></p>
            <p><strong>Adresse de livraison:</strong> {selectedOrder?.shippingAddress || 'Non renseignée'}</p>
            <p><strong>Téléphone:</strong> {selectedOrder?.phoneNumber || 'Non renseigné'}</p>
            <hr />
            <h4>Articles commandés</h4>
            {selectedOrder?.items && selectedOrder.items.length > 0 ? (
              selectedOrder.items.map((item, idx) => (
                <div key={idx} className="order-detail-item">
                  <span>{item?.productName || 'Produit'}</span>
                  <span>{item?.quantity || 0} x {item?.unitPrice?.toFixed(2) || '0.00'} €</span>
                  <span><strong>{item?.subtotal?.toFixed(2) || '0.00'} €</strong></span>
                </div>
              ))
            ) : (
              <div>Aucun article</div>
            )}
            <hr />
            <div className="order-detail-totals">
              <p>Sous-total: {selectedOrder?.subtotal?.toFixed(2) || '0.00'} €</p>
              <p>TVA (20%): {selectedOrder?.taxAmount?.toFixed(2) || '0.00'} €</p>
              <p>Livraison: {selectedOrder?.shippingCost?.toFixed(2) || '0.00'} €</p>
              <p className="total"><strong>Total: {selectedOrder?.totalAmount?.toFixed(2) || '0.00'} €</strong></p>
            </div>
          </div>
        </div>
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
                placeholder="Endpoint (ex: products, health, orders)"
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
        <p>Microservices Enterprise Platform v5.0 - Professional Order Management</p>
      </footer>
    </div>
  );
}

export default App;
