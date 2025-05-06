package com.altr.exception;

public class RetriesExhaustedException extends ALTRException {
    public RetriesExhaustedException() {
        super("Retries exhausted. The operation could not be completed after multiple attempts.");
    }
}
