const { Client } = require('@elastic/elasticsearch');
const config = require('../config');

class SearchService {
  constructor() {
    this.client = null;
    this.isConnected = false;
  }

  async connect() {
    try {
      this.client = new Client({ node: config.elasticsearch.node });
      await this.client.info();
      this.isConnected = true;
      console.log('✅ Elasticsearch connected');
    } catch (error) {
      console.error('❌ Elasticsearch connection failed:', error.message);
      this.isConnected = false;
    }
  }

  async indexProduct(product) {
    if (!this.isConnected) return;
    try {
      await this.client.index({
        index: 'products',
        id: product.id.toString(),
        document: {
          id: product.id,
          name: product.name,
          description: product.description,
          price: parseFloat(product.price),
          category: product.category,
          is_available: product.is_available,
          stock: product.stock,
        },
      });
    } catch (error) {
      console.error('Elasticsearch index error:', error);
    }
  }

  async search(query, category, minPrice, maxPrice, limit = 20, offset = 0) {
    if (!this.isConnected) return { total: 0, products: [] };
    
    try {
      const must = [];
      
      if (query) {
        must.push({
          multi_match: {
            query,
            fields: ['name^3', 'description'],
          },
        });
      }
      
      if (category) {
        must.push({ term: { category } });
      }
      
      const filter = [];
      if (minPrice !== undefined || maxPrice !== undefined) {
        const range = {};
        if (minPrice !== undefined) range.gte = minPrice;
        if (maxPrice !== undefined) range.lte = maxPrice;
        filter.push({ range: { price: range } });
      }
      
      const body = {
        query: {
          bool: { must, filter },
        },
        from: offset,
        size: limit,
      };
      
      const result = await this.client.search({ index: 'products', body });
      
      return {
        total: result.hits.total.value,
        products: result.hits.hits.map(h => h._source),
      };
    } catch (error) {
      console.error('Elasticsearch search error:', error);
      return { total: 0, products: [] };
    }
  }

  async deleteProduct(id) {
    if (!this.isConnected) return;
    try {
      await this.client.delete({ index: 'products', id: id.toString() });
    } catch (error) {
      console.error('Elasticsearch delete error:', error);
    }
  }
}

module.exports = new SearchService();
