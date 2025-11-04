package com.traker.traker.exception;

public class ExpenseRecordNotFoundException extends RuntimeException {
    public ExpenseRecordNotFoundException(Long id) {
        super("Расход с идентификатором " + id + " не найден");
    }
}
