package org.example.domain.product.service;

import lombok.RequiredArgsConstructor;
import org.example.domain.product.dto.request.ProductCreateRequest;
import org.example.domain.product.dto.request.ProductUpdateRequest;
import org.example.domain.product.dto.response.ProductResponse;
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

/**
 * 판매자 상품 관리 서비스 구현체
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerProductServiceImpl implements SellerProductService {

    private final ProductRepository productRepository;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public ProductResponse createProduct(ProductCreateRequest request, Long sellerId) {
        Product product = request.toEntity(sellerId);
        Product savedProduct = productRepository.save(product);
        return ProductResponse.from(savedProduct);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PagingResponse<ProductResponse> getMyProducts(Long sellerId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Product> productPage = productRepository.findBySellerId(sellerId, pageRequest);

        List<ProductResponse> content = productPage.getContent().stream()
                .map(ProductResponse::from)
                .toList();

        PagingInfo pagingInfo = new PagingInfo(
                productPage.getNumber(),
                productPage.getSize(),
                productPage.getTotalElements()
        );

        return new PagingResponse<>(pagingInfo, content);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public ProductResponse updateProduct(Long productId, ProductUpdateRequest request, Long sellerId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        validateOwnership(product, sellerId);

        product.updateInfo(request.getName(), request.getPrice(), request.getStockQuantity());
        return ProductResponse.from(product);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void deleteProduct(Long productId, Long sellerId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        validateOwnership(product, sellerId);

        productRepository.delete(product);
    }

    /**
     * 상품 소유권 검증
     *
     * @param product  검증할 상품
     * @param sellerId 판매자 ID
     * @throws BusinessException ACCESS_DENIED - 본인 상품이 아닐 경우
     */
    private void validateOwnership(Product product, Long sellerId) {
        if (!product.getSellerId().equals(sellerId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }
}
