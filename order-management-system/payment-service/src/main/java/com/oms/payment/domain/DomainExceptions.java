package com.oms.payment.domain;

public final class DomainExceptions {

    private DomainExceptions() {
    }

    public static class NotFoundException extends RuntimeException {
        public NotFoundException(String message) {
            super(message);
        }
    }

    public static class ServiceUnavailableException extends RuntimeException {
        public ServiceUnavailableException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
