const { Kafka } = require('kafkajs');
const config = require('../config');

class EventService {
  constructor() {
    this.producer = null;
    this.isConnected = false;
  }

  async connect() {
    try {
      const kafka = new Kafka({
        clientId: config.kafka.clientId,
        brokers: config.kafka.brokers,
      });
      
      this.producer = kafka.producer();
      await this.producer.connect();
      this.isConnected = true;
      console.log('✅ Kafka connected');
    } catch (error) {
      console.error('❌ Kafka connection failed:', error.message);
      this.isConnected = false;
    }
  }

  async sendEvent(eventType, productData) {
    if (!this.isConnected) return;
    try {
      await this.producer.send({
        topic: 'product-events',
        messages: [
          {
            key: productData.id?.toString(),
            value: JSON.stringify({
              type: eventType,
              product: productData,
              timestamp: new Date().toISOString(),
            }),
          },
        ],
      });
    } catch (error) {
      console.error('Kafka send error:', error);
    }
  }

  async disconnect() {
    if (this.producer && this.isConnected) {
      await this.producer.disconnect();
    }
  }
}

module.exports = new EventService();
