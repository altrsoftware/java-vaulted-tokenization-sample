package com.altr.exception;

public class ALTRException extends Exception {
    public ALTRException(String message) {
        super("ALTR Exception: " + message);
    }
}
