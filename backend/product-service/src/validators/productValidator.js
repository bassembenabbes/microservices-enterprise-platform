const Joi = require('joi');

const productCreateSchema = Joi.object({
  name: Joi.string().min(1).max(100).required(),
  description: Joi.string().allow('', null).optional(),
  price: Joi.number().positive().required(),
  stock: Joi.number().integer().min(0).required(),
  category: Joi.string().required(),
});

const productUpdateSchema = Joi.object({
  name: Joi.string().min(1).max(100).optional(),
  description: Joi.string().allow('', null).optional(),
  price: Joi.number().positive().optional(),
  stock: Joi.number().integer().min(0).optional(),
  category: Joi.string().optional(),
  is_available: Joi.boolean().optional(),
}).min(1);

const stockUpdateSchema = Joi.object({
  quantity: Joi.number().integer().positive().required(),
  operation: Joi.string().valid('increment', 'decrement').required(),
});

module.exports = {
  productCreateSchema,
  productUpdateSchema,
  stockUpdateSchema,
};
