package com.traker.traker.service;

import com.traker.traker.dto.expense.ExpenseBatchCreateRequestDto;
import com.traker.traker.dto.expense.ExpenseBatchUpdateRequestDto;
import com.traker.traker.dto.expense.ExpenseRecordRequestDto;
import com.traker.traker.dto.expense.ExpenseRecordResponseDto;
import com.traker.traker.dto.expense.ExpenseRecordUpdateDto;
import com.traker.traker.dto.expense.ExpenseSummaryDto;
import com.traker.traker.entity.ExpenseCategory;
import com.traker.traker.entity.ExpenseRecord;
import com.traker.traker.entity.User;
import com.traker.traker.exception.ExpenseCategoryNotFoundException;
import com.traker.traker.exception.ExpenseRecordNotFoundException;
import com.traker.traker.mapper.ExpenseRecordMapper;
import com.traker.traker.repository.ExpenseCategoryRepository;
import com.traker.traker.repository.ExpenseRecordRepository;
import com.traker.traker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.traker.traker.utils.FinanceUtils.FinanceFilter;
import static com.traker.traker.utils.FinanceUtils.buildFilter;
import static com.traker.traker.utils.FinanceUtils.formatPeriod;
import static com.traker.traker.utils.FinanceUtils.normalizeAmount;
import static com.traker.traker.utils.FinanceUtils.parseOptionalPeriod;
import static com.traker.traker.utils.FinanceUtils.parsePeriod;

@Service
@RequiredArgsConstructor
public class ExpenseRecordService {

    private final ExpenseRecordRepository expenseRecordRepository;
    private final ExpenseCategoryRepository expenseCategoryRepository;
    private final ExpenseRecordMapper expenseRecordMapper;
    private final UserRepository userRepository;

    @Transactional
    public List<ExpenseRecordResponseDto> createBatch(ExpenseBatchCreateRequestDto request) {
        User currentUser = getCurrentUser();
        YearMonth defaultPeriod = parseOptionalPeriod(request.getDefaultPeriod());

        List<ExpenseRecord> records = request.getExpenses().stream()
                .map(dto -> mapToEntity(dto, defaultPeriod, currentUser))
                .collect(Collectors.toList());

        List<ExpenseRecord> saved = expenseRecordRepository.saveAll(records);
        return expenseRecordMapper.toDtoList(saved);
    }

    @Transactional
    public ExpenseRecordResponseDto updateExpense(Long id, ExpenseRecordRequestDto request) {
        User currentUser = getCurrentUser();
        ExpenseRecord record = expenseRecordRepository.findByIdAndUser(id, currentUser)
                .orElseThrow(() -> new ExpenseRecordNotFoundException(id));

        applyFullUpdate(record, request, currentUser);
        ExpenseRecord saved = expenseRecordRepository.save(record);
        return expenseRecordMapper.toDto(saved);
    }

