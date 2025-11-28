package com.hyperativa.exception;

public class DuplicateCardException extends RuntimeException {
    public DuplicateCardException(String message) {
        super(message);
    }

    public DuplicateCardException(String message, Throwable cause) {
        super(message, cause);
    }
}
