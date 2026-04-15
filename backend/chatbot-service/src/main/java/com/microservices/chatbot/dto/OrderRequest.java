package com.microservices.chatbot.dto;

import java.util.List;

public class OrderRequest {
    private String userId;
    private String email;
    private List<OrderItemRequest> items;
    private String shippingAddress;
    private String phoneNumber;
    private String paymentMethod;
    private String couponCode;
    
    // Constructors, Getters and Setters
    public OrderRequest() {}
    
    public OrderRequest(String userId, String email, List<OrderItemRequest> items) {
        this.userId = userId;
        this.email = email;
        this.items = items;
    }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public List<OrderItemRequest> getItems() { return items; }
    public void setItems(List<OrderItemRequest> items) { this.items = items; }
    public String getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(String shippingAddress) { this.shippingAddress = shippingAddress; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getCouponCode() { return couponCode; }
    public void setCouponCode(String couponCode) { this.couponCode = couponCode; }
}
