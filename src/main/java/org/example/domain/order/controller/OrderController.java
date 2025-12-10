package org.example.domain.order.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.domain.order.dto.request.OrderCreateRequest;
import org.example.domain.order.dto.response.OrderResponse;
import org.example.domain.order.service.OrderService;
import org.example.global.common.ApiResponse;
import org.example.global.common.PagingResponse;
import org.example.global.security.auth.CustomUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "주문", description = "주문 관련 API")
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "주문 생성", description = "상품을 주문합니다. 재고가 차감됩니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "주문 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "재고 부족 또는 잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "상품을 찾을 수 없음")
    })
    @PostMapping
    public ApiResponse<OrderResponse> createOrder(
            @Valid @RequestBody OrderCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ApiResponse.success(orderService.createOrder(request, principal.getId()));
    }

    @Operation(summary = "내 주문 목록 조회", description = "로그인한 사용자의 주문 목록을 페이징하여 조회합니다")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping
    public ApiResponse<PagingResponse<OrderResponse>> getMyOrders(
            @Parameter(description = "페이지 번호 (0부터 시작)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기")
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ApiResponse.success(orderService.getMyOrders(principal.getId(), page, size));
    }

    @Operation(summary = "주문 상세 조회", description = "주문 ID로 주문 상세 정보를 조회합니다 (본인 주문만)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "주문을 찾을 수 없음")
    })
    @GetMapping("/{id}")
    public ApiResponse<OrderResponse> getOrder(
            @Parameter(description = "주문 ID", required = true)
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ApiResponse.success(orderService.getOrder(id, principal.getId()));
    }

    @Operation(summary = "주문 취소", description = "주문을 취소합니다. 재고가 복구됩니다. (본인 주문만, PENDING/PAID 상태만 취소 가능)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "취소 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "취소 불가능한 상태"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "주문을 찾을 수 없음")
    })
    @PostMapping("/{id}/cancel")
    public ApiResponse<Void> cancelOrder(
            @Parameter(description = "주문 ID", required = true)
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {
        orderService.cancelOrder(id, principal.getId());
        return ApiResponse.success(null);
    }


}
