package org.example.domain.product.service;

import org.example.domain.product.dto.response.ProductResponse;
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
import org.springframework.data.domain.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductServiceImpl 단위 테스트")
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    @Nested
    @DisplayName("getProduct 메서드")
    class GetProductTest {

        @Test
        @DisplayName("존재하는 상품 조회 성공")
        void getProduct_Success() {
            // given
            Long productId = 1L;
            Product product = createProduct(productId, "테스트 상품", 10000L, 100, ProductStatus.SELLING, 1L);

            given(productRepository.findById(productId)).willReturn(Optional.of(product));

            // when
            ProductResponse response = productService.getProduct(productId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(productId);
            assertThat(response.getName()).isEqualTo("테스트 상품");
            assertThat(response.getPrice()).isEqualTo(10000L);
            assertThat(response.getStockQuantity()).isEqualTo(100);
            assertThat(response.getStatus()).isEqualTo(ProductStatus.SELLING);

            verify(productRepository).findById(productId);
        }

        @Test
        @DisplayName("존재하지 않는 상품 조회 시 PRODUCT_NOT_FOUND 예외")
        void getProduct_NotFound_ThrowsException() {
            // given
            Long productId = 999L;

            given(productRepository.findById(productId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> productService.getProduct(productId))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception -> {
                        BusinessException be = (BusinessException) exception;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
                    });

            verify(productRepository).findById(productId);
        }
    }

    @Nested
    @DisplayName("getProducts 메서드")
    class GetProductsTest {

        @Test
        @DisplayName("SELLING/SOLD_OUT 상품만 조회 (STOPPED 제외)")
        void getProducts_OnlySellingAndSoldOut() {
            // given
            int page = 0;
            int size = 10;

            Product sellingProduct = createProduct(1L, "판매중 상품", 10000L, 100, ProductStatus.SELLING, 1L);
            Product soldOutProduct = createProduct(2L, "품절 상품", 20000L, 0, ProductStatus.SOLD_OUT, 1L);

            List<Product> products = List.of(sellingProduct, soldOutProduct);
            Page<Product> productPage = new PageImpl<>(products, PageRequest.of(page, size), 2);

            given(productRepository.findByStatusIn(
                    eq(List.of(ProductStatus.SELLING, ProductStatus.SOLD_OUT)),
                    any(Pageable.class)
            )).willReturn(productPage);

            // when
            PagingResponse<ProductResponse> response = productService.getProducts(page, size);

            // then
            assertThat(response).isNotNull();
            assertThat(response.content()).hasSize(2);
            assertThat(response.content().get(0).getStatus()).isEqualTo(ProductStatus.SELLING);
            assertThat(response.content().get(1).getStatus()).isEqualTo(ProductStatus.SOLD_OUT);

            verify(productRepository).findByStatusIn(
                    eq(List.of(ProductStatus.SELLING, ProductStatus.SOLD_OUT)),
                    any(Pageable.class)
            );
        }

        @Test
        @DisplayName("페이징 정보 검증")
        void getProducts_PagingInfoValidation() {
            // given
            int page = 1;
            int size = 20;
            long totalElements = 50L;

            List<Product> products = List.of(
                    createProduct(1L, "상품1", 10000L, 100, ProductStatus.SELLING, 1L),
                    createProduct(2L, "상품2", 20000L, 50, ProductStatus.SELLING, 1L)
            );
            Page<Product> productPage = new PageImpl<>(products, PageRequest.of(page, size), totalElements);

            given(productRepository.findByStatusIn(any(), any(Pageable.class))).willReturn(productPage);

            // when
            PagingResponse<ProductResponse> response = productService.getProducts(page, size);

            // then
            assertThat(response.paging().currentPage()).isEqualTo(page);
            assertThat(response.paging().pageSize()).isEqualTo(size);
            assertThat(response.paging().totalElements()).isEqualTo(totalElements);
        }

        @Test
        @DisplayName("빈 결과 반환")
        void getProducts_EmptyResult() {
            // given
            int page = 0;
            int size = 10;

            Page<Product> emptyPage = new PageImpl<>(List.of(), PageRequest.of(page, size), 0);

            given(productRepository.findByStatusIn(any(), any(Pageable.class))).willReturn(emptyPage);

            // when
            PagingResponse<ProductResponse> response = productService.getProducts(page, size);

            // then
            assertThat(response).isNotNull();
            assertThat(response.content()).isEmpty();
            assertThat(response.paging().totalElements()).isEqualTo(0);
        }
    }

    // ========== Helper Methods ==========

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
}
