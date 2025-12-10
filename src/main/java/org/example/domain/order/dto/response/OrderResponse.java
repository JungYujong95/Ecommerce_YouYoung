package org.example.domain.order.dto.response;

import lombok.Builder;
import lombok.Getter;
import org.example.domain.order.entity.Order;
import org.example.domain.order.entity.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class OrderResponse {

    private Long id;
    private Long buyerId;
    private OrderStatus status;
    private Long totalPrice;
    private List<OrderItemResponse> orderItems;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static OrderResponse from(Order order) {
        List<OrderItemResponse> items = order.getOrderItems().stream()
                .map(OrderItemResponse::from)
                .toList();

        return OrderResponse.builder()
                .id(order.getId())
                .buyerId(order.getBuyerId())
                .status(order.getStatus())
                .totalPrice(order.getTotalPrice())
                .orderItems(items)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
