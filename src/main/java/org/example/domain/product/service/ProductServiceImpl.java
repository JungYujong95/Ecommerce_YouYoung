package org.example.domain.product.service;

import lombok.RequiredArgsConstructor;
import org.example.domain.product.dto.response.ProductResponse;
import org.example.domain.product.entity.Product;
import org.example.domain.product.entity.ProductStatus;
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
 * 상품 조회 서비스 구현체
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    /**
     * {@inheritDoc}
     */
    @Override
    public ProductResponse getProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        return ProductResponse.from(product);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PagingResponse<ProductResponse> getProducts(int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Product> productPage = productRepository.findByStatusIn(
                List.of(ProductStatus.SELLING, ProductStatus.SOLD_OUT), pageRequest);

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
}
