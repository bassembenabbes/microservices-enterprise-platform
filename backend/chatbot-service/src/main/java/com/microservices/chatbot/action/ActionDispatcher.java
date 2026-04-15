package com.microservices.chatbot.action;

import com.microservices.chatbot.client.OrderServiceClient;
import com.microservices.chatbot.client.ProductServiceClient;
import com.microservices.chatbot.client.UserServiceClient;
import com.microservices.chatbot.dto.ContractRequest;
import com.microservices.chatbot.dto.ContractResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ActionDispatcher {
    
    private final UserServiceClient userServiceClient;
    private final ProductServiceClient productServiceClient;
    private final OrderServiceClient orderServiceClient;
    
    public Map<String, Object> dispatch(String action, Map<String, Object> parameters) {
        log.info("🚀 Dispatch action: {} avec paramètres: {}", action, parameters);
        
        return switch (action) {
            case "GET_USER_INFO" -> getUserInfo(parameters);
            case "GET_PRODUCT_INFO" -> getProductInfo(parameters);
            case "SEARCH_PRODUCTS" -> searchProducts(parameters);
            case "CREATE_ORDER" -> createOrder(parameters);
            case "GET_ORDER_STATUS" -> getOrderStatus(parameters);
            case "GET_USER_ORDERS" -> getUserOrders(parameters);
            case "CHECK_STOCK" -> checkStock(parameters);
            default -> Map.of("error", "Action non reconnue: " + action);
        };
    }
    
    private Map<String, Object> getUserInfo(Map<String, Object> params) {
        String userId = (String) params.get("userId");
        return userServiceClient.getUserInfo(userId);
    }
    
    private Map<String, Object> getProductInfo(Map<String, Object> params) {
        String productId = (String) params.get("productId");
        return productServiceClient.getProductInfo(productId);
    }
    
    private Map<String, Object> searchProducts(Map<String, Object> params) {
        String query = (String) params.get("query");
        String category = (String) params.get("category");
        
        List<Map<String, Object>> products = productServiceClient.searchProducts(query, category);
        
        Map<String, Object> result = new HashMap<>();
        result.put("products", products);
        result.put("count", products.size());
        return result;
    }
    
    private Map<String, Object> createOrder(Map<String, Object> params) {
        Map<String, Object> orderData = new HashMap<>();
        orderData.put("userId", params.get("userId"));
        orderData.put("email", params.get("email"));
        orderData.put("items", params.get("items"));
        orderData.put("shippingAddress", params.get("shippingAddress"));
        orderData.put("phoneNumber", params.get("phoneNumber"));
        orderData.put("paymentMethod", params.getOrDefault("paymentMethod", "card"));
        orderData.put("couponCode", params.get("couponCode"));
        
        return orderServiceClient.createOrder(orderData);
    }
    
    private Map<String, Object> getOrderStatus(Map<String, Object> params) {
        String orderId = (String) params.get("orderId");
        String userId = (String) params.get("userId");
        return orderServiceClient.getOrderStatus(orderId, userId);
    }
    
    private Map<String, Object> getUserOrders(Map<String, Object> params) {
        String userId = (String) params.get("userId");
        List<Map<String, Object>> orders = orderServiceClient.getUserOrders(userId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("orders", orders);
        result.put("count", orders.size());
        return result;
    }
    
    private Map<String, Object> checkStock(Map<String, Object> params) {
        String productId = (String) params.get("productId");
        int stock = productServiceClient.checkStock(productId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("productId", productId);
        result.put("stock", stock);
        result.put("available", stock > 0);
        return result;
    }
}
