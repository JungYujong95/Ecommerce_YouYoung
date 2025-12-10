package org.example.domain.product.repository;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.example.domain.product.entity.Product;
import org.example.domain.product.entity.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 상품 Repository
 * <p>
 * 상품 엔티티에 대한 데이터 접근을 담당합니다.
 * </p>
 */
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * 상태 목록으로 상품 조회 (페이징)
     *
     * @param statuses 조회할 상태 목록
     * @param pageable 페이징 정보
     * @return 상품 페이지
     */
    Page<Product> findByStatusIn(List<ProductStatus> statuses, Pageable pageable);

    /**
     * 판매자 ID로 상품 조회 (페이징)
     *
     * @param sellerId 판매자 ID
     * @param pageable 페이징 정보
     * @return 상품 페이지
     */
    Page<Product> findBySellerId(Long sellerId, Pageable pageable);

    /**
     * 비관적 락으로 상품 조회 (SELECT FOR UPDATE)
     * <p>
     * 주문 생성/취소 시 재고 동시성 제어에 사용됩니다.
     * 락 획득 타임아웃: 3초
     * </p>
     *
     * @param id 상품 ID
     * @return 상품 Optional (락 획득 후)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000")})
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdWithLock(@Param("id") Long id);
}
