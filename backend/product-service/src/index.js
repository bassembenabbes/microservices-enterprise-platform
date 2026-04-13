const express = require('express');
const cors = require('cors');
const Product = require('./models/Product');
const cacheService = require('./services/CacheService');
const searchService = require('./services/SearchService');
const eventService = require('./services/EventService');
const { productCreateSchema, productUpdateSchema, stockUpdateSchema } = require('./validators/productValidator');
const config = require('./config');

const app = express();

// Middleware
app.use(cors({
  origin: ['http://localhost:3000', 'http://localhost:8000'],
  credentials: true,
  methods: ['GET', 'POST', 'PUT', 'PATCH', 'DELETE', 'OPTIONS'],
  allowedHeaders: ['Content-Type', 'Authorization'],
}));
app.use(express.json());

// Health check
app.get('/health', (req, res) => {
  res.json({ status: 'healthy', service: 'product-service', timestamp: new Date().toISOString() });
});

// Routes
app.get('/', (req, res) => {
  res.json({ message: 'Product Service is running', version: '1.0.0' });
});

// Create product
app.post('/api/products', async (req, res) => {
  try {
    const { error, value } = productCreateSchema.validate(req.body);
    if (error) {
      return res.status(400).json({ error: error.details[0].message });
    }
    
    const product = await Product.create(value);
    
    // Index in Elasticsearch
    await searchService.indexProduct(product);
    
    // Send Kafka event
    await eventService.sendEvent('product_created', product);
    
    // Cache in Redis
    await cacheService.set(`product:${product.id}`, product);
    
    res.status(201).json(product);
  } catch (error) {
    console.error('Create product error:', error);
    res.status(500).json({ error: error.message });
  }
});

// Get product by ID
app.get('/api/products/:id', async (req, res) => {
  try {
    const id = parseInt(req.params.id);
    
    // Try cache first
    const cached = await cacheService.get(`product:${id}`);
    if (cached) {
      return res.json(cached);
    }
    
    const product = await Product.findById(id);
    if (!product) {
      return res.status(404).json({ error: 'Product not found' });
    }
    
    // Cache for next time
    await cacheService.set(`product:${id}`, product);
    
    res.json(product);
  } catch (error) {
    console.error('Get product error:', error);
    res.status(500).json({ error: error.message });
  }
});

// Get all products
app.get('/api/products', async (req, res) => {
  try {
    const limit = parseInt(req.query.limit) || 100;
    const offset = parseInt(req.query.offset) || 0;
    const products = await Product.findAll(limit, offset);
    res.json({ total: products.length, products });
  } catch (error) {
    console.error('Get products error:', error);
    res.status(500).json({ error: error.message });
  }
});

// Search products
app.get('/api/products/search', async (req, res) => {
  try {
    const { q, category, min_price, max_price, limit, offset } = req.query;
    
    const result = await searchService.search(
      q, category, min_price, max_price,
      parseInt(limit) || 20,
      parseInt(offset) || 0
    );
    
    res.json(result);
  } catch (error) {
    console.error('Search error:', error);
    res.status(500).json({ error: error.message });
  }
});

// Update product
app.put('/api/products/:id', async (req, res) => {
  try {
    const id = parseInt(req.params.id);
    const { error, value } = productUpdateSchema.validate(req.body);
    if (error) {
      return res.status(400).json({ error: error.details[0].message });
    }
    
    const product = await Product.update(id, value);
    if (!product) {
      return res.status(404).json({ error: 'Product not found' });
    }
    
    // Update Elasticsearch
    await searchService.indexProduct(product);
    
    // Invalidate cache
    await cacheService.del(`product:${id}`);
    
    // Send Kafka event
    await eventService.sendEvent('product_updated', product);
    
    res.json(product);
  } catch (error) {
    console.error('Update product error:', error);
    res.status(500).json({ error: error.message });
  }
});

// Update stock
app.patch('/api/products/:id/stock', async (req, res) => {
  try {
    const id = parseInt(req.params.id);
    const { error, value } = stockUpdateSchema.validate(req.body);
    if (error) {
      return res.status(400).json({ error: error.details[0].message });
    }
    
    const result = await Product.updateStock(id, value.quantity, value.operation);
    if (!result) {
      return res.status(404).json({ error: 'Product not found' });
    }
    if (result.error) {
      return res.status(400).json({ error: result.error });
    }
    
    // Update Elasticsearch
    await searchService.indexProduct(result);
    
    // Invalidate cache
    await cacheService.del(`product:${id}`);
    
    // Send Kafka event
    await eventService.sendEvent('stock_updated', { id, new_stock: result.stock });
    
    res.json({ product_id: id, new_stock: result.stock });
  } catch (error) {
    console.error('Update stock error:', error);
    res.status(500).json({ error: error.message });
  }
});

// Delete product
app.delete('/api/products/:id', async (req, res) => {
  try {
    const id = parseInt(req.params.id);
    const product = await Product.delete(id);
    if (!product) {
      return res.status(404).json({ error: 'Product not found' });
    }
    
    // Delete from Elasticsearch
    await searchService.deleteProduct(id);
    
    // Invalidate cache
    await cacheService.del(`product:${id}`);
    
    // Send Kafka event
    await eventService.sendEvent('product_deleted', { id });
    
    res.json({ message: 'Product deleted successfully' });
  } catch (error) {
    console.error('Delete product error:', error);
    res.status(500).json({ error: error.message });
  }
});

// Initialize services and start server
async function start() {
  try {
    // Create database table
    await Product.createTable();
    
    // Connect to Redis
    await cacheService.connect();
    
    // Connect to Elasticsearch
    await searchService.connect();
    
    // Connect to Kafka
    await eventService.connect();
    
    // Start server
    app.listen(config.port, () => {
      console.log(`✅ Product Service running on port ${config.port}`);
    });
  } catch (error) {
    console.error('Failed to start service:', error);
    process.exit(1);
  }
}

// Graceful shutdown
process.on('SIGTERM', async () => {
  console.log('SIGTERM received, shutting down...');
  await cacheService.disconnect();
  await eventService.disconnect();
  process.exit(0);
});

start();

module.exports = app;
