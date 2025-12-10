package org.example.domain.order.service;

import org.example.domain.order.dto.request.OrderCreateRequest;
import org.example.domain.order.dto.response.OrderResponse;
import org.example.global.common.PagingResponse;

/**
 * 주문 서비스
 * <p>
 * 주문 생성, 조회, 취소 기능을 제공합니다.
 * </p>
 */
public interface OrderService {

    /**
     * 주문 생성
     * <p>
     * 비관적 락으로 상품 재고를 차감하고 주문을 생성합니다.
     * </p>
     *
     * @param request 주문 생성 요청 DTO (상품 ID, 수량)
     * @param buyerId 구매자 ID
     * @return 생성된 주문 응답 DTO
     * @throws org.example.global.exception.BusinessException PRODUCT_NOT_FOUND - 상품이 존재하지 않을 경우
     * @throws org.example.global.exception.BusinessException INSUFFICIENT_STOCK - 재고가 부족할 경우
     */
    OrderResponse createOrder(OrderCreateRequest request, Long buyerId);

    /**
     * 내 주문 목록 조회
     *
     * @param buyerId 구매자 ID
     * @param page    페이지 번호 (0부터 시작)
     * @param size    페이지 크기
     * @return 페이징된 주문 목록
     */
    PagingResponse<OrderResponse> getMyOrders(Long buyerId, int page, int size);

    /**
     * 주문 상세 조회
     *
     * @param orderId 주문 ID
     * @param buyerId 구매자 ID (본인 확인용)
     * @return 주문 응답 DTO
     * @throws org.example.global.exception.BusinessException ORDER_NOT_FOUND - 주문이 존재하지 않거나 본인 주문이 아닐 경우
     */
    OrderResponse getOrder(Long orderId, Long buyerId);

    /**
     * 주문 취소
     * <p>
     * 비관적 락으로 재고를 복구하고 주문을 취소합니다.
     * PENDING, PAID 상태에서만 취소 가능합니다.
     * </p>
     *
     * @param orderId 주문 ID
     * @param buyerId 구매자 ID (본인 확인용)
     * @throws org.example.global.exception.BusinessException ORDER_NOT_FOUND - 주문이 존재하지 않을 경우
     * @throws org.example.global.exception.BusinessException ACCESS_DENIED - 본인 주문이 아닐 경우
     * @throws org.example.global.exception.BusinessException ORDER_ALREADY_CANCELLED - 이미 취소된 주문일 경우
     * @throws org.example.global.exception.BusinessException ORDER_CANNOT_CANCEL - 취소 불가능한 상태일 경우
     */
    void cancelOrder(Long orderId, Long buyerId);
}
