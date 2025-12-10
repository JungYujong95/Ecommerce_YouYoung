package org.example.domain.product.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.global.common.BaseTimeEntity;
import org.example.global.exception.BusinessException;
import org.example.global.exception.ErrorCode;

/**
 * 상품 엔티티
 * <p>
 * 상품 정보를 관리하는 엔티티입니다.
 * </p>
 */
@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseTimeEntity {

    /** 상품 고유 식별자 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 상품명 */
    @Column(nullable = false, length = 200)
    private String name;

    /** 판매 가격 */
    @Column(nullable = false)
    private Long price;

    /** 재고 수량 */
    @Column(nullable = false)
    private Integer stockQuantity;

    /** 상품 상태 (SELLING, SOLD_OUT, STOPPED) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductStatus status;

    /** 판매자 ID */
    @Column(nullable = false)
    private Long sellerId;

    @Builder
    public Product(String name, Long price, Integer stockQuantity, Long sellerId) {
        this.name = name;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.status = ProductStatus.SELLING;
        this.sellerId = sellerId;
    }

    /**
     * 상품 정보 수정
     *
     * @param name          변경할 상품명 (null이면 변경하지 않음)
     * @param price         변경할 가격 (null이면 변경하지 않음)
     * @param stockQuantity 변경할 재고 수량 (null이면 변경하지 않음)
     */
    public void updateInfo(String name, Long price, Integer stockQuantity) {
        if (name != null) {
            this.name = name;
        }
        if (price != null) {
            this.price = price;
        }
        if (stockQuantity != null) {
            this.stockQuantity = stockQuantity;
            updateStatusByStock();
        }
    }

    /**
     * 상품 상태 수정
     *
     * @param status 변경할 상태
     */
    public void updateStatus(ProductStatus status) {
        this.status = status;
    }

    /**
     * 재고 차감
     * <p>
     * 재고가 부족하면 예외가 발생합니다.
     * 재고가 0이 되면 상태가 SOLD_OUT으로 변경됩니다.
     * </p>
     *
     * @param quantity 차감할 수량
     * @throws BusinessException INSUFFICIENT_STOCK - 재고가 부족할 경우
     */
    public void decreaseStock(int quantity) {
        if (this.stockQuantity < quantity) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);
        }
        this.stockQuantity -= quantity;
        updateStatusByStock();
    }

    /**
     * 재고 증가
     * <p>
     * 주문 취소 시 재고를 복구할 때 사용됩니다.
     * 재고가 0에서 증가하면 상태가 SELLING으로 변경됩니다.
     * </p>
     *
     * @param quantity 증가할 수량
     */
    public void increaseStock(int quantity) {
        this.stockQuantity += quantity;
        updateStatusByStock();
    }

    /**
     * 재고에 따른 상태 자동 변경
     */
    private void updateStatusByStock() {
        if (this.stockQuantity == 0 && this.status == ProductStatus.SELLING) {
            this.status = ProductStatus.SOLD_OUT;
        } else if (this.stockQuantity > 0 && this.status == ProductStatus.SOLD_OUT) {
            this.status = ProductStatus.SELLING;
        }
    }
}
