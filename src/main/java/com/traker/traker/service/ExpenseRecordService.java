package com.traker.traker.service;

import com.traker.traker.dto.expense.ExpenseBatchCreateRequestDto;
import com.traker.traker.dto.expense.ExpenseRecordRequestDto;
import com.traker.traker.dto.expense.ExpenseRecordResponseDto;
import com.traker.traker.dto.expense.ExpenseSummaryDto;
import com.traker.traker.entity.ExpenseCategory;
import com.traker.traker.entity.ExpenseRecord;
import com.traker.traker.entity.User;
import com.traker.traker.exception.ExpenseCategoryNotFoundException;
import com.traker.traker.mapper.ExpenseRecordMapper;
import com.traker.traker.repository.ExpenseCategoryRepository;
import com.traker.traker.repository.ExpenseRecordRepository;
import com.traker.traker.repository.UserRepository;
import com.traker.traker.repository.projection.CategoryAmountView;
import com.traker.traker.repository.projection.CategoryPeriodAmountView;
import com.traker.traker.repository.projection.PeriodAmountView;
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

        List<Long> normalizedCategories = normalizeCategoryFilter(categoryIds);

        List<CategoryAmountView> totalsByCategory = expenseRecordRepository.sumByCategory(
                currentUser,
                filter.fromDate(),
                filter.toDate(),
                filter.fromPeriod(),
                filter.toPeriod(),
                normalizedCategories);

        BigDecimal totalAmount = totalsByCategory.stream()
                .map(CategoryAmountView::getTotalAmount)
                .map(amount -> normalizeAmount(amount))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<PeriodAmountView> totalsByPeriod = expenseRecordRepository.sumByPeriod(
                currentUser,
                filter.fromDate(),
                filter.toDate(),
                filter.fromPeriod(),
                filter.toPeriod(),
                normalizedCategories);

        List<CategoryPeriodAmountView> categoryPeriodTotals = expenseRecordRepository.sumByCategoryAndPeriod(
                currentUser,
                filter.fromDate(),
                filter.toDate(),
                filter.fromPeriod(),
                filter.toPeriod(),
                normalizedCategories);

        ExpenseSummaryDto summaryDto = new ExpenseSummaryDto();
        summaryDto.setTotalsByCategory(totalsByCategory.stream()
                .map(view -> {
                    BigDecimal amount = normalizeAmount(view.getTotalAmount());
                    return new ExpenseSummaryDto.CategoryTotalDto(
                            view.getCategoryId(),
                            view.getCategoryName(),
                            amount,
                            calculatePercentage(amount, totalAmount));
                })
                .collect(Collectors.toList()));

        summaryDto.setTotalsByMonth(totalsByPeriod.stream()
                .map(view -> new ExpenseSummaryDto.MonthlyTotalDto(
                        formatPeriod(view.getPeriod()),
                        normalizeAmount(view.getTotalAmount())))
                .sorted(Comparator.comparing(ExpenseSummaryDto.MonthlyTotalDto::getPeriod))
                .collect(Collectors.toList()));

        summaryDto.setCategoryMonthlyTotals(buildCategoryMonthlySummary(categoryPeriodTotals));

        summaryDto.setTotalAmount(normalizeAmount(totalAmount));
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
        return categoryIds;
    }

    private List<ExpenseSummaryDto.CategoryMonthlySummaryDto> buildCategoryMonthlySummary(List<CategoryPeriodAmountView> categoryPeriodTotals) {
        Map<Long, ExpenseSummaryDto.CategoryMonthlySummaryDto> grouped = new LinkedHashMap<>();
        for (CategoryPeriodAmountView view : categoryPeriodTotals) {
            ExpenseSummaryDto.CategoryMonthlySummaryDto summary = grouped.computeIfAbsent(
                    view.getCategoryId(),
                    key -> new ExpenseSummaryDto.CategoryMonthlySummaryDto(
                            view.getCategoryId(),
                            view.getCategoryName(),
                            new ArrayList<>()));
            summary.getMonthlyTotals().add(new ExpenseSummaryDto.MonthlyTotalDto(
                    formatPeriod(view.getPeriod()),
                    normalizeAmount(view.getTotalAmount())));
        }

        return grouped.values().stream()
                .peek(summary -> summary.getMonthlyTotals().sort(Comparator.comparing(ExpenseSummaryDto.MonthlyTotalDto::getPeriod)))
                .collect(Collectors.toList());
    }

    private ExpenseRecord mapToEntity(ExpenseRecordRequestDto dto, YearMonth defaultPeriod, User currentUser) {
        ExpenseCategory category = expenseCategoryRepository.findByIdAndUser(dto.getCategoryId(), currentUser)
                .orElseThrow(() -> new ExpenseCategoryNotFoundException(dto.getCategoryId()));

        LocalDate expenseDate = dto.getExpenseDate();
        YearMonth period = resolvePeriod(dto, defaultPeriod, expenseDate);

        ExpenseRecord record = new ExpenseRecord();
        record.setUser(currentUser);
        record.setCategory(category);
        record.setTitle(dto.getTitle());
        record.setDescription(dto.getDescription());
        record.setAmount(normalizeAmount(dto.getAmount()));
        record.setPeriod(period.atDay(1));
        record.setExpenseDate(expenseDate);
        return record;
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
