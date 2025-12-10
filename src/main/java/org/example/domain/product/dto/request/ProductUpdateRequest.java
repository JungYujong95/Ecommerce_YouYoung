package org.example.domain.product.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductUpdateRequest {

    @Size(max = 200, message = "상품명은 200자 이내여야 합니다")
    private String name;

    @Min(value = 0, message = "가격은 0 이상이어야 합니다")
    private Long price;

    @Min(value = 0, message = "재고수량은 0 이상이어야 합니다")
    private Integer stockQuantity;

    @Builder
    public ProductUpdateRequest(String name, Long price, Integer stockQuantity) {
        this.name = name;
        this.price = price;
        this.stockQuantity = stockQuantity;
    }
}
