package org.example.domain.product.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.domain.product.dto.request.ProductCreateRequest;
import org.example.domain.product.dto.request.ProductUpdateRequest;
import org.example.domain.product.dto.response.ProductResponse;
import org.example.domain.product.service.SellerProductService;
import org.example.global.common.ApiResponse;
import org.example.global.common.PagingResponse;
import org.example.global.security.auth.CustomUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 판매자 상품 관리 API 컨트롤러
 * <p>
 * 판매자(SELLER, ADMIN)가 상품을 등록, 수정, 삭제하는 API를 제공합니다.
 * </p>
 */
@Tag(name = "판매자 상품", description = "판매자 상품 관리 API")
@RestController
@RequestMapping("/api/v1/seller/products")
@RequiredArgsConstructor
public class SellerProductController {

    private final SellerProductService sellerProductService;

    @Operation(summary = "상품 등록", description = "새로운 상품을 등록합니다 (SELLER, ADMIN 권한 필요)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "상품 등록 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @PostMapping
    public ApiResponse<ProductResponse> createProduct(
            @Valid @RequestBody ProductCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ApiResponse.success(sellerProductService.createProduct(request, principal.getId()));
    }

    @Operation(summary = "내 상품 목록 조회", description = "로그인한 판매자의 상품 목록을 조회합니다 (SELLER, ADMIN 권한 필요)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @GetMapping
    public ApiResponse<PagingResponse<ProductResponse>> getMyProducts(
            @Parameter(description = "페이지 번호 (0부터 시작)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기")
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ApiResponse.success(sellerProductService.getMyProducts(principal.getId(), page, size));
    }

    @Operation(summary = "상품 수정", description = "상품 정보를 수정합니다 (본인 상품만 수정 가능)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "상품을 찾을 수 없음")
    })
    @PutMapping("/{id}")
    public ApiResponse<ProductResponse> updateProduct(
            @Parameter(description = "상품 ID", required = true)
            @PathVariable Long id,
            @Valid @RequestBody ProductUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ApiResponse.success(sellerProductService.updateProduct(id, request, principal.getId()));
    }

    @Operation(summary = "상품 삭제", description = "상품을 삭제합니다 (본인 상품만 삭제 가능)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "상품을 찾을 수 없음")
    })
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteProduct(
            @Parameter(description = "상품 ID", required = true)
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {
        sellerProductService.deleteProduct(id, principal.getId());
        return ApiResponse.success(null);
    }
}
