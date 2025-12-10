package org.example.domain.order.repository;

import org.example.domain.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * 주문 Repository
 * <p>
 * 주문 엔티티에 대한 데이터 접근을 담당합니다.
 * </p>
 */
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * 구매자 ID로 주문 조회 (페이징, 주문 상품 포함)
     * <p>
     * N+1 문제 방지를 위해 orderItems를 fetch join합니다.
     * </p>
     *
     * @param buyerId  구매자 ID
     * @param pageable 페이징 정보
     * @return 주문 페이지 (주문 상품 포함)
     */
    @Query(value = "SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.orderItems WHERE o.buyerId = :buyerId",
           countQuery = "SELECT COUNT(o) FROM Order o WHERE o.buyerId = :buyerId")
    Page<Order> findByBuyerId(@Param("buyerId") Long buyerId, Pageable pageable);

    /**
     * 주문 상세 조회 (주문 상품 포함)
     *
     * @param id 주문 ID
     * @return 주문 Optional (주문 상품 fetch join)
     */
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.orderItems WHERE o.id = :id")
    Optional<Order> findByIdWithItems(@Param("id") Long id);

    /**
     * 구매자 본인 주문 상세 조회 (주문 상품 포함)
     *
     * @param id      주문 ID
     * @param buyerId 구매자 ID
     * @return 주문 Optional (주문 상품 fetch join)
     */
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.orderItems WHERE o.id = :id AND o.buyerId = :buyerId")
    Optional<Order> findByIdAndBuyerIdWithItems(@Param("id") Long id, @Param("buyerId") Long buyerId);
}
