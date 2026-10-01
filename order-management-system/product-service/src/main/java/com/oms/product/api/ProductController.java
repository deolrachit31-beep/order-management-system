package com.oms.product.api;

import com.oms.product.api.dto.ProductDtos.CreateProductRequest;
import com.oms.product.api.dto.ProductDtos.ProductResponse;
import com.oms.product.api.dto.ProductDtos.StockChangeResponse;
import com.oms.product.api.dto.ProductDtos.StockQuantityRequest;
import com.oms.product.api.dto.ProductDtos.UpdateProductRequest;
import com.oms.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/products")
@Tag(name = "Products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    @Operation(summary = "Create a product")
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody CreateProductRequest request) {
        ProductResponse response = productService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a product, including price and stock")
    public ProductResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateProductRequest request) {
        return productService.update(id, request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a product")
    public ProductResponse get(@PathVariable UUID id) {
        return productService.get(id);
    }

    @PostMapping("/{id}/stock/decrement")
    @Operation(summary = "Check stock and decrement it when an order reserves the product")
    public StockChangeResponse decrement(@PathVariable UUID id, @Valid @RequestBody StockQuantityRequest request) {
        return productService.decrementStock(id, request.quantity());
    }

    @PostMapping("/{id}/stock/increment")
    @Operation(summary = "Restore stock when order creation is rolled back, or restock a product")
    public StockChangeResponse increment(@PathVariable UUID id, @Valid @RequestBody StockQuantityRequest request) {
        return productService.incrementStock(id, request.quantity());
    }
}
