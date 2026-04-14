const express = require('express');
const cors = require('cors');
const { Pool } = require('pg');

const app = express();
const port = process.env.PORT || 8002;

// CORS
app.use(cors({
  origin: ['http://localhost:3000', 'http://localhost:8000', '*'],
  credentials: true,
  methods: ['GET', 'POST', 'PUT', 'PATCH', 'DELETE', 'OPTIONS'],
  allowedHeaders: ['Content-Type', 'Authorization'],
}));
app.use(express.json());

// PostgreSQL connection
const pool = new Pool({
  host: process.env.DB_HOST || 'postgres-product',
  port: process.env.DB_PORT || 5432,
  database: process.env.DB_NAME || 'products_db',
  user: process.env.DB_USER || 'user',
  password: process.env.DB_PASSWORD || 'password',
});

// Create table
async function initDb() {
  const query = `
    CREATE TABLE IF NOT EXISTS products (
      id SERIAL PRIMARY KEY,
      name VARCHAR(100) NOT NULL,
      description TEXT,
      price DECIMAL(10,2) NOT NULL,
      stock INTEGER NOT NULL DEFAULT 0,
      category VARCHAR(50),
      is_available BOOLEAN DEFAULT TRUE,
      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
      updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );
  `;
  await pool.query(query);
  console.log('✅ Products table ready');
}

// Health check
app.get('/health', (req, res) => {
  console.log('📊 Health check called');
  res.json({ status: 'healthy', service: 'product-service', timestamp: new Date().toISOString() });
});

app.get('/', (req, res) => {
  res.json({ message: 'Product Service is running', version: '1.0.0' });
});

// ============================================
// ⚠️ IMPORTANT: La route search DOIT être AVANT la route avec paramètre :id
// ============================================

// Search products (route fixe - doit être avant /:id)
app.get('/api/products/search', async (req, res) => {
  console.log('🔍 Search called with query:', req.query);
  try {
    const { q, category, min_price, max_price } = req.query;
    let query = 'SELECT * FROM products WHERE 1=1';
    const params = [];
    let idx = 1;
    
    if (q && q !== '*' && q !== '') {
      query += ` AND (name ILIKE $${idx} OR description ILIKE $${idx})`;
      params.push(`%${q}%`);
      idx++;
    }
    if (category && category !== '') {
      query += ` AND category = $${idx}`;
      params.push(category);
      idx++;
    }
    if (min_price && min_price !== '') {
      query += ` AND price >= $${idx}`;
      params.push(parseFloat(min_price));
      idx++;
    }
    if (max_price && max_price !== '') {
      query += ` AND price <= $${idx}`;
      params.push(parseFloat(max_price));
      idx++;
    }
    
    query += ' ORDER BY id LIMIT 50';
    const result = await pool.query(query, params);
    console.log(`✅ Search found ${result.rows.length} products`);
    res.json({ total: result.rows.length, products: result.rows });
  } catch (error) {
    console.error('❌ Search error:', error);
    res.status(500).json({ error: error.message });
  }
});

// ============================================
// Routes avec paramètres (doivent être APRÈS les routes fixes)
// ============================================

// Get all products
app.get('/api/products', async (req, res) => {
  console.log('📋 GET /api/products called');
  try {
    const result = await pool.query('SELECT * FROM products ORDER BY id');
    console.log(`✅ Found ${result.rows.length} products`);
    res.json({ total: result.rows.length, products: result.rows });
  } catch (error) {
    console.error('❌ Error getting products:', error);
    res.status(500).json({ error: error.message });
  }
});

// Get product by ID
app.get('/api/products/:id', async (req, res) => {
  const id = parseInt(req.params.id);
  
  if (isNaN(id)) {
    console.log(`❌ Invalid product ID: ${req.params.id}`);
    return res.status(400).json({ error: 'Invalid product ID' });
  }
  
  console.log(`📋 GET /api/products/${id} called`);
  try {
    const result = await pool.query('SELECT * FROM products WHERE id = $1', [id]);
    if (result.rows.length === 0) {
      console.log(`❌ Product ${id} not found`);
      return res.status(404).json({ error: 'Product not found' });
    }
    console.log(`✅ Product ${id} found`);
    res.json(result.rows[0]);
  } catch (error) {
    console.error('❌ Error:', error);
    res.status(500).json({ error: error.message });
  }
});

