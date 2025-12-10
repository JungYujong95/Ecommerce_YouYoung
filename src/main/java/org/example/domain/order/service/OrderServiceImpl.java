package org.example.domain.order.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.domain.order.dto.request.OrderCreateRequest;
import org.example.domain.order.dto.response.OrderResponse;
import org.example.domain.order.entity.Order;
import org.example.domain.order.entity.OrderItem;
import org.example.domain.order.repository.OrderRepository;
import org.example.domain.product.entity.Product;
import org.example.domain.product.repository.ProductRepository;
import org.example.global.common.PagingInfo;
import org.example.global.common.PagingResponse;
import org.example.global.exception.BusinessException;
import org.example.global.exception.ErrorCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional
    public OrderResponse createOrder(OrderCreateRequest request, Long buyerId) {
        // 1. 비관적 락으로 상품 조회 (SELECT FOR UPDATE)
        Product product = productRepository.findByIdWithLock(request.getProductId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        // 2. 재고 차감 (락 걸린 상태에서 안전하게 처리)
        product.decreaseStock(request.getQuantity());

        // 3. 주문 생성
        Order order = Order.create(buyerId, product, request.getQuantity());
        Order savedOrder = orderRepository.save(order);

        log.info("주문 생성 완료: orderId={}, buyerId={}, productId={}, quantity={}",
                savedOrder.getId(), buyerId, request.getProductId(), request.getQuantity());

        return OrderResponse.from(savedOrder);
    }

    @Override
    public PagingResponse<OrderResponse> getMyOrders(Long buyerId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Order> orderPage = orderRepository.findByBuyerId(buyerId, pageRequest);

        List<OrderResponse> content = orderPage.getContent().stream()
                .map(OrderResponse::from)
                .toList();

        PagingInfo pagingInfo = new PagingInfo(
                orderPage.getNumber(),
                orderPage.getSize(),
                orderPage.getTotalElements()
        );

        return new PagingResponse<>(pagingInfo, content);
    }

    @Override
    public OrderResponse getOrder(Long orderId, Long buyerId) {
        Order order = orderRepository.findByIdAndBuyerIdWithItems(orderId, buyerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        return OrderResponse.from(order);
    }

    @Override
    @Transactional
    public void cancelOrder(Long orderId, Long buyerId) {
        // 1. 주문 조회 (아이템 포함)
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        // 2. 본인 주문인지 확인
        if (!order.getBuyerId().equals(buyerId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        // 3. 주문 취소 (상태 검증 포함)
        order.cancel();

        // 4. 비관적 락으로 상품 조회 후 재고 복구
        for (OrderItem item : order.getOrderItems()) {
            Product product = productRepository.findByIdWithLock(item.getProductId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
            product.increaseStock(item.getQuantity());
        }

        log.info("주문 취소 완료: orderId={}, buyerId={}", orderId, buyerId);
    }
}
