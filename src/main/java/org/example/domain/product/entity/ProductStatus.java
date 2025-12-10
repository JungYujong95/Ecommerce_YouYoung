package org.example.domain.product.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 상품 상태
 * <p>
 * 상품의 판매 상태를 정의합니다.
 * </p>
 */
@Getter
@RequiredArgsConstructor
public enum ProductStatus {

    /** 판매중 - 구매 가능 */
    SELLING("판매중"),
    /** 품절 - 재고 소진 */
    SOLD_OUT("품절"),
    /** 판매중지 - 판매자가 판매 중지 */
    STOPPED("판매중지");

    /** 상태 설명 */
    private final String description;
}
