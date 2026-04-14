package com.microservices.order.service;

import com.microservices.order.client.UserClient;
import com.microservices.order.dto.*;
import com.microservices.order.model.*;
import com.microservices.order.repository.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OrderService {
    
    private final OrderRepository orderRepository;
    private final UserClient userClient;
    
    public OrderService(OrderRepository orderRepository, UserClient userClient) {
        this.orderRepository = orderRepository;
        this.userClient = userClient;
    }
    
    private static final String ORDER_NUMBER_PREFIX = "ORD-";
    
    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        log.info("Creating order for user: {}", request.getUserId());
        
        // Vérifier l'utilisateur via User Service (avec Resilience4j)
        boolean userExists = userClient.userExists(request.getUserId());
        if (!userExists) {
            log.warn("User not found in User Service: {}, but continuing", request.getUserId());
        }
        
        // Récupérer les infos utilisateur
        Map<String, Object> userInfo = userClient.getUserInfo(request.getUserId());
        String userEmail = userInfo != null ? (String) userInfo.get("email") : request.getEmail();
        
        double totalAmount = request.getItems().stream()
                .mapToDouble(item -> item.getQuantity() * item.getUnitPrice())
                .sum();
        String orderNumber = ORDER_NUMBER_PREFIX + System.currentTimeMillis();
        
        Order order = new Order();
        order.setOrderNumber(orderNumber);
        order.setUserId(request.getUserId());
        order.setUserEmail(userEmail);
        order.setTotalAmount(totalAmount);
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setDeliveryStatus(DeliveryStatus.PENDING);
        order.setShippingAddress(request.getShippingAddress());
        order.setPhoneNumber(request.getPhoneNumber());
        order.setEmail(request.getEmail());
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        
        for (OrderItemRequest itemReq : request.getItems()) {
            OrderItem item = new OrderItem();
            item.setProductId(itemReq.getProductId());
            item.setProductName(itemReq.getProductName());
            item.setQuantity(itemReq.getQuantity());
            item.setUnitPrice(itemReq.getUnitPrice());
            item.setSubtotal(itemReq.getQuantity() * itemReq.getUnitPrice());
            item.setOrder(order);
            order.getItems().add(item);
        }
        
        Order savedOrder = orderRepository.save(order);
        log.info("Order created with id: {}", savedOrder.getId());
        
        return mapToResponse(savedOrder);
    }
    
    public OrderResponse getOrder(String orderId, String userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized access");
        }
        return mapToResponse(order);
    }
    
    public List<OrderResponse> getUserOrders(String userId) {
        return orderRepository.findByUserId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    private OrderResponse mapToResponse(Order order) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setOrderNumber(order.getOrderNumber());
        response.setUserId(order.getUserId());
        response.setUserEmail(order.getUserEmail());
        response.setTotalAmount(order.getTotalAmount());
        response.setStatus(order.getStatus());
        response.setPaymentStatus(order.getPaymentStatus());
        response.setDeliveryStatus(order.getDeliveryStatus());
        response.setShippingAddress(order.getShippingAddress());
        response.setPhoneNumber(order.getPhoneNumber());
        response.setEmail(order.getEmail());
        response.setCreatedAt(order.getCreatedAt());
        
        List<OrderItemResponse> items = order.getItems().stream().map(item -> {
            OrderItemResponse itemRes = new OrderItemResponse();
            itemRes.setId(item.getId());
            itemRes.setProductId(item.getProductId());
            itemRes.setProductName(item.getProductName());
            itemRes.setQuantity(item.getQuantity());
            itemRes.setUnitPrice(item.getUnitPrice());
            itemRes.setSubtotal(item.getSubtotal());
            return itemRes;
        }).collect(Collectors.toList());
        response.setItems(items);
        
        return response;
    }
}
