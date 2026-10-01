package com.oms.order.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oms.order.api.dto.OrderDtos.ProductStockResponse;
import com.oms.order.domain.DomainExceptions.BadRequestException;
import com.oms.order.domain.DomainExceptions.ConflictException;
import com.oms.order.domain.DomainExceptions.NotFoundException;
import com.oms.order.domain.DomainExceptions.ServiceUnavailableException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class ProductStockClient {

    private static final Logger log = LoggerFactory.getLogger(ProductStockClient.class);

    private final RestClient productRestClient;
    private final ObjectMapper objectMapper;

    public ProductStockClient(RestClient productRestClient, ObjectMapper objectMapper) {
        this.productRestClient = productRestClient;
        this.objectMapper = objectMapper;
    }

    public ProductStockResponse decrement(UUID productId, int quantity) {
        return change("/api/products/{id}/stock/decrement", productId, quantity);
    }

    public void increment(UUID productId, int quantity) {
        change("/api/products/{id}/stock/increment", productId, quantity);
    }

    private ProductStockResponse change(String path, UUID productId, int quantity) {
        try {
            ProductStockResponse body = productRestClient.post()
                    .uri(path, productId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new StockQuantityRequest(quantity))
                    .retrieve()
                    .onStatus(status -> status.value() == 400, (request, response) -> {
                        throw new BadRequestException(readMessage(response.getBody().readAllBytes(), "Invalid stock request"));
                    })
                    .onStatus(status -> status.value() == 404, (request, response) -> {
                        throw new NotFoundException("Product not found: " + productId);
                    })
                    .onStatus(status -> status.value() == 409, (request, response) -> {
                        throw new ConflictException(readMessage(
                                response.getBody().readAllBytes(),
                                "Insufficient stock for product " + productId));
                    })
                    .onStatus(status -> status.isError(), (request, response) -> {
                        throw new ServiceUnavailableException(
                                "Product service returned " + response.getStatusCode().value(), null);
                    })
                    .body(ProductStockResponse.class);
            if (body == null) {
                throw new ServiceUnavailableException("Product service returned an empty body", null);
            }
            return body;
        } catch (BadRequestException | NotFoundException | ConflictException ex) {
            throw ex;
        } catch (RestClientResponseException ex) {
            throw mapStatus(ex, productId);
        } catch (RestClientException ex) {
            log.error("Product service call failed for product {}", productId, ex);
            throw new ServiceUnavailableException("Product service is unavailable", ex);
        }
    }

    private RuntimeException mapStatus(RestClientResponseException ex, UUID productId) {
        int status = ex.getStatusCode().value();
        if (status == 404) {
            return new NotFoundException("Product not found: " + productId);
        }
        if (status == 409) {
            return new ConflictException(readMessage(ex.getResponseBodyAsByteArray(), "Insufficient stock for product " + productId));
        }
        if (status == 400) {
            return new BadRequestException(readMessage(ex.getResponseBodyAsByteArray(), "Invalid stock request"));
        }
        return new ServiceUnavailableException("Product service returned " + status, ex);
    }

    private String readMessage(byte[] body, String fallback) {
        if (body == null || body.length == 0) {
            return fallback;
        }
        try {
            JsonNode node = objectMapper.readTree(body);
            if (node.hasNonNull("message")) {
                return node.get("message").asText();
            }
        } catch (IOException ex) {
            log.debug("Could not parse product service error body: {}", new String(body, StandardCharsets.UTF_8));
        }
        return fallback;
    }

    private record StockQuantityRequest(int quantity) {
    }
}
