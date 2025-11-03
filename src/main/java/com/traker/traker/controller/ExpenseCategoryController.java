package com.traker.traker.controller;

import com.traker.traker.controller.api.ExpenseCategoryControllerApi;
import com.traker.traker.dto.expense.ExpenseCategoryDto;
import com.traker.traker.service.ExpenseCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ExpenseCategoryController implements ExpenseCategoryControllerApi {

    private final ExpenseCategoryService expenseCategoryService;

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ExpenseCategoryDto> createCategory(@RequestBody @Valid ExpenseCategoryDto dto) {
        return ResponseEntity.ok(expenseCategoryService.createCategory(dto));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ExpenseCategoryDto> updateCategory(@PathVariable Long id,
                                                             @RequestBody @Valid ExpenseCategoryDto dto) {
        return ResponseEntity.ok(expenseCategoryService.updateCategory(id, dto));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        expenseCategoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ExpenseCategoryDto>> getCategories() {
        return ResponseEntity.ok(expenseCategoryService.getCategories());
    }
}
