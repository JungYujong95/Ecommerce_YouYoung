package org.example.domain.order.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.domain.product.entity.Product;
import org.example.global.common.BaseTimeEntity;

/**
 * 주문 상품 엔티티
 * <p>
 * 주문에 포함된 개별 상품 정보를 관리합니다.
 * 주문 시점의 상품 정보(이름, 가격)를 스냅샷으로 저장합니다.
 * </p>
 */
@Entity
@Table(name = "order_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem extends BaseTimeEntity {

    /** 주문 상품 고유 식별자 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 소속 주문 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    /** 상품 ID */
    @Column(nullable = false)
    private Long productId;

    /** 주문 시점 상품명 (스냅샷) */
    @Column(nullable = false, length = 200)
    private String productName;

    /** 주문 시점 상품 가격 (스냅샷) */
    @Column(nullable = false)
    private Long productPrice;

    /** 주문 수량 */
    @Column(nullable = false)
    private Integer quantity;

    @Builder
    public OrderItem(Long productId, String productName, Long productPrice, Integer quantity) {
        this.productId = productId;
        this.productName = productName;
        this.productPrice = productPrice;
        this.quantity = quantity;
    }

    /**
     * 상품으로부터 주문 상품 생성
     *
     * @param product  주문할 상품
     * @param quantity 주문 수량
     * @return 생성된 주문 상품 (상품 정보 스냅샷 포함)
     */
    public static OrderItem createFromProduct(Product product, int quantity) {
        return OrderItem.builder()
                .productId(product.getId())
                .productName(product.getName())
                .productPrice(product.getPrice())
                .quantity(quantity)
                .build();
    }

    /**
     * 소속 주문 설정 (패키지 내부 사용)
     *
     * @param order 소속 주문
     */
    void setOrder(Order order) {
        this.order = order;
    }

    /**
     * 소계 계산 (단가 × 수량)
     *
     * @return 소계 금액
     */
    public Long getSubtotal() {
        return this.productPrice * this.quantity;
    }
}
