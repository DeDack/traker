package com.traker.traker.exception;

public class IncomeRecordNotFoundException extends RuntimeException {
    public IncomeRecordNotFoundException(Long id) {
        super("Доход с идентификатором " + id + " не найден");
    }
}
