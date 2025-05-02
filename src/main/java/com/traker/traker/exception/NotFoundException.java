package com.traker.traker.exception;

import java.util.NoSuchElementException;

public abstract class NotFoundException extends NoSuchElementException {
    public final Object id;
    public final String attributeName;

    public NotFoundException(String attributeName, Object attributeValue) {
        this.attributeName = attributeName;
        this.id = attributeValue;
    }

    public NotFoundException(Object id) {
        this.attributeName = "ID";
        this.id = id;
    }

    public String getMessage() {
        return "Объект \"" + getEntityClassName()
               + "\" с " + attributeName + " = \"" + id.toString()
               + "\" не найден.";
    }

    public abstract String getEntityClassName();
}
