package com.oms.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.oms.product.domain.DomainExceptions.BadRequestException;
import com.oms.product.domain.DomainExceptions.InsufficientStockException;
import com.oms.product.domain.DomainExceptions.NotFoundException;
import com.oms.product.domain.Product;
import com.oms.product.repo.ProductRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductServiceStockTest {

    @Mock
    private ProductRepository productRepository;

    private ProductService productService;

    private final UUID productId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        productService = new ProductService(productRepository);
    }

    @Test
    void decrementReducesAvailableStock() {
        Product product = product(5);
        when(productRepository.findByIdForUpdate(productId)).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenReturn(product);

        var response = productService.decrementStock(productId, 2);

        assertThat(response.stockQuantity()).isEqualTo(3);
        assertThat(product.getStockQuantity()).isEqualTo(3);
        assertThat(response.price()).isEqualByComparingTo("12.50");
    }

    @Test
    void decrementCanTakeStockToZero() {
        Product product = product(2);
        when(productRepository.findByIdForUpdate(productId)).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenReturn(product);

        var response = productService.decrementStock(productId, 2);

        assertThat(response.stockQuantity()).isZero();
    }

    @Test
    void decrementRejectsInsufficientStockWithoutSaving() {
        Product product = product(1);
        when(productRepository.findByIdForUpdate(productId)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> productService.decrementStock(productId, 2))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("requested 2")
                .hasMessageContaining("available 1");
        assertThat(product.getStockQuantity()).isEqualTo(1);
        verify(productRepository, never()).save(any());
    }

    @Test
    void decrementRejectsNonPositiveQuantityBeforeLoadingTheProduct() {
        assertThatThrownBy(() -> productService.decrementStock(productId, 0))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Quantity must be positive");
        assertThatThrownBy(() -> productService.decrementStock(productId, -3))
                .isInstanceOf(BadRequestException.class);
        verify(productRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void decrementOfUnknownProductIsNotFound() {
        when(productRepository.findByIdForUpdate(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.decrementStock(productId, 1))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining(productId.toString());
        verify(productRepository, never()).save(any());
    }

    @Test
    void incrementRestoresStock() {
        Product product = product(3);
        when(productRepository.findByIdForUpdate(productId)).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenReturn(product);

        var response = productService.incrementStock(productId, 2);

        assertThat(response.stockQuantity()).isEqualTo(5);
    }

    @Test
    void incrementRejectsNonPositiveQuantity() {
        assertThatThrownBy(() -> productService.incrementStock(productId, 0))
                .isInstanceOf(BadRequestException.class);
        verify(productRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void incrementRejectsStockAboveTheCap() {
        Product product = product(Product.MAX_STOCK);
        when(productRepository.findByIdForUpdate(productId)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> productService.incrementStock(productId, 1))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("1000000");
        verify(productRepository, never()).save(any());
    }

    private Product product(int stock) {
        Product product = new Product();
        product.setId(productId);
        product.setName("Notebook");
        product.setPrice(new BigDecimal("12.50"));
        product.setStockQuantity(stock);
        return product;
    }
}
