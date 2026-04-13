require('dotenv').config();

module.exports = {
  port: process.env.PORT || 8002,
  database: {
    host: process.env.DB_HOST || 'postgres-product',
    port: process.env.DB_PORT || 5432,
    database: process.env.DB_NAME || 'products_db',
    user: process.env.DB_USER || 'user',
    password: process.env.DB_PASSWORD || 'password',
  },
  redis: {
    url: process.env.REDIS_URL || 'redis://redis:6379',
  },
  elasticsearch: {
    node: process.env.ELASTICSEARCH_URL || 'http://elasticsearch:9200',
  },
  kafka: {
    brokers: (process.env.KAFKA_BOOTSTRAP || 'kafka:9092').split(','),
    clientId: 'product-service',
    groupId: 'product-service-group',
  },
};
