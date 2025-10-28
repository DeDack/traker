package com.traker.traker.service;

import com.traker.traker.dto.expense.ExpenseCategoryDto;
import com.traker.traker.entity.ExpenseCategory;
import com.traker.traker.entity.User;
import com.traker.traker.exception.ExpenseCategoryNotFoundException;
import com.traker.traker.mapper.ExpenseCategoryMapper;
import com.traker.traker.repository.ExpenseCategoryRepository;
import com.traker.traker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExpenseCategoryService {

    private final ExpenseCategoryRepository expenseCategoryRepository;
    private final ExpenseCategoryMapper expenseCategoryMapper;
    private final UserRepository userRepository;

    public ExpenseCategoryDto createCategory(ExpenseCategoryDto dto) {
        User currentUser = getCurrentUser();
        validateUniqueName(dto.getName(), null, currentUser);
        ExpenseCategory category = expenseCategoryMapper.toEntity(dto);
        category.setUser(currentUser);
        ExpenseCategory saved = expenseCategoryRepository.save(category);
        return expenseCategoryMapper.toDto(saved);
    }

    public ExpenseCategoryDto updateCategory(Long id, ExpenseCategoryDto dto) {
        User currentUser = getCurrentUser();
        ExpenseCategory existing = expenseCategoryRepository.findByIdAndUser(id, currentUser)
                .orElseThrow(() -> new ExpenseCategoryNotFoundException(id));
        validateUniqueName(dto.getName(), id, currentUser);
        expenseCategoryMapper.updateEntityFromDto(dto, existing);
        ExpenseCategory saved = expenseCategoryRepository.save(existing);
        return expenseCategoryMapper.toDto(saved);
    }

    public List<ExpenseCategoryDto> getCategories() {
        User currentUser = getCurrentUser();
        return expenseCategoryRepository.findByUser(currentUser).stream()
                .map(expenseCategoryMapper::toDto)
                .collect(Collectors.toList());
    }

    public void deleteCategory(Long id) {
        User currentUser = getCurrentUser();
        ExpenseCategory category = expenseCategoryRepository.findByIdAndUser(id, currentUser)
                .orElseThrow(() -> new ExpenseCategoryNotFoundException(id));
        expenseCategoryRepository.delete(category);
    }

    public ExpenseCategory getCategory(Long id) {
        User currentUser = getCurrentUser();
        return expenseCategoryRepository.findByIdAndUser(id, currentUser)
                .orElseThrow(() -> new ExpenseCategoryNotFoundException(id));
    }

    private void validateUniqueName(String name, Long currentId, User user) {
        if (name == null) {
            return;
        }
        expenseCategoryRepository.findByUserAndNameIgnoreCase(user, name)
                .ifPresent(existing -> {
                    if (currentId == null || !existing.getId().equals(currentId)) {
                        throw new IllegalArgumentException("Категория с таким названием уже существует");
                    }
                });
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Пользователь не найден: " + username));
    }
}
