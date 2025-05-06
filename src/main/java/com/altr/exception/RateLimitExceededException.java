package com.altr.exception;

public class RateLimitExceededException extends ALTRException {
    public RateLimitExceededException() {
        super("Rate limit exceeded. Please wait before making more requests.");
    }
}
