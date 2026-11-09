package com.pdms.exception;

public class EmailDispatchException extends RuntimeException {

    public EmailDispatchException(String message) {
        super(message);
    }

    public EmailDispatchException(String message, Throwable cause) {
        super(message, cause);
    }
}
