package com.oms.order.domain;

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

    public static class InvalidStatusTransitionException extends ConflictException {
        public InvalidStatusTransitionException(String message) {
            super(message);
        }
    }

    public static class ServiceUnavailableException extends RuntimeException {
        public ServiceUnavailableException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
