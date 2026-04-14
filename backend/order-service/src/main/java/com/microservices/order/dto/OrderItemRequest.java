// OrderItemRequest.java
package com.microservices.order.dto;

public class OrderItemRequest {
    private String productId;
    private String productName;
    private Integer quantity;
    private Double unitPrice;
    
    // Getters et Setters
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public Double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(Double unitPrice) { this.unitPrice = unitPrice; }
}