    @Transactional
    public List<ExpenseRecordResponseDto> updateExpenses(ExpenseBatchUpdateRequestDto request) {
        User currentUser = getCurrentUser();
        List<ExpenseRecordUpdateDto> updates = request.getRecords();
        if (updates == null || updates.isEmpty()) {
            return List.of();
        }

        Set<Long> ids = updates.stream()
                .map(ExpenseRecordUpdateDto::getId)
                .collect(Collectors.toSet());

        Map<Long, ExpenseRecord> existing = expenseRecordRepository.findByUserAndIdIn(currentUser, new ArrayList<>(ids)).stream()
                .collect(Collectors.toMap(ExpenseRecord::getId, Function.identity()));

        for (ExpenseRecordUpdateDto updateDto : updates) {
            ExpenseRecord record = existing.get(updateDto.getId());
            if (record == null) {
                throw new ExpenseRecordNotFoundException(updateDto.getId());
            }
            applyPartialUpdate(record, updateDto, currentUser);
        }

        expenseRecordRepository.saveAll(existing.values());
        return updates.stream()
                .map(update -> expenseRecordMapper.toDto(existing.get(update.getId())))
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteExpense(Long id) {
        User currentUser = getCurrentUser();
        ExpenseRecord record = expenseRecordRepository.findByIdAndUser(id, currentUser)
                .orElseThrow(() -> new ExpenseRecordNotFoundException(id));
        expenseRecordRepository.delete(record);
    }

    @Transactional
    public void deleteExpenses(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        User currentUser = getCurrentUser();
        List<ExpenseRecord> records = expenseRecordRepository.findByUserAndIdIn(currentUser, ids);
        long requested = ids.stream().distinct().count();
        if (records.size() != requested) {
            Set<Long> foundIds = records.stream().map(ExpenseRecord::getId).collect(Collectors.toSet());
            Long missing = ids.stream().filter(id -> !foundIds.contains(id)).findFirst().orElse(null);
            throw new ExpenseRecordNotFoundException(missing);
        }
        expenseRecordRepository.deleteAll(records);
    }

    @Transactional(readOnly = true)
    public List<ExpenseRecordResponseDto> getExpenses(String fromDate, String toDate, String month, List<Long> categoryIds) {
        FinanceFilter filter = buildFilter(fromDate, toDate, month);
        User currentUser = getCurrentUser();
        List<ExpenseRecord> records = expenseRecordRepository.findByUserAndFilter(
                currentUser,
                filter.fromDate(),
                filter.toDate(),
                filter.fromPeriod(),
                filter.toPeriod(),
                normalizeCategoryFilter(categoryIds));
        return expenseRecordMapper.toDtoList(records);
    }

    @Transactional(readOnly = true)
    public ExpenseSummaryDto getSummary(String fromDate, String toDate, String month, List<Long> categoryIds) {
        FinanceFilter filter = buildFilter(fromDate, toDate, month);
        User currentUser = getCurrentUser();

        List<ExpenseRecord> records = expenseRecordRepository.findByUserAndFilter(
                currentUser,
                filter.fromDate(),
                filter.toDate(),
                filter.fromPeriod(),
                filter.toPeriod(),
                normalizeCategoryFilter(categoryIds));

        Map<LocalDate, BigDecimal> totalsByPeriod = new LinkedHashMap<>();
        Map<Long, CategoryAggregation> categoryAggregations = new LinkedHashMap<>();

        for (ExpenseRecord record : records) {
            BigDecimal amount = normalizeAmount(record.getAmount());
            totalsByPeriod.merge(record.getPeriod(), amount, BigDecimal::add);

            Long categoryId = record.getCategory().getId();
            CategoryAggregation aggregation = categoryAggregations.computeIfAbsent(
                    categoryId,
                    id -> new CategoryAggregation(categoryId, record.getCategory().getName()));
            aggregation.addAmount(record.getPeriod(), amount);
        }

        BigDecimal totalAmount = records.stream()
                .map(ExpenseRecord::getAmount)
                .map(amount -> normalizeAmount(amount))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        ExpenseSummaryDto summaryDto = new ExpenseSummaryDto();
        BigDecimal normalizedTotal = normalizeAmount(totalAmount);
        summaryDto.setTotalAmount(normalizedTotal);

        summaryDto.setTotalsByCategory(categoryAggregations.values().stream()
                .sorted(Comparator.comparing(CategoryAggregation::getCategoryName, String.CASE_INSENSITIVE_ORDER))
                .map(aggregation -> new ExpenseSummaryDto.CategoryTotalDto(
                        aggregation.getCategoryId(),
                        aggregation.getCategoryName(),
                        normalizeAmount(aggregation.getTotal()),
                        calculatePercentage(normalizeAmount(aggregation.getTotal()), normalizedTotal)))
                .collect(Collectors.toList()));

        summaryDto.setTotalsByMonth(totalsByPeriod.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new ExpenseSummaryDto.MonthlyTotalDto(
                        formatPeriod(entry.getKey()),
                        normalizeAmount(entry.getValue())))
                .collect(Collectors.toList()));

        summaryDto.setCategoryMonthlyTotals(buildCategoryMonthlySummary(categoryAggregations));
        return summaryDto;
    }

    private BigDecimal calculatePercentage(BigDecimal amount, BigDecimal total) {
        if (total == null || total.compareTo(BigDecimal.ZERO) == 0) {
            return normalizeAmount(BigDecimal.ZERO);
        }
        return amount.multiply(BigDecimal.valueOf(100)).divide(total, 2, RoundingMode.HALF_UP);
    }

