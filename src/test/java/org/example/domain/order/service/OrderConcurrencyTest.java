package org.example.domain.order.service;

import org.example.domain.order.dto.request.OrderCreateRequest;
import org.example.domain.order.entity.Order;
import org.example.domain.order.repository.OrderRepository;
import org.example.domain.product.entity.Product;
import org.example.domain.product.entity.ProductStatus;
import org.example.domain.product.repository.ProductRepository;
import org.example.global.exception.BusinessException;
import org.example.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("OrderService 동시성 통합 테스트")
class OrderConcurrencyTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        productRepository.deleteAll();
    }

    @Test
    @DisplayName("동시 주문 시 재고보다 많이 주문 불가 (재고 1개, 동시 주문 2개)")
    void 동시_주문시_재고보다_많이_주문_불가() throws InterruptedException {
        // given
        int initialStock = 1;
        int threadCount = 2;

        testProduct = productRepository.save(Product.builder()
                .name("한정 상품")
                .price(10000L)
                .stockQuantity(initialStock)
                .sellerId(1L)
                .build());

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

        // when
        for (int i = 0; i < threadCount; i++) {
            final long buyerId = i + 1L;
            executorService.submit(() -> {
                try {
                    startLatch.await();

                    OrderCreateRequest request = createOrderRequest(testProduct.getId(), 1);
                    orderService.createOrder(request, buyerId);
                    successCount.incrementAndGet();
                } catch (BusinessException e) {
                    if (e.getErrorCode() == ErrorCode.INSUFFICIENT_STOCK) {
                        failCount.incrementAndGet();
                    }
                    exceptions.add(e);
                } catch (Exception e) {
                    exceptions.add(e);
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        endLatch.await();
        executorService.shutdown();

        // then
        Product updatedProduct = productRepository.findById(testProduct.getId()).orElseThrow();

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(1);
        assertThat(updatedProduct.getStockQuantity()).isEqualTo(0);
        assertThat(updatedProduct.getStatus()).isEqualTo(ProductStatus.SOLD_OUT);
    }

    @Test
    @DisplayName("동시 주문 시 재고 정합성 보장 (재고 10개, 동시 주문 10개)")
    void 동시_주문시_재고_정합성_보장() throws InterruptedException {
        // given
        int initialStock = 10;
        int threadCount = 10;

        testProduct = productRepository.save(Product.builder()
                .name("인기 상품")
                .price(10000L)
                .stockQuantity(initialStock)
                .sellerId(1L)
                .build());

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when
        for (int i = 0; i < threadCount; i++) {
            final long buyerId = i + 1L;
            executorService.submit(() -> {
                try {
                    startLatch.await();

                    OrderCreateRequest request = createOrderRequest(testProduct.getId(), 1);
                    orderService.createOrder(request, buyerId);
                    successCount.incrementAndGet();
                } catch (BusinessException e) {
                    if (e.getErrorCode() == ErrorCode.INSUFFICIENT_STOCK) {
                        failCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    // 기타 예외
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        endLatch.await();
        executorService.shutdown();

        // then
        Product updatedProduct = productRepository.findById(testProduct.getId()).orElseThrow();
        long orderCount = orderRepository.count();

        assertThat(successCount.get()).isEqualTo(threadCount);
        assertThat(failCount.get()).isEqualTo(0);
        assertThat(updatedProduct.getStockQuantity()).isEqualTo(0);
        assertThat(updatedProduct.getStatus()).isEqualTo(ProductStatus.SOLD_OUT);
        assertThat(orderCount).isEqualTo(threadCount);
    }

    @Test
    @DisplayName("주문과 취소가 동시에 발생해도 재고 정합성 유지")
    void 주문과_취소가_동시에_발생해도_재고_정합성_유지() throws InterruptedException {
        // given
        int initialStock = 5;
        int orderQuantity = 5;

        testProduct = productRepository.save(Product.builder()
                .name("테스트 상품")
                .price(10000L)
                .stockQuantity(initialStock)
                .sellerId(1L)
                .build());

        // 기존 주문 생성
        OrderCreateRequest initialRequest = createOrderRequest(testProduct.getId(), orderQuantity);
        orderService.createOrder(initialRequest, 100L);

        // 상품 재고 0, 주문 1개 존재 상태
        Product productAfterOrder = productRepository.findById(testProduct.getId()).orElseThrow();
        assertThat(productAfterOrder.getStockQuantity()).isEqualTo(0);

        Order existingOrder = orderRepository.findAll().get(0);

        // 2개 스레드: 1개는 취소(재고 +5), 1개는 주문(재고 -1)
        int threadCount = 2;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        AtomicInteger cancelSuccess = new AtomicInteger(0);
        AtomicInteger orderSuccess = new AtomicInteger(0);

        // when
        // 스레드 1: 주문 취소 (재고 복구)
        executorService.submit(() -> {
            try {
                startLatch.await();
                orderService.cancelOrder(existingOrder.getId(), 100L);
                cancelSuccess.incrementAndGet();
            } catch (Exception e) {
                // 예외 처리
            } finally {
                endLatch.countDown();
            }
        });

        // 스레드 2: 새 주문 시도
        executorService.submit(() -> {
            try {
                startLatch.await();
                Thread.sleep(50); // 취소가 먼저 완료되도록 약간의 지연
                OrderCreateRequest newRequest = createOrderRequest(testProduct.getId(), 1);
                orderService.createOrder(newRequest, 200L);
                orderSuccess.incrementAndGet();
            } catch (BusinessException e) {
                // INSUFFICIENT_STOCK 예외는 정상 케이스
            } catch (Exception e) {
                // 기타 예외
            } finally {
                endLatch.countDown();
            }
        });

        startLatch.countDown();
        endLatch.await();
        executorService.shutdown();

        // then
        Product finalProduct = productRepository.findById(testProduct.getId()).orElseThrow();

        assertThat(cancelSuccess.get()).isEqualTo(1);

        // 취소 후 재고가 복구되고, 새 주문이 성공했다면 재고 4개
        // 취소만 성공하고 주문이 재고 부족으로 실패했다면 재고 5개
        if (orderSuccess.get() == 1) {
            assertThat(finalProduct.getStockQuantity()).isEqualTo(4);
        } else {
            assertThat(finalProduct.getStockQuantity()).isEqualTo(5);
        }

        // 재고가 음수가 되는 일은 없어야 함
        assertThat(finalProduct.getStockQuantity()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("대량 동시 주문 시 재고 정합성 테스트 (재고 100개, 동시 주문 150개)")
    void 대량_동시_주문시_재고_정합성_테스트() throws InterruptedException {
        // given
        int initialStock = 100;
        int threadCount = 150;

        testProduct = productRepository.save(Product.builder()
                .name("인기 상품")
                .price(10000L)
                .stockQuantity(initialStock)
                .sellerId(1L)
                .build());

        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when
        for (int i = 0; i < threadCount; i++) {
            final long buyerId = i + 1L;
            executorService.submit(() -> {
                try {
                    startLatch.await();

                    OrderCreateRequest request = createOrderRequest(testProduct.getId(), 1);
                    orderService.createOrder(request, buyerId);
                    successCount.incrementAndGet();
                } catch (BusinessException e) {
                    if (e.getErrorCode() == ErrorCode.INSUFFICIENT_STOCK) {
                        failCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    // PessimisticLockingFailureException 등 다른 예외도 발생할 수 있음
                    failCount.incrementAndGet();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        endLatch.await();
        executorService.shutdown();

        // then
        Product updatedProduct = productRepository.findById(testProduct.getId()).orElseThrow();
        long orderCount = orderRepository.count();

        // 성공 + 실패 = 총 요청 수
        assertThat(successCount.get() + failCount.get()).isEqualTo(threadCount);

        // 성공한 주문 수만큼 재고 감소
        assertThat(updatedProduct.getStockQuantity()).isEqualTo(initialStock - successCount.get());

        // 재고가 음수가 되는 일은 없어야 함 (핵심 검증)
        assertThat(updatedProduct.getStockQuantity()).isGreaterThanOrEqualTo(0);

        // 성공한 주문 수만큼 주문 레코드 생성
        assertThat(orderCount).isEqualTo(successCount.get());

        // 성공한 주문은 최대 초기 재고(100)를 넘을 수 없음
        assertThat(successCount.get()).isLessThanOrEqualTo(initialStock);

        System.out.println("=== 테스트 결과 ===");
        System.out.println("초기 재고: " + initialStock);
        System.out.println("동시 요청 수: " + threadCount);
        System.out.println("성공: " + successCount.get());
        System.out.println("실패: " + failCount.get());
        System.out.println("최종 재고: " + updatedProduct.getStockQuantity());
        System.out.println("생성된 주문 수: " + orderCount);
    }

    // ========== Helper Methods ==========

    private OrderCreateRequest createOrderRequest(Long productId, Integer quantity) {
        return OrderCreateRequest.builder()
                .productId(productId)
                .quantity(quantity)
                .build();
    }
}
