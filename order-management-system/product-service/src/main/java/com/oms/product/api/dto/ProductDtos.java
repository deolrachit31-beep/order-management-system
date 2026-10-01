package com.oms.product.api.dto;

import com.oms.product.domain.Product;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class ProductDtos {

    private ProductDtos() {
    }

    public record CreateProductRequest(
            @NotBlank @Size(max = 200) String name,
            @Size(max = 2000) String description,
            @NotNull @DecimalMin(value = "0.01") @Digits(integer = 10, fraction = 2) BigDecimal price,
            @NotNull @Min(0) @Max(Product.MAX_STOCK) Integer stockQuantity) {
    }

    public record UpdateProductRequest(
            @NotBlank @Size(max = 200) String name,
            @Size(max = 2000) String description,
            @NotNull @DecimalMin(value = "0.01") @Digits(integer = 10, fraction = 2) BigDecimal price,
            @NotNull @Min(0) @Max(Product.MAX_STOCK) Integer stockQuantity) {
    }

    public record StockQuantityRequest(
            @NotNull @Positive @Max(1000) Integer quantity) {
    }

    public record ProductResponse(
            UUID id,
            String name,
            String description,
            BigDecimal price,
            int stockQuantity,
            Instant createdAt,
            Instant updatedAt) {

        public static ProductResponse from(Product product) {
            return new ProductResponse(
                    product.getId(),
                    product.getName(),
                    product.getDescription(),
                    product.getPrice(),
                    product.getStockQuantity(),
                    product.getCreatedAt(),
                    product.getUpdatedAt());
        }
    }

    public record StockChangeResponse(
            UUID productId,
            String name,
            BigDecimal price,
            int stockQuantity) {

        public static StockChangeResponse from(Product product) {
            return new StockChangeResponse(
                    product.getId(), product.getName(), product.getPrice(), product.getStockQuantity());
        }
    }
}
