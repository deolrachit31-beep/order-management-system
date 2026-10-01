package com.oms.product.domain;

public final class DomainExceptions {

    private DomainExceptions() {
    }

    public static class NotFoundException extends RuntimeException {
        public NotFoundException(String message) {
            super(message);
        }
    }

    public static class ConflictException extends RuntimeException {
        public ConflictException(String message) {
            super(message);
        }
    }

    public static class BadRequestException extends RuntimeException {
        public BadRequestException(String message) {
            super(message);
        }
    }

    public static class InsufficientStockException extends ConflictException {
        public InsufficientStockException(String message) {
            super(message);
        }
    }
}
