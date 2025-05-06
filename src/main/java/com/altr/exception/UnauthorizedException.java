package com.altr.exception;

public class UnauthorizedException extends ALTRException {
    public UnauthorizedException() {
        super("Unauthorized access. Please check your credentials.");
    }
}
