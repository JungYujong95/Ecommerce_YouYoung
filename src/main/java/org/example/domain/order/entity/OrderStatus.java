package org.example.domain.order.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 주문 상태
 * <p>
 * 주문의 진행 상태를 정의합니다.
 * </p>
 */
@Getter
@RequiredArgsConstructor
public enum OrderStatus {

    /** 주문대기 - 결제 전 */
    PENDING("주문대기"),
    /** 결제완료 - 결제 후 배송 전 */
    PAID("결제완료"),
    /** 배송중 */
    SHIPPING("배송중"),
    /** 배송완료 */
    DELIVERED("배송완료"),
    /** 취소됨 */
    CANCELLED("취소됨");

    /** 상태 설명 */
    private final String description;

    /**
     * 취소 가능 여부 확인
     *
     * @return PENDING 또는 PAID 상태면 true
     */
    public boolean isCancellable() {
        return this == PENDING || this == PAID;
    }
}
