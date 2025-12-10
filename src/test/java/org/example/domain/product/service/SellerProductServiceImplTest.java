package org.example.domain.product.service;

import org.example.domain.product.dto.request.ProductCreateRequest;
import org.example.domain.product.dto.request.ProductUpdateRequest;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("SellerProductServiceImpl 단위 테스트")
class SellerProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private SellerProductServiceImpl sellerProductService;

    @Nested
    @DisplayName("createProduct 메서드")
    class CreateProductTest {

        @Test
        @DisplayName("정상 상품 생성 성공")
        void createProduct_Success() {
            // given
            Long sellerId = 1L;
            ProductCreateRequest request = createProductCreateRequest("테스트 상품", 10000L, 100);
            Product savedProduct = createProduct(1L, "테스트 상품", 10000L, 100, ProductStatus.SELLING, sellerId);

            given(productRepository.save(any(Product.class))).willReturn(savedProduct);

            // when
            ProductResponse response = sellerProductService.createProduct(request, sellerId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getName()).isEqualTo("테스트 상품");
            assertThat(response.getPrice()).isEqualTo(10000L);
            assertThat(response.getStockQuantity()).isEqualTo(100);
            assertThat(response.getStatus()).isEqualTo(ProductStatus.SELLING);

            verify(productRepository).save(any(Product.class));
        }

        @Test
        @DisplayName("sellerId가 올바르게 설정되는지 확인")
        void createProduct_SellerIdSet() {
            // given
            Long sellerId = 5L;
            ProductCreateRequest request = createProductCreateRequest("테스트 상품", 10000L, 100);
            Product savedProduct = createProduct(1L, "테스트 상품", 10000L, 100, ProductStatus.SELLING, sellerId);

            given(productRepository.save(any(Product.class))).willAnswer(invocation -> {
                Product product = invocation.getArgument(0);
                assertThat(product.getSellerId()).isEqualTo(sellerId);
                ReflectionTestUtils.setField(product, "id", 1L);
                return product;
            });

            // when
            sellerProductService.createProduct(request, sellerId);

            // then
            verify(productRepository).save(any(Product.class));
        }
    }

    @Nested
    @DisplayName("getMyProducts 메서드")
    class GetMyProductsTest {

        @Test
        @DisplayName("본인 상품만 조회")
        void getMyProducts_Success() {
            // given
            Long sellerId = 1L;
            int page = 0;
            int size = 10;

            List<Product> products = List.of(
                    createProduct(1L, "상품1", 10000L, 100, ProductStatus.SELLING, sellerId),
                    createProduct(2L, "상품2", 20000L, 50, ProductStatus.SELLING, sellerId)
            );
            Page<Product> productPage = new PageImpl<>(products);

            given(productRepository.findBySellerId(eq(sellerId), any(Pageable.class))).willReturn(productPage);

            // when
            PagingResponse<ProductResponse> response = sellerProductService.getMyProducts(sellerId, page, size);

            // then
            assertThat(response).isNotNull();
            assertThat(response.content()).hasSize(2);

            verify(productRepository).findBySellerId(eq(sellerId), any(Pageable.class));
        }

        @Test
        @DisplayName("페이징 정보 검증")
        void getMyProducts_PagingInfoValidation() {
            // given
            Long sellerId = 1L;
            int page = 1;
            int size = 20;

            Page<Product> emptyPage = new PageImpl<>(List.of());

            given(productRepository.findBySellerId(eq(sellerId), any(Pageable.class))).willReturn(emptyPage);

            // when
            PagingResponse<ProductResponse> response = sellerProductService.getMyProducts(sellerId, page, size);

            // then
            assertThat(response).isNotNull();
            assertThat(response.content()).isEmpty();
        }
    }

    @Nested
    @DisplayName("updateProduct 메서드")
    class UpdateProductTest {

        @Test
        @DisplayName("정상 상품 수정 성공")
        void updateProduct_Success() {
            // given
            Long productId = 1L;
            Long sellerId = 1L;
            Product product = createProduct(productId, "기존 상품", 10000L, 100, ProductStatus.SELLING, sellerId);
            ProductUpdateRequest request = createProductUpdateRequest("수정된 상품", 20000L, 200);

            given(productRepository.findById(productId)).willReturn(Optional.of(product));

            // when
            ProductResponse response = sellerProductService.updateProduct(productId, request, sellerId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getName()).isEqualTo("수정된 상품");
            assertThat(response.getPrice()).isEqualTo(20000L);
            assertThat(response.getStockQuantity()).isEqualTo(200);

            verify(productRepository).findById(productId);
        }

        @Test
        @DisplayName("존재하지 않는 상품 수정 시 PRODUCT_NOT_FOUND 예외")
        void updateProduct_NotFound_ThrowsException() {
            // given
            Long productId = 999L;
            Long sellerId = 1L;
            ProductUpdateRequest request = createProductUpdateRequest("수정된 상품", 20000L, 200);

            given(productRepository.findById(productId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> sellerProductService.updateProduct(productId, request, sellerId))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception -> {
                        BusinessException be = (BusinessException) exception;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
                    });

            verify(productRepository).findById(productId);
        }

        @Test
        @DisplayName("다른 판매자 상품 수정 시 ACCESS_DENIED 예외")
        void updateProduct_NotOwner_ThrowsException() {
            // given
            Long productId = 1L;
            Long ownerId = 1L;
            Long attempterId = 2L;
            Product product = createProduct(productId, "상품", 10000L, 100, ProductStatus.SELLING, ownerId);
            ProductUpdateRequest request = createProductUpdateRequest("수정된 상품", 20000L, 200);

            given(productRepository.findById(productId)).willReturn(Optional.of(product));

            // when & then
            assertThatThrownBy(() -> sellerProductService.updateProduct(productId, request, attempterId))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception -> {
                        BusinessException be = (BusinessException) exception;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED);
                    });
        }

        @Test
        @DisplayName("재고 0으로 수정 시 SOLD_OUT 상태로 변경")
        void updateProduct_StockZero_StatusSoldOut() {
            // given
            Long productId = 1L;
            Long sellerId = 1L;
            Product product = createProduct(productId, "상품", 10000L, 100, ProductStatus.SELLING, sellerId);
            ProductUpdateRequest request = createProductUpdateRequest(null, null, 0);

            given(productRepository.findById(productId)).willReturn(Optional.of(product));

            // when
            ProductResponse response = sellerProductService.updateProduct(productId, request, sellerId);

            // then
            assertThat(response.getStockQuantity()).isEqualTo(0);
            assertThat(response.getStatus()).isEqualTo(ProductStatus.SOLD_OUT);
        }
    }

    @Nested
    @DisplayName("deleteProduct 메서드")
    class DeleteProductTest {

        @Test
        @DisplayName("정상 상품 삭제 성공")
        void deleteProduct_Success() {
            // given
            Long productId = 1L;
            Long sellerId = 1L;
            Product product = createProduct(productId, "삭제할 상품", 10000L, 100, ProductStatus.SELLING, sellerId);

            given(productRepository.findById(productId)).willReturn(Optional.of(product));

            // when
            sellerProductService.deleteProduct(productId, sellerId);

            // then
            verify(productRepository).findById(productId);
            verify(productRepository).delete(product);
        }

        @Test
        @DisplayName("존재하지 않는 상품 삭제 시 PRODUCT_NOT_FOUND 예외")
        void deleteProduct_NotFound_ThrowsException() {
            // given
            Long productId = 999L;
            Long sellerId = 1L;

            given(productRepository.findById(productId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> sellerProductService.deleteProduct(productId, sellerId))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception -> {
                        BusinessException be = (BusinessException) exception;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
                    });

            verify(productRepository).findById(productId);
            verify(productRepository, never()).delete(any(Product.class));
        }

        @Test
        @DisplayName("다른 판매자 상품 삭제 시 ACCESS_DENIED 예외")
        void deleteProduct_NotOwner_ThrowsException() {
            // given
            Long productId = 1L;
            Long ownerId = 1L;
            Long attempterId = 2L;
            Product product = createProduct(productId, "상품", 10000L, 100, ProductStatus.SELLING, ownerId);

            given(productRepository.findById(productId)).willReturn(Optional.of(product));

            // when & then
            assertThatThrownBy(() -> sellerProductService.deleteProduct(productId, attempterId))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception -> {
                        BusinessException be = (BusinessException) exception;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED);
                    });

            verify(productRepository).findById(productId);
            verify(productRepository, never()).delete(any(Product.class));
        }
    }

    // ========== Helper Methods ==========

    private ProductCreateRequest createProductCreateRequest(String name, Long price, Integer stockQuantity) {
        return ProductCreateRequest.builder()
                .name(name)
                .price(price)
                .stockQuantity(stockQuantity)
                .build();
    }

    private ProductUpdateRequest createProductUpdateRequest(String name, Long price, Integer stockQuantity) {
        return ProductUpdateRequest.builder()
                .name(name)
                .price(price)
                .stockQuantity(stockQuantity)
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
}
