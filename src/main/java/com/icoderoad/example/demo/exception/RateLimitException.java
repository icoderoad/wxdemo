package com.icoderoad.example.demo.exception;

public class RateLimitException extends RuntimeException {

    private static final long serialVersionUID = -4534748437953463440L;

	public RateLimitException(String message) {
        super(message);
    }
}
