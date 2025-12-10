package org.example.domain.product.service;

import org.example.domain.product.dto.request.ProductCreateRequest;
import org.example.domain.product.dto.request.ProductUpdateRequest;
import org.example.domain.product.dto.response.ProductResponse;
import org.example.global.common.PagingResponse;

/**
 * 판매자 상품 관리 서비스
 * <p>
 * 판매자(SELLER, ADMIN)가 상품을 등록, 수정, 삭제하는 기능을 제공합니다.
 * </p>
 */
public interface SellerProductService {

    /**
     * 상품 등록
     *
     * @param request  상품 생성 요청 DTO (상품명, 가격, 재고수량)
     * @param sellerId 판매자 ID
     * @return 생성된 상품 응답 DTO
     */
    ProductResponse createProduct(ProductCreateRequest request, Long sellerId);

    /**
     * 내 상품 목록 조회
     * <p>
     * 판매자 본인이 등록한 상품 목록을 페이징하여 조회합니다.
     * 모든 상태(SELLING, SOLD_OUT, STOPPED)의 상품이 조회됩니다.
     * </p>
     *
     * @param sellerId 판매자 ID
     * @param page     페이지 번호 (0부터 시작)
     * @param size     페이지 크기
     * @return 페이징된 상품 목록
     */
    PagingResponse<ProductResponse> getMyProducts(Long sellerId, int page, int size);

    /**
     * 상품 수정
     * <p>
     * 상품명, 가격, 재고수량을 수정합니다. 본인 상품만 수정 가능합니다.
     * </p>
     *
     * @param productId 상품 ID
     * @param request   수정 요청 DTO (수정할 필드만 포함)
     * @param sellerId  판매자 ID (소유권 검증용)
     * @return 수정된 상품 응답 DTO
     * @throws org.example.global.exception.BusinessException PRODUCT_NOT_FOUND - 상품이 존재하지 않을 경우
     * @throws org.example.global.exception.BusinessException ACCESS_DENIED - 본인 상품이 아닐 경우
     */
    ProductResponse updateProduct(Long productId, ProductUpdateRequest request, Long sellerId);

    /**
     * 상품 삭제
     * <p>
     * 본인 상품만 삭제 가능합니다.
     * </p>
     *
     * @param productId 상품 ID
     * @param sellerId  판매자 ID (소유권 검증용)
     * @throws org.example.global.exception.BusinessException PRODUCT_NOT_FOUND - 상품이 존재하지 않을 경우
     * @throws org.example.global.exception.BusinessException ACCESS_DENIED - 본인 상품이 아닐 경우
     */
    void deleteProduct(Long productId, Long sellerId);
}
