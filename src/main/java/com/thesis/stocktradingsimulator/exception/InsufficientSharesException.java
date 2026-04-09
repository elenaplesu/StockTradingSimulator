package com.thesis.stocktradingsimulator.exception;

public class InsufficientSharesException extends RuntimeException {
    public InsufficientSharesException(String message) { super(message); }
}