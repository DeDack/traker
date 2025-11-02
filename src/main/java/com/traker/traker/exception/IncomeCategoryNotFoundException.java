package com.traker.traker.exception;

public class IncomeCategoryNotFoundException extends RuntimeException {
    public IncomeCategoryNotFoundException(Long id) {
        super("Категория доходов не найдена: " + id);
    }
}
