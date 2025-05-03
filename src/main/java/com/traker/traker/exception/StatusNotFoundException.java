package com.traker.traker.exception;

public class StatusNotFoundException extends NotFoundException {
    public StatusNotFoundException(Object id) {
        super("ID", id);
    }

    @Override
    public String getEntityClassName() {
        return "Status";
    }
}