package com.oms.product.service;

import com.oms.product.api.dto.ProductDtos.CreateProductRequest;
import com.oms.product.api.dto.ProductDtos.ProductResponse;
import com.oms.product.api.dto.ProductDtos.StockChangeResponse;
import com.oms.product.api.dto.ProductDtos.UpdateProductRequest;
import com.oms.product.domain.DomainExceptions.BadRequestException;
import com.oms.product.domain.DomainExceptions.InsufficientStockException;
import com.oms.product.domain.DomainExceptions.NotFoundException;
import com.oms.product.domain.Product;
import com.oms.product.repo.ProductRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional
    public ProductResponse create(CreateProductRequest request) {
        Product product = new Product();
        apply(product, request.name(), request.description(), request.price(), request.stockQuantity());
        Product saved = productRepository.save(product);
        log.info("Created product {} with stock {}", saved.getId(), saved.getStockQuantity());
        return ProductResponse.from(saved);
    }

    @Transactional
    public ProductResponse update(UUID id, UpdateProductRequest request) {
        Product product = productRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException("Product not found: " + id));
        apply(product, request.name(), request.description(), request.price(), request.stockQuantity());
        Product saved = productRepository.save(product);
        log.info("Updated product {}", saved.getId());
        return ProductResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public ProductResponse get(UUID id) {
        return productRepository.findById(id)
                .map(ProductResponse::from)
                .orElseThrow(() -> new NotFoundException("Product not found: " + id));
    }

    @Transactional
    public StockChangeResponse decrementStock(UUID productId, int quantity) {
        if (quantity <= 0) {
            throw new BadRequestException("Quantity must be positive");
        }
        Product product = lock(productId);
        if (product.getStockQuantity() < quantity) {
            throw new InsufficientStockException(
                    "Insufficient stock for product " + productId
                            + ": requested " + quantity
                            + ", available " + product.getStockQuantity());
        }
        product.setStockQuantity(product.getStockQuantity() - quantity);
        Product saved = productRepository.save(product);
        log.info("Decremented stock for product {} by {}. Remaining {}", productId, quantity, saved.getStockQuantity());
        return StockChangeResponse.from(saved);
    }

    @Transactional
    public StockChangeResponse incrementStock(UUID productId, int quantity) {
        if (quantity <= 0) {
            throw new BadRequestException("Quantity must be positive");
        }
        Product product = lock(productId);
        int updated = product.getStockQuantity() + quantity;
        if (updated > Product.MAX_STOCK) {
            throw new BadRequestException("Stock cannot exceed " + Product.MAX_STOCK);
        }
        product.setStockQuantity(updated);
        Product saved = productRepository.save(product);
        log.info("Incremented stock for product {} by {}. Remaining {}", productId, quantity, saved.getStockQuantity());
        return StockChangeResponse.from(saved);
    }

    private Product lock(UUID productId) {
        return productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new NotFoundException("Product not found: " + productId));
    }

    private static void apply(Product product, String name, String description, BigDecimal price, int stockQuantity) {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Price must be positive");
        }
        if (stockQuantity < 0 || stockQuantity > Product.MAX_STOCK) {
            throw new BadRequestException("Stock must be between 0 and " + Product.MAX_STOCK);
        }
        product.setName(name.trim());
        product.setDescription(normalizeDescription(description));
        product.setPrice(price.setScale(2, RoundingMode.HALF_UP));
        product.setStockQuantity(stockQuantity);
    }

    private static String normalizeDescription(String description) {
        if (description == null) {
            return null;
        }
        String trimmed = description.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
