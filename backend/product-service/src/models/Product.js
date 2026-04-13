const { Pool } = require('pg');
const config = require('../config');

const pool = new Pool(config.database);

class Product {
  static async createTable() {
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
      
      CREATE INDEX IF NOT EXISTS idx_products_name ON products(name);
      CREATE INDEX IF NOT EXISTS idx_products_category ON products(category);
    `;
    await pool.query(query);
    console.log('✅ Products table ready');
  }

  static async create(productData) {
    const { name, description, price, stock, category } = productData;
    const query = `
      INSERT INTO products (name, description, price, stock, category, is_available)
      VALUES ($1, $2, $3, $4, $5, $6)
      RETURNING *
    `;
    const is_available = stock > 0;
    const values = [name, description, price, stock, category, is_available];
    const result = await pool.query(query, values);
    return result.rows[0];
  }

  static async findById(id) {
    const query = 'SELECT * FROM products WHERE id = $1';
    const result = await pool.query(query, [id]);
    return result.rows[0];
  }

  static async findAll(limit = 100, offset = 0) {
    const query = 'SELECT * FROM products ORDER BY id LIMIT $1 OFFSET $2';
    const result = await pool.query(query, [limit, offset]);
    return result.rows;
  }

  static async update(id, updates) {
    const fields = [];
    const values = [];
    let idx = 1;
    
    for (const [key, value] of Object.entries(updates)) {
      if (value !== undefined) {
        fields.push(`${key} = $${idx}`);
        values.push(value);
        idx++;
      }
    }
    fields.push(`updated_at = CURRENT_TIMESTAMP`);
    
    const query = `UPDATE products SET ${fields.join(', ')} WHERE id = $${idx} RETURNING *`;
    values.push(id);
    
    const result = await pool.query(query, values);
    return result.rows[0];
  }

  static async updateStock(id, quantity, operation) {
    const product = await this.findById(id);
    if (!product) return null;
    
    let newStock = product.stock;
    if (operation === 'decrement') {
      if (product.stock < quantity) return { error: 'Insufficient stock' };
      newStock = product.stock - quantity;
    } else if (operation === 'increment') {
      newStock = product.stock + quantity;
    } else {
      return { error: 'Invalid operation' };
    }
    
    const query = `
      UPDATE products 
      SET stock = $1, is_available = $2, updated_at = CURRENT_TIMESTAMP 
      WHERE id = $3 
      RETURNING *
    `;
    const is_available = newStock > 0;
    const result = await pool.query(query, [newStock, is_available, id]);
    return result.rows[0];
  }

  static async delete(id) {
    const query = 'DELETE FROM products WHERE id = $1 RETURNING *';
    const result = await pool.query(query, [id]);
    return result.rows[0];
  }

  static async search(filters) {
    const conditions = [];
    const values = [];
    let idx = 1;
    
    if (filters.name) {
      conditions.push(`name ILIKE $${idx}`);
      values.push(`%${filters.name}%`);
      idx++;
    }
    if (filters.category) {
      conditions.push(`category = $${idx}`);
      values.push(filters.category);
      idx++;
    }
    if (filters.minPrice) {
      conditions.push(`price >= $${idx}`);
      values.push(filters.minPrice);
      idx++;
    }
    if (filters.maxPrice) {
      conditions.push(`price <= $${idx}`);
      values.push(filters.maxPrice);
      idx++;
    }
    if (filters.is_available !== undefined) {
      conditions.push(`is_available = $${idx}`);
      values.push(filters.is_available);
      idx++;
    }
    
    const whereClause = conditions.length > 0 ? `WHERE ${conditions.join(' AND ')}` : '';
    const query = `SELECT * FROM products ${whereClause} ORDER BY id LIMIT $${idx} OFFSET $${idx + 1}`;
    values.push(filters.limit || 100, filters.offset || 0);
    
    const result = await pool.query(query, values);
    return result.rows;
  }
}

module.exports = Product;
