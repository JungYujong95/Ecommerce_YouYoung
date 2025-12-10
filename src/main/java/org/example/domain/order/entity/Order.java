package org.example.domain.order.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.domain.product.entity.Product;
import org.example.global.common.BaseTimeEntity;
import org.example.global.exception.BusinessException;
import org.example.global.exception.ErrorCode;

import java.util.ArrayList;
import java.util.List;

/**
 * 주문 엔티티
 * <p>
 * 주문 정보를 관리하는 엔티티입니다.
 * </p>
 */
@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseTimeEntity {

    /** 주문 고유 식별자 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 구매자 ID */
    @Column(nullable = false)
    private Long buyerId;

    /** 주문 상태 (PENDING, PAID, SHIPPING, DELIVERED, CANCELLED) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    /** 총 주문 금액 */
    @Column(nullable = false)
    private Long totalPrice;

    /** 주문 상품 목록 */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    @Builder
    private Order(Long buyerId) {
        this.buyerId = buyerId;
        this.status = OrderStatus.PENDING;
        this.totalPrice = 0L;
    }

    /**
     * 주문 생성 팩토리 메서드
     *
     * @param buyerId  구매자 ID
     * @param product  주문할 상품
     * @param quantity 주문 수량
     * @return 생성된 주문
     */
    public static Order create(Long buyerId, Product product, int quantity) {
        Order order = Order.builder()
                .buyerId(buyerId)
                .build();

        OrderItem orderItem = OrderItem.createFromProduct(product, quantity);
        order.addOrderItem(orderItem);

        return order;
    }

    /**
     * 주문 상품 추가
     *
     * @param orderItem 추가할 주문 상품
     */
    public void addOrderItem(OrderItem orderItem) {
        this.orderItems.add(orderItem);
        orderItem.setOrder(this);
        calculateTotalPrice();
    }

    /**
     * 총 주문 금액 계산
     */
    private void calculateTotalPrice() {
        this.totalPrice = this.orderItems.stream()
                .mapToLong(OrderItem::getSubtotal)
                .sum();
    }

    /**
     * 주문 취소
     * <p>
     * PENDING, PAID 상태에서만 취소 가능합니다.
     * </p>
     *
     * @throws BusinessException ORDER_ALREADY_CANCELLED - 이미 취소된 주문일 경우
     * @throws BusinessException ORDER_CANNOT_CANCEL - 취소 불가능한 상태일 경우
     */
    public void cancel() {
        if (this.status == OrderStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.ORDER_ALREADY_CANCELLED);
        }
        if (!this.status.isCancellable()) {
            throw new BusinessException(ErrorCode.ORDER_CANNOT_CANCEL);
        }
        this.status = OrderStatus.CANCELLED;
    }
}
