package org.example.domain.product.service;

import org.example.domain.product.dto.response.ProductResponse;
import org.example.global.common.PagingResponse;

/**
 * 상품 조회 서비스
 * <p>
 * 모든 사용자가 접근 가능한 상품 조회 기능을 제공합니다.
 * </p>
 */
public interface ProductService {

    /**
     * 상품 상세 조회
     *
     * @param productId 상품 ID
     * @return 상품 응답 DTO
     * @throws org.example.global.exception.BusinessException PRODUCT_NOT_FOUND - 상품이 존재하지 않을 경우
     */
    ProductResponse getProduct(Long productId);

    /**
     * 판매중/품절 상품 목록 페이징 조회
     * <p>
     * SELLING, SOLD_OUT 상태의 상품만 조회됩니다. STOPPED 상태는 제외됩니다.
     * </p>
     *
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @return 페이징된 상품 목록
     */
    PagingResponse<ProductResponse> getProducts(int page, int size);
}
