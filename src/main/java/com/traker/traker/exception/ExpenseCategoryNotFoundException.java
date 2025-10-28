package com.traker.traker.exception;

public class ExpenseCategoryNotFoundException extends NotFoundException {
    public ExpenseCategoryNotFoundException(Object id) {
        super("ID", id);
    }

    @Override
    public String getEntityClassName() {
        return "ExpenseCategory";
    }
}
