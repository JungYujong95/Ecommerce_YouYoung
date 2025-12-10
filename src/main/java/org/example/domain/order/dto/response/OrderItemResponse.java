package org.example.domain.order.dto.response;

import lombok.Builder;
import lombok.Getter;
import org.example.domain.order.entity.OrderItem;

@Getter
@Builder
public class OrderItemResponse {

    private Long id;
    private Long productId;
    private String productName;
    private Long productPrice;
    private Integer quantity;
    private Long subtotal;

    public static OrderItemResponse from(OrderItem orderItem) {
        return OrderItemResponse.builder()
                .id(orderItem.getId())
                .productId(orderItem.getProductId())
                .productName(orderItem.getProductName())
                .productPrice(orderItem.getProductPrice())
                .quantity(orderItem.getQuantity())
                .subtotal(orderItem.getSubtotal())
                .build();
    }
}
