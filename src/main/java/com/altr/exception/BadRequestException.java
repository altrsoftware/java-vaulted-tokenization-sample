package com.altr.exception;

public class BadRequestException extends ALTRException {
    public BadRequestException() {
        super("Bad request. The server could not understand the request due to invalid syntax.");
    }
}
