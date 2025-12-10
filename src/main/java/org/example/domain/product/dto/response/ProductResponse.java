package org.example.domain.product.dto.response;

import lombok.Builder;
import lombok.Getter;
import org.example.domain.product.entity.Product;
import org.example.domain.product.entity.ProductStatus;

import java.time.LocalDateTime;

@Getter
@Builder
public class ProductResponse {

    private Long id;
    private String name;
    private Long price;
    private Integer stockQuantity;
    private ProductStatus status;
    private Long sellerId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ProductResponse from(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .status(product.getStatus())
                .sellerId(product.getSellerId())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
