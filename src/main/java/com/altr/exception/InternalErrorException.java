package com.altr.exception;

public class InternalErrorException extends ALTRException {
    public InternalErrorException() {
        super("Internal server error. Please try again later.");
    }
    
}
