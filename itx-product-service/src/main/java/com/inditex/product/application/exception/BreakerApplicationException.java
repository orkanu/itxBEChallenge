package com.inditex.product.application.exception;

public class BreakerApplicationException extends ApplicationException {
    public BreakerApplicationException(String message) { super(message); }
    public BreakerApplicationException(String message, Throwable cause) { super(message, cause); }
}