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
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExpenseRecordService {

    private static final DateTimeFormatter PERIOD_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

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
    public List<ExpenseRecordResponseDto> getExpenses(String fromDate, String toDate, String month) {
        FilterCriteria filter = buildFilterCriteria(fromDate, toDate, month);
        User currentUser = getCurrentUser();
        List<ExpenseRecord> records = expenseRecordRepository.findByUserAndFilter(
                currentUser,
                filter.fromDate(),
                filter.toDate(),
                filter.fromPeriod(),
                filter.toPeriod());
        return expenseRecordMapper.toDtoList(records);
    }

    @Transactional(readOnly = true)
    public ExpenseSummaryDto getSummary(String fromDate, String toDate, String month) {
        FilterCriteria filter = buildFilterCriteria(fromDate, toDate, month);
        User currentUser = getCurrentUser();

        List<CategoryAmountView> totalsByCategory = expenseRecordRepository.sumByCategory(
                currentUser,
                filter.fromDate(),
                filter.toDate(),
                filter.fromPeriod(),
                filter.toPeriod());

        List<PeriodAmountView> totalsByPeriod = expenseRecordRepository.sumByPeriod(
                currentUser,
                filter.fromDate(),
                filter.toDate(),
                filter.fromPeriod(),
                filter.toPeriod());

        List<CategoryPeriodAmountView> categoryPeriodTotals = expenseRecordRepository.sumByCategoryAndPeriod(
                currentUser,
                filter.fromDate(),
                filter.toDate(),
                filter.fromPeriod(),
                filter.toPeriod());

        ExpenseSummaryDto summaryDto = new ExpenseSummaryDto();
        summaryDto.setTotalsByCategory(totalsByCategory.stream()
                .map(view -> new ExpenseSummaryDto.CategoryTotalDto(
                        view.getCategoryId(),
                        view.getCategoryName(),
                        normalizeAmount(view.getTotalAmount())))
                .collect(Collectors.toList()));

        summaryDto.setTotalsByMonth(totalsByPeriod.stream()
                .map(view -> new ExpenseSummaryDto.MonthlyTotalDto(
                        formatPeriod(view.getPeriod()),
                        normalizeAmount(view.getTotalAmount())))
                .sorted(Comparator.comparing(ExpenseSummaryDto.MonthlyTotalDto::getPeriod))
                .collect(Collectors.toList()));

        summaryDto.setCategoryMonthlyTotals(buildCategoryMonthlySummary(categoryPeriodTotals));

        BigDecimal totalAmount = summaryDto.getTotalsByCategory().stream()
                .map(ExpenseSummaryDto.CategoryTotalDto::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        summaryDto.setTotalAmount(normalizeAmount(totalAmount));
        return summaryDto;
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

    private FilterCriteria buildFilterCriteria(String fromDate, String toDate, String month) {
        YearMonth monthFilter = parseOptionalPeriod(month);
        LocalDate from = parseOptionalDate(fromDate);
        LocalDate to = parseOptionalDate(toDate);

        if (monthFilter != null && (from != null || to != null)) {
            throw new IllegalArgumentException("Нельзя одновременно задавать месяц и произвольный диапазон дат");
        }

        if (from != null && to != null && to.isBefore(from)) {
            throw new IllegalArgumentException("Дата окончания диапазона не может быть раньше даты начала");
        }

        if (monthFilter != null) {
            LocalDate period = monthFilter.atDay(1);
            return new FilterCriteria(
                    monthFilter.atDay(1),
                    monthFilter.atEndOfMonth(),
                    period,
                    period);
        }

        LocalDate fromPeriod = from != null ? from.withDayOfMonth(1) : null;
        LocalDate toPeriod = to != null ? to.withDayOfMonth(1) : null;
        return new FilterCriteria(from, to, fromPeriod, toPeriod);
    }

    private YearMonth parseOptionalPeriod(String period) {
        if (period == null || period.isBlank()) {
            return null;
        }
        return parsePeriod(period);
    }

    private YearMonth parsePeriod(String period) {
        try {
            return YearMonth.parse(period, PERIOD_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Некорректный формат периода: " + period, e);
        }
    }

    private LocalDate parseOptionalDate(String date) {
        if (date == null || date.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Некорректный формат даты: " + date, e);
        }
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private String formatPeriod(LocalDate period) {
        return period == null ? null : period.format(PERIOD_FORMATTER);
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Пользователь не найден: " + username));
    }

    private record FilterCriteria(LocalDate fromDate, LocalDate toDate, LocalDate fromPeriod, LocalDate toPeriod) {
    }
}
