package org.example.domain.product.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.domain.product.dto.response.ProductResponse;
import org.example.domain.product.service.ProductService;
import org.example.global.common.ApiResponse;
import org.example.global.common.PagingResponse;
import org.springframework.web.bind.annotation.*;

/**
 * 상품 조회 API 컨트롤러
 * <p>
 * 모든 사용자가 접근 가능한 상품 조회 API를 제공합니다.
 * </p>
 */
@Tag(name = "상품", description = "상품 조회 API")
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "상품 목록 조회", description = "판매중/품절 상품 목록을 페이징하여 조회합니다")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping
    public ApiResponse<PagingResponse<ProductResponse>> getProducts(
            @Parameter(description = "페이지 번호 (0부터 시작)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기")
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(productService.getProducts(page, size));
    }

    @Operation(summary = "상품 상세 조회", description = "상품 ID로 상품 상세 정보를 조회합니다")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "상품을 찾을 수 없음")
    })
    @GetMapping("/{id}")
    public ApiResponse<ProductResponse> getProduct(
            @Parameter(description = "상품 ID", required = true)
            @PathVariable Long id) {
        return ApiResponse.success(productService.getProduct(id));
    }
}