    private List<Long> normalizeCategoryFilter(List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return null;
        }
        List<Long> distinct = categoryIds.stream()
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        return distinct.isEmpty() ? null : distinct;
    }

    private List<ExpenseSummaryDto.CategoryMonthlySummaryDto> buildCategoryMonthlySummary(Map<Long, CategoryAggregation> aggregations) {
        return aggregations.values().stream()
                .sorted(Comparator.comparing(CategoryAggregation::getCategoryName, String.CASE_INSENSITIVE_ORDER))
                .map(aggregation -> {
                    List<ExpenseSummaryDto.MonthlyTotalDto> monthlyTotals = aggregation.getMonthlyTotals().entrySet().stream()
                            .sorted(Map.Entry.comparingByKey())
                            .map(entry -> new ExpenseSummaryDto.MonthlyTotalDto(
                                    formatPeriod(entry.getKey()),
                                    normalizeAmount(entry.getValue())))
                            .collect(Collectors.toList());
                    return new ExpenseSummaryDto.CategoryMonthlySummaryDto(
                            aggregation.getCategoryId(),
                            aggregation.getCategoryName(),
                            monthlyTotals);
                })
                .collect(Collectors.toList());
    }

    private static class CategoryAggregation {
        private final Long categoryId;
        private final String categoryName;
        private BigDecimal total = BigDecimal.ZERO;
        private final Map<LocalDate, BigDecimal> monthlyTotals = new LinkedHashMap<>();

        CategoryAggregation(Long categoryId, String categoryName) {
            this.categoryId = categoryId;
            this.categoryName = categoryName;
        }

        void addAmount(LocalDate period, BigDecimal amount) {
            total = total.add(amount);
            monthlyTotals.merge(period, amount, BigDecimal::add);
        }

        Long getCategoryId() {
            return categoryId;
        }

        String getCategoryName() {
            return categoryName;
        }

        BigDecimal getTotal() {
            return total;
        }

        Map<LocalDate, BigDecimal> getMonthlyTotals() {
            return monthlyTotals;
        }
    }

    private ExpenseRecord mapToEntity(ExpenseRecordRequestDto dto, YearMonth defaultPeriod, User currentUser) {
        ExpenseCategory category = expenseCategoryRepository.findByIdAndUser(dto.getCategoryId(), currentUser)
                .orElseThrow(() -> new ExpenseCategoryNotFoundException(dto.getCategoryId()));

        LocalDate expenseDate = dto.getExpenseDate();
        YearMonth period = resolvePeriod(dto, defaultPeriod, expenseDate);

        ExpenseRecord record = new ExpenseRecord();
        record.setUser(currentUser);
        record.setCategory(category);
        record.setTitle(dto.getTitle().trim());
        record.setDescription(normalizeDescription(dto.getDescription()));
        record.setAmount(normalizeAmount(dto.getAmount()));
        record.setPeriod(period.atDay(1));
        record.setExpenseDate(expenseDate);
        return record;
    }

    private void applyFullUpdate(ExpenseRecord record, ExpenseRecordRequestDto dto, User currentUser) {
        ExpenseCategory category = expenseCategoryRepository.findByIdAndUser(dto.getCategoryId(), currentUser)
                .orElseThrow(() -> new ExpenseCategoryNotFoundException(dto.getCategoryId()));

        LocalDate expenseDate = dto.getExpenseDate();
        YearMonth period = resolvePeriod(dto, null, expenseDate);

        record.setCategory(category);
        record.setTitle(dto.getTitle().trim());
        record.setDescription(normalizeDescription(dto.getDescription()));
        record.setAmount(normalizeAmount(dto.getAmount()));
        record.setExpenseDate(expenseDate);
        record.setPeriod(period.atDay(1));
    }

    private void applyPartialUpdate(ExpenseRecord record, ExpenseRecordUpdateDto dto, User currentUser) {
        if (dto.getTitle() != null) {
            if (dto.getTitle().isBlank()) {
                throw new IllegalArgumentException("Название траты не может быть пустым");
            }
            record.setTitle(dto.getTitle().trim());
        }

        if (dto.getDescription() != null) {
            record.setDescription(normalizeDescription(dto.getDescription()));
        }

        if (dto.getAmount() != null) {
            record.setAmount(normalizeAmount(dto.getAmount()));
        }

        if (dto.getCategoryId() != null) {
            ExpenseCategory category = expenseCategoryRepository.findByIdAndUser(dto.getCategoryId(), currentUser)
                    .orElseThrow(() -> new ExpenseCategoryNotFoundException(dto.getCategoryId()));
            record.setCategory(category);
        }

        if (dto.getExpenseDate() != null) {
            LocalDate expenseDate = dto.getExpenseDate();
            record.setExpenseDate(expenseDate);
            record.setPeriod(expenseDate.withDayOfMonth(1));
        } else if (dto.getPeriod() != null) {
            if (dto.getPeriod().isBlank()) {
                throw new IllegalArgumentException("Период должен быть указан в формате yyyy-MM");
            }
            YearMonth period = parsePeriod(dto.getPeriod());
            record.setExpenseDate(null);
            record.setPeriod(period.atDay(1));
        }
    }

    private String normalizeDescription(String description) {
        if (description == null) {
            return null;
        }
        String trimmed = description.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private YearMonth resolvePeriod(ExpenseRecordRequestDto dto, YearMonth defaultPeriod, LocalDate expenseDate) {
        if (expenseDate != null) {
            return YearMonth.from(expenseDate);
        }
        if (dto.getPeriod() != null && !dto.getPeriod().isBlank()) {
            return parsePeriod(dto.getPeriod());
        }
        if (defaultPeriod != null) {
            return defaultPeriod;
        }
        throw new IllegalArgumentException("Для траты необходимо указать дату или месяц");
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Пользователь не найден: " + username));
    }
}