// Create product
app.post('/api/products', async (req, res) => {
  console.log('📝 POST /api/products called');
  console.log('📦 Request body:', JSON.stringify(req.body, null, 2));
  
  try {
    const { name, description, price, stock, category } = req.body;
    
    // Validation
    if (!name || price === undefined || price === null) {
      console.log('❌ Validation failed: missing required fields');
      return res.status(400).json({ error: 'Name and price are required' });
    }
    
    const stockValue = stock !== undefined ? stock : 0;
    const is_available = stockValue > 0;
    const result = await pool.query(
      'INSERT INTO products (name, description, price, stock, category, is_available) VALUES ($1, $2, $3, $4, $5, $6) RETURNING *',
      [name, description || '', parseFloat(price), stockValue, category || 'General', is_available]
    );
    
    console.log('✅ Product created successfully:', result.rows[0]);
    res.status(201).json(result.rows[0]);
  } catch (error) {
    console.error('❌ Create error:', error);
    res.status(500).json({ error: error.message });
  }
});

// Update product
app.put('/api/products/:id', async (req, res) => {
  const id = parseInt(req.params.id);
  
  if (isNaN(id)) {
    return res.status(400).json({ error: 'Invalid product ID' });
  }
  
  console.log(`✏️ PUT /api/products/${id} called`);
  console.log('📦 Request body:', JSON.stringify(req.body, null, 2));
  
  try {
    const { name, description, price, stock, category } = req.body;
    const stockValue = stock !== undefined ? stock : 0;
    const is_available = stockValue > 0;
    
    const result = await pool.query(
      'UPDATE products SET name=$1, description=$2, price=$3, stock=$4, category=$5, is_available=$6, updated_at=CURRENT_TIMESTAMP WHERE id=$7 RETURNING *',
      [name, description || '', parseFloat(price), stockValue, category || 'General', is_available, id]
    );
    
    if (result.rows.length === 0) {
      console.log(`❌ Product ${id} not found for update`);
      return res.status(404).json({ error: 'Product not found' });
    }
    console.log('✅ Product updated:', result.rows[0]);
    res.json(result.rows[0]);
  } catch (error) {
    console.error('❌ Update error:', error);
    res.status(500).json({ error: error.message });
  }
});

// Update stock
app.patch('/api/products/:id/stock', async (req, res) => {
  const id = parseInt(req.params.id);
  
  if (isNaN(id)) {
    return res.status(400).json({ error: 'Invalid product ID' });
  }
  
  console.log(`📊 PATCH /api/products/${id}/stock called`);
  console.log('📦 Request body:', JSON.stringify(req.body, null, 2));
  
  try {
    const { quantity, operation } = req.body;
    
    if (!quantity || !operation) {
      return res.status(400).json({ error: 'Quantity and operation are required' });
    }
    
    const current = await pool.query('SELECT stock FROM products WHERE id = $1', [id]);
    if (current.rows.length === 0) {
      console.log(`❌ Product ${id} not found for stock update`);
      return res.status(404).json({ error: 'Product not found' });
    }
    
    let newStock = current.rows[0].stock;
    if (operation === 'decrement') {
      if (newStock < quantity) {
        console.log(`❌ Insufficient stock: ${newStock} < ${quantity}`);
        return res.status(400).json({ error: 'Insufficient stock' });
      }
      newStock -= quantity;
    } else if (operation === 'increment') {
      newStock += quantity;
    } else {
      console.log(`❌ Invalid operation: ${operation}`);
      return res.status(400).json({ error: 'Invalid operation. Use "increment" or "decrement"' });
    }
    
    const is_available = newStock > 0;
    await pool.query(
      'UPDATE products SET stock=$1, is_available=$2, updated_at=CURRENT_TIMESTAMP WHERE id=$3',
      [newStock, is_available, id]
    );
    
    console.log(`✅ Stock updated: ${current.rows[0].stock} -> ${newStock}`);
    res.json({ product_id: id, new_stock: newStock });
  } catch (error) {
    console.error('❌ Stock update error:', error);
    res.status(500).json({ error: error.message });
  }
});

// Delete product
app.delete('/api/products/:id', async (req, res) => {
  const id = parseInt(req.params.id);
  
  if (isNaN(id)) {
    return res.status(400).json({ error: 'Invalid product ID' });
  }
  
  console.log(`🗑️ DELETE /api/products/${id} called`);
  
  try {
    const result = await pool.query('DELETE FROM products WHERE id = $1 RETURNING *', [id]);
    if (result.rows.length === 0) {
      console.log(`❌ Product ${id} not found for deletion`);
      return res.status(404).json({ error: 'Product not found' });
    }
    console.log(`✅ Product ${id} deleted`);
    res.json({ message: 'Product deleted successfully' });
  } catch (error) {
    console.error('❌ Delete error:', error);
    res.status(500).json({ error: error.message });
  }
});

// Start server
async function start() {
  await initDb();
  app.listen(port, () => {
    console.log(`✅ Product Service running on port ${port}`);
  });
}

start();
