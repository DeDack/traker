package com.traker.traker.controller;

import com.traker.traker.controller.api.IncomeCategoryControllerApi;
import com.traker.traker.dto.income.IncomeCategoryDto;
import com.traker.traker.service.IncomeCategoryService;
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
public class IncomeCategoryController implements IncomeCategoryControllerApi {

    private final IncomeCategoryService incomeCategoryService;

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<IncomeCategoryDto> createCategory(@RequestBody @Valid IncomeCategoryDto dto) {
        return ResponseEntity.ok(incomeCategoryService.createCategory(dto));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<IncomeCategoryDto> updateCategory(@PathVariable Long id,
                                                            @RequestBody @Valid IncomeCategoryDto dto) {
        return ResponseEntity.ok(incomeCategoryService.updateCategory(id, dto));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        incomeCategoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<IncomeCategoryDto>> getCategories() {
        return ResponseEntity.ok(incomeCategoryService.getCategories());
    }
}
