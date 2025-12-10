package org.example.domain.order.service;

import org.example.domain.order.dto.request.OrderCreateRequest;
import org.example.domain.order.dto.response.OrderResponse;
import org.example.domain.order.entity.Order;
import org.example.domain.order.entity.OrderItem;
import org.example.domain.order.entity.OrderStatus;
import org.example.domain.order.repository.OrderRepository;
import org.example.domain.product.entity.Product;
import org.example.domain.product.entity.ProductStatus;
import org.example.domain.product.repository.ProductRepository;
import org.example.global.common.PagingResponse;
import org.example.global.exception.BusinessException;
import org.example.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderServiceImpl 단위 테스트")
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Nested
    @DisplayName("createOrder 메서드")
    class CreateOrderTest {

        @Test
        @DisplayName("정상 주문 생성 성공")
        void createOrder_Success() {
            // given
            Long buyerId = 1L;
            Long productId = 1L;
            int quantity = 2;
            OrderCreateRequest request = createOrderCreateRequest(productId, quantity);
            Product product = createProduct(productId, "테스트 상품", 10000L, 100, ProductStatus.SELLING, 1L);
            Order savedOrder = createOrder(1L, buyerId, OrderStatus.PENDING, product, quantity);

            given(productRepository.findByIdWithLock(productId)).willReturn(Optional.of(product));
            given(orderRepository.save(any(Order.class))).willReturn(savedOrder);

            // when
            OrderResponse response = orderService.createOrder(request, buyerId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo(OrderStatus.PENDING);
            assertThat(response.getTotalPrice()).isEqualTo(20000L);

            verify(productRepository).findByIdWithLock(productId);
            verify(orderRepository).save(any(Order.class));
        }

        @Test
        @DisplayName("주문 시 재고 차감 확인")
        void createOrder_StockDecreased() {
            // given
            Long buyerId = 1L;
            Long productId = 1L;
            int quantity = 10;
            int initialStock = 100;
            OrderCreateRequest request = createOrderCreateRequest(productId, quantity);
            Product product = createProduct(productId, "테스트 상품", 10000L, initialStock, ProductStatus.SELLING, 1L);
            Order savedOrder = createOrder(1L, buyerId, OrderStatus.PENDING, product, quantity);

            given(productRepository.findByIdWithLock(productId)).willReturn(Optional.of(product));
            given(orderRepository.save(any(Order.class))).willReturn(savedOrder);

            // when
            orderService.createOrder(request, buyerId);

            // then
            assertThat(product.getStockQuantity()).isEqualTo(initialStock - quantity);
        }

        @Test
        @DisplayName("재고 소진 시 SOLD_OUT 상태 변경 확인")
        void createOrder_StockZero_StatusSoldOut() {
            // given
            Long buyerId = 1L;
            Long productId = 1L;
            int quantity = 10;
            int initialStock = 10;
            OrderCreateRequest request = createOrderCreateRequest(productId, quantity);
            Product product = createProduct(productId, "테스트 상품", 10000L, initialStock, ProductStatus.SELLING, 1L);
            Order savedOrder = createOrder(1L, buyerId, OrderStatus.PENDING, product, quantity);

            given(productRepository.findByIdWithLock(productId)).willReturn(Optional.of(product));
            given(orderRepository.save(any(Order.class))).willReturn(savedOrder);

            // when
            orderService.createOrder(request, buyerId);

            // then
            assertThat(product.getStockQuantity()).isEqualTo(0);
            assertThat(product.getStatus()).isEqualTo(ProductStatus.SOLD_OUT);
        }

        @Test
        @DisplayName("존재하지 않는 상품 주문 시 PRODUCT_NOT_FOUND 예외")
        void createOrder_ProductNotFound_ThrowsException() {
            // given
            Long buyerId = 1L;
            Long productId = 999L;
            OrderCreateRequest request = createOrderCreateRequest(productId, 1);

            given(productRepository.findByIdWithLock(productId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> orderService.createOrder(request, buyerId))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception -> {
                        BusinessException be = (BusinessException) exception;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
                    });

            verify(productRepository).findByIdWithLock(productId);
            verify(orderRepository, never()).save(any(Order.class));
        }

        @Test
        @DisplayName("재고 부족 시 INSUFFICIENT_STOCK 예외")
        void createOrder_InsufficientStock_ThrowsException() {
            // given
            Long buyerId = 1L;
            Long productId = 1L;
            int quantity = 100;
            int availableStock = 10;
            OrderCreateRequest request = createOrderCreateRequest(productId, quantity);
            Product product = createProduct(productId, "테스트 상품", 10000L, availableStock, ProductStatus.SELLING, 1L);

            given(productRepository.findByIdWithLock(productId)).willReturn(Optional.of(product));

            // when & then
            assertThatThrownBy(() -> orderService.createOrder(request, buyerId))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception -> {
                        BusinessException be = (BusinessException) exception;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.INSUFFICIENT_STOCK);
                    });

            verify(productRepository).findByIdWithLock(productId);
            verify(orderRepository, never()).save(any(Order.class));
        }
    }

    @Nested
    @DisplayName("getMyOrders 메서드")
    class GetMyOrdersTest {

        @Test
        @DisplayName("본인 주문만 조회")
        void getMyOrders_Success() {
            // given
            Long buyerId = 1L;
            int page = 0;
            int size = 10;

            Product product = createProduct(1L, "상품", 10000L, 100, ProductStatus.SELLING, 1L);
            List<Order> orders = List.of(
                    createOrder(1L, buyerId, OrderStatus.PENDING, product, 1),
                    createOrder(2L, buyerId, OrderStatus.PAID, product, 2)
            );
            Page<Order> orderPage = new PageImpl<>(orders);

            given(orderRepository.findByBuyerId(eq(buyerId), any(Pageable.class))).willReturn(orderPage);

            // when
            PagingResponse<OrderResponse> response = orderService.getMyOrders(buyerId, page, size);

            // then
            assertThat(response).isNotNull();
            assertThat(response.content()).hasSize(2);

            verify(orderRepository).findByBuyerId(eq(buyerId), any(Pageable.class));
        }

        @Test
        @DisplayName("페이징 정보 검증")
        void getMyOrders_PagingInfoValidation() {
            // given
            Long buyerId = 1L;
            int page = 1;
            int size = 20;
            long totalElements = 50L;

            Product product = createProduct(1L, "상품", 10000L, 100, ProductStatus.SELLING, 1L);
            List<Order> orders = List.of(createOrder(1L, buyerId, OrderStatus.PENDING, product, 1));
            Page<Order> orderPage = new PageImpl<>(orders, org.springframework.data.domain.PageRequest.of(page, size), totalElements);

            given(orderRepository.findByBuyerId(eq(buyerId), any(Pageable.class))).willReturn(orderPage);

            // when
            PagingResponse<OrderResponse> response = orderService.getMyOrders(buyerId, page, size);

            // then
            assertThat(response.paging().currentPage()).isEqualTo(page);
            assertThat(response.paging().pageSize()).isEqualTo(size);
            assertThat(response.paging().totalElements()).isEqualTo(totalElements);
        }
    }

    @Nested
    @DisplayName("getOrder 메서드")
    class GetOrderTest {

        @Test
        @DisplayName("본인 주문 조회 성공")
        void getOrder_Success() {
            // given
            Long orderId = 1L;
            Long buyerId = 1L;
            Product product = createProduct(1L, "상품", 10000L, 100, ProductStatus.SELLING, 1L);
            Order order = createOrder(orderId, buyerId, OrderStatus.PENDING, product, 1);

            given(orderRepository.findByIdAndBuyerIdWithItems(orderId, buyerId)).willReturn(Optional.of(order));

            // when
            OrderResponse response = orderService.getOrder(orderId, buyerId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(orderId);

            verify(orderRepository).findByIdAndBuyerIdWithItems(orderId, buyerId);
        }

        @Test
        @DisplayName("존재하지 않는 주문 조회 시 ORDER_NOT_FOUND 예외")
        void getOrder_NotFound_ThrowsException() {
            // given
            Long orderId = 999L;
            Long buyerId = 1L;

            given(orderRepository.findByIdAndBuyerIdWithItems(orderId, buyerId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> orderService.getOrder(orderId, buyerId))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception -> {
                        BusinessException be = (BusinessException) exception;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.ORDER_NOT_FOUND);
                    });
        }

        @Test
        @DisplayName("타인 주문 조회 시 ORDER_NOT_FOUND 예외 (쿼리 결과 없음)")
        void getOrder_OtherBuyer_ThrowsException() {
            // given
            Long orderId = 1L;
            Long buyerId = 1L;
            Long otherBuyerId = 2L;

            given(orderRepository.findByIdAndBuyerIdWithItems(orderId, otherBuyerId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> orderService.getOrder(orderId, otherBuyerId))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception -> {
                        BusinessException be = (BusinessException) exception;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.ORDER_NOT_FOUND);
                    });
        }
    }

    @Nested
    @DisplayName("cancelOrder 메서드")
    class CancelOrderTest {

        @Test
        @DisplayName("PENDING 상태 주문 정상 취소")
        void cancelOrder_PendingOrder_Success() {
            // given
            Long orderId = 1L;
            Long buyerId = 1L;
            Long productId = 1L;
            Product product = createProduct(productId, "상품", 10000L, 90, ProductStatus.SELLING, 1L);
            Order order = createOrder(orderId, buyerId, OrderStatus.PENDING, product, 10);

            given(orderRepository.findByIdWithItems(orderId)).willReturn(Optional.of(order));
            given(productRepository.findByIdWithLock(productId)).willReturn(Optional.of(product));

            // when
            orderService.cancelOrder(orderId, buyerId);

            // then
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
            verify(orderRepository).findByIdWithItems(orderId);
            verify(productRepository).findByIdWithLock(productId);
        }

        @Test
        @DisplayName("PAID 상태 주문 정상 취소")
        void cancelOrder_PaidOrder_Success() {
            // given
            Long orderId = 1L;
            Long buyerId = 1L;
            Long productId = 1L;
            Product product = createProduct(productId, "상품", 10000L, 90, ProductStatus.SELLING, 1L);
            Order order = createOrder(orderId, buyerId, OrderStatus.PAID, product, 10);

            given(orderRepository.findByIdWithItems(orderId)).willReturn(Optional.of(order));
            given(productRepository.findByIdWithLock(productId)).willReturn(Optional.of(product));

            // when
            orderService.cancelOrder(orderId, buyerId);

            // then
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        }

        @Test
        @DisplayName("취소 시 재고 복구 확인")
        void cancelOrder_StockRestored() {
            // given
            Long orderId = 1L;
            Long buyerId = 1L;
            Long productId = 1L;
            int currentStock = 90;
            int orderQuantity = 10;
            Product product = createProduct(productId, "상품", 10000L, currentStock, ProductStatus.SELLING, 1L);
            Order order = createOrder(orderId, buyerId, OrderStatus.PENDING, product, orderQuantity);

            given(orderRepository.findByIdWithItems(orderId)).willReturn(Optional.of(order));
            given(productRepository.findByIdWithLock(productId)).willReturn(Optional.of(product));

            // when
            orderService.cancelOrder(orderId, buyerId);

            // then
            assertThat(product.getStockQuantity()).isEqualTo(currentStock + orderQuantity);
        }

        @Test
        @DisplayName("SOLD_OUT 상품이 취소 시 SELLING 상태로 변경")
        void cancelOrder_SoldOutToSelling() {
            // given
            Long orderId = 1L;
            Long buyerId = 1L;
            Long productId = 1L;
            int currentStock = 0;
            int orderQuantity = 10;
            Product product = createProduct(productId, "상품", 10000L, currentStock, ProductStatus.SOLD_OUT, 1L);
            Order order = createOrder(orderId, buyerId, OrderStatus.PENDING, product, orderQuantity);

            given(orderRepository.findByIdWithItems(orderId)).willReturn(Optional.of(order));
            given(productRepository.findByIdWithLock(productId)).willReturn(Optional.of(product));

            // when
            orderService.cancelOrder(orderId, buyerId);

            // then
            assertThat(product.getStockQuantity()).isEqualTo(orderQuantity);
            assertThat(product.getStatus()).isEqualTo(ProductStatus.SELLING);
        }

        @Test
        @DisplayName("존재하지 않는 주문 취소 시 ORDER_NOT_FOUND 예외")
        void cancelOrder_NotFound_ThrowsException() {
            // given
            Long orderId = 999L;
            Long buyerId = 1L;

            given(orderRepository.findByIdWithItems(orderId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> orderService.cancelOrder(orderId, buyerId))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception -> {
                        BusinessException be = (BusinessException) exception;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.ORDER_NOT_FOUND);
                    });
        }

        @Test
        @DisplayName("타인 주문 취소 시 ACCESS_DENIED 예외")
        void cancelOrder_OtherBuyer_ThrowsException() {
            // given
            Long orderId = 1L;
            Long ownerId = 1L;
            Long attempterId = 2L;
            Product product = createProduct(1L, "상품", 10000L, 100, ProductStatus.SELLING, 1L);
            Order order = createOrder(orderId, ownerId, OrderStatus.PENDING, product, 10);

            given(orderRepository.findByIdWithItems(orderId)).willReturn(Optional.of(order));

            // when & then
            assertThatThrownBy(() -> orderService.cancelOrder(orderId, attempterId))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception -> {
                        BusinessException be = (BusinessException) exception;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED);
                    });

            verify(productRepository, never()).findByIdWithLock(anyLong());
        }

        @Test
        @DisplayName("이미 취소된 주문 재취소 시 ORDER_ALREADY_CANCELLED 예외")
        void cancelOrder_AlreadyCancelled_ThrowsException() {
            // given
            Long orderId = 1L;
            Long buyerId = 1L;
            Product product = createProduct(1L, "상품", 10000L, 100, ProductStatus.SELLING, 1L);
            Order order = createOrder(orderId, buyerId, OrderStatus.CANCELLED, product, 10);

            given(orderRepository.findByIdWithItems(orderId)).willReturn(Optional.of(order));

            // when & then
            assertThatThrownBy(() -> orderService.cancelOrder(orderId, buyerId))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception -> {
                        BusinessException be = (BusinessException) exception;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.ORDER_ALREADY_CANCELLED);
                    });

            verify(productRepository, never()).findByIdWithLock(anyLong());
        }

        @Test
        @DisplayName("SHIPPING 상태 주문 취소 시 ORDER_CANNOT_CANCEL 예외")
        void cancelOrder_ShippingOrder_ThrowsException() {
            // given
            Long orderId = 1L;
            Long buyerId = 1L;
            Product product = createProduct(1L, "상품", 10000L, 100, ProductStatus.SELLING, 1L);
            Order order = createOrder(orderId, buyerId, OrderStatus.SHIPPING, product, 10);

            given(orderRepository.findByIdWithItems(orderId)).willReturn(Optional.of(order));

            // when & then
            assertThatThrownBy(() -> orderService.cancelOrder(orderId, buyerId))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception -> {
                        BusinessException be = (BusinessException) exception;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.ORDER_CANNOT_CANCEL);
                    });

            verify(productRepository, never()).findByIdWithLock(anyLong());
        }

        @Test
        @DisplayName("DELIVERED 상태 주문 취소 시 ORDER_CANNOT_CANCEL 예외")
        void cancelOrder_DeliveredOrder_ThrowsException() {
            // given
            Long orderId = 1L;
            Long buyerId = 1L;
            Product product = createProduct(1L, "상품", 10000L, 100, ProductStatus.SELLING, 1L);
            Order order = createOrder(orderId, buyerId, OrderStatus.DELIVERED, product, 10);

            given(orderRepository.findByIdWithItems(orderId)).willReturn(Optional.of(order));

            // when & then
            assertThatThrownBy(() -> orderService.cancelOrder(orderId, buyerId))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception -> {
                        BusinessException be = (BusinessException) exception;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.ORDER_CANNOT_CANCEL);
                    });

            verify(productRepository, never()).findByIdWithLock(anyLong());
        }
    }

    // ========== Helper Methods ==========

    private OrderCreateRequest createOrderCreateRequest(Long productId, Integer quantity) {
        return OrderCreateRequest.builder()
                .productId(productId)
                .quantity(quantity)
                .build();
    }

    private Product createProduct(Long id, String name, Long price, Integer stockQuantity, ProductStatus status, Long sellerId) {
        Product product = Product.builder()
                .name(name)
                .price(price)
                .stockQuantity(stockQuantity)
                .sellerId(sellerId)
                .build();
        ReflectionTestUtils.setField(product, "id", id);
        ReflectionTestUtils.setField(product, "status", status);
        ReflectionTestUtils.setField(product, "createdAt", LocalDateTime.now());
        return product;
    }

    private Order createOrder(Long id, Long buyerId, OrderStatus status, Product product, int quantity) {
        Order order = Order.builder()
                .buyerId(buyerId)
                .build();
        ReflectionTestUtils.setField(order, "id", id);
        ReflectionTestUtils.setField(order, "status", status);
        ReflectionTestUtils.setField(order, "createdAt", LocalDateTime.now());

        OrderItem orderItem = OrderItem.builder()
                .productId(product.getId())
                .productName(product.getName())
                .productPrice(product.getPrice())
                .quantity(quantity)
                .build();
        ReflectionTestUtils.setField(orderItem, "id", 1L);

        order.addOrderItem(orderItem);

        return order;
    }
}
