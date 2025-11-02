package com.traker.traker.service;

import com.traker.traker.dto.income.IncomeCategoryDto;
import com.traker.traker.entity.IncomeCategory;
import com.traker.traker.entity.User;
import com.traker.traker.exception.CategoryInUseException;
import com.traker.traker.exception.IncomeCategoryNotFoundException;
import com.traker.traker.mapper.IncomeCategoryMapper;
import com.traker.traker.repository.IncomeCategoryRepository;
import com.traker.traker.repository.IncomeRecordRepository;
import com.traker.traker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IncomeCategoryService {

    private final IncomeCategoryRepository incomeCategoryRepository;
    private final IncomeRecordRepository incomeRecordRepository;
    private final IncomeCategoryMapper incomeCategoryMapper;
    private final UserRepository userRepository;

    public IncomeCategoryDto createCategory(IncomeCategoryDto dto) {
        User currentUser = getCurrentUser();
        validateUniqueName(dto.getName(), null, currentUser);
        IncomeCategory category = incomeCategoryMapper.toEntity(dto);
        category.setUser(currentUser);
        IncomeCategory saved = incomeCategoryRepository.save(category);
        return incomeCategoryMapper.toDto(saved);
    }

    public IncomeCategoryDto updateCategory(Long id, IncomeCategoryDto dto) {
        User currentUser = getCurrentUser();
        IncomeCategory existing = incomeCategoryRepository.findByIdAndUser(id, currentUser)
                .orElseThrow(() -> new IncomeCategoryNotFoundException(id));
        validateUniqueName(dto.getName(), id, currentUser);
        incomeCategoryMapper.updateEntityFromDto(dto, existing);
        IncomeCategory saved = incomeCategoryRepository.save(existing);
        return incomeCategoryMapper.toDto(saved);
    }

    public List<IncomeCategoryDto> getCategories() {
        User currentUser = getCurrentUser();
        return incomeCategoryRepository.findByUser(currentUser).stream()
                .map(incomeCategoryMapper::toDto)
                .collect(Collectors.toList());
    }

    public void deleteCategory(Long id) {
        User currentUser = getCurrentUser();
        IncomeCategory category = incomeCategoryRepository.findByIdAndUser(id, currentUser)
                .orElseThrow(() -> new IncomeCategoryNotFoundException(id));
        if (incomeRecordRepository.existsByUserAndCategory_Id(currentUser, id)) {
            throw new CategoryInUseException("Нельзя удалить категорию с привязанными доходами. Перенесите или удалите записи.");
        }
        incomeCategoryRepository.delete(category);
    }

    public IncomeCategory getCategory(Long id) {
        User currentUser = getCurrentUser();
        return incomeCategoryRepository.findByIdAndUser(id, currentUser)
                .orElseThrow(() -> new IncomeCategoryNotFoundException(id));
    }

    private void validateUniqueName(String name, Long currentId, User user) {
        if (name == null) {
            return;
        }
        incomeCategoryRepository.findByUserAndNameIgnoreCase(user, name)
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
