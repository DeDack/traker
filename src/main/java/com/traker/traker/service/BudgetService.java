package com.traker.traker.service;

import com.traker.traker.dto.budget.BudgetRequestDto;
import com.traker.traker.dto.budget.BudgetResponseDto;
import com.traker.traker.entity.Budget;
import com.traker.traker.entity.User;
import com.traker.traker.repository.BudgetRepository;
import com.traker.traker.repository.ExpenseRecordRepository;
import com.traker.traker.repository.IncomeRecordRepository;
import com.traker.traker.repository.UserRepository;
import com.traker.traker.repository.projection.PeriodAmountView;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.traker.traker.utils.FinanceUtils.FinanceFilter;
import static com.traker.traker.utils.FinanceUtils.buildFilter;
import static com.traker.traker.utils.FinanceUtils.formatPeriod;
import static com.traker.traker.utils.FinanceUtils.normalizeAmount;
import static com.traker.traker.utils.FinanceUtils.parsePeriod;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final ExpenseRecordRepository expenseRecordRepository;
    private final IncomeRecordRepository incomeRecordRepository;
    private final UserRepository userRepository;

    @Transactional
    public BudgetResponseDto upsertBudget(BudgetRequestDto request) {
        User user = getCurrentUser();
        YearMonth month = parsePeriod(request.getMonth());
        LocalDate periodStart = month.atDay(1);

        Budget budget = budgetRepository.findByUserAndPeriod(user, periodStart)
                .orElseGet(() -> {
                    Budget created = new Budget();
                    created.setUser(user);
                    created.setPeriod(periodStart);
                    return created;
                });

        budget.setPlannedIncome(normalizeAmount(request.getPlannedIncome()));
        budget.setPlannedExpense(normalizeAmount(request.getPlannedExpense()));
        budget.setSavingsGoal(normalizeAmount(request.getSavingsGoal()));
        budget.setNotes(request.getNotes());

        budgetRepository.save(budget);
        return getBudget(request.getMonth());
    }

    @Transactional(readOnly = true)
    public BudgetResponseDto getBudget(String month) {
        if (month == null || month.isBlank()) {
            throw new IllegalArgumentException("Необходимо указать месяц в формате yyyy-MM");
        }
        FinanceFilter filter = buildFilter(null, null, month);
        User user = getCurrentUser();
        LocalDate period = Objects.requireNonNull(filter.month()).atDay(1);

        Budget budget = budgetRepository.findByUserAndPeriod(user, period).orElse(null);
        BigDecimal actualExpenses = extractTotalForPeriod(expenseRecordRepository.sumByPeriod(
                user,
                filter.fromDate(),
                filter.toDate(),
                filter.fromPeriod(),
                filter.toPeriod()),
                period);
        BigDecimal actualIncomes = extractTotalForPeriod(incomeRecordRepository.sumByPeriod(
                user,
                filter.fromDate(),
                filter.toDate(),
                filter.fromPeriod(),
                filter.toPeriod()),
                period);

        return buildBudgetDto(period, budget, actualIncomes, actualExpenses);
    }

    @Transactional(readOnly = true)
    public List<BudgetResponseDto> getBudgets(String fromDate, String toDate, String month) {
        FinanceFilter filter = buildFilter(fromDate, toDate, month);
        User user = getCurrentUser();

        List<Budget> budgets = findBudgets(user, filter);
        Map<LocalDate, Budget> budgetByPeriod = budgets.stream()
                .collect(Collectors.toMap(Budget::getPeriod, Function.identity(), (left, right) -> left, LinkedHashMap::new));

        Map<LocalDate, BigDecimal> expenseTotals = expenseRecordRepository.sumByPeriod(
                        user,
                        filter.fromDate(),
                        filter.toDate(),
                        filter.fromPeriod(),
                        filter.toPeriod())
                .stream()
                .collect(Collectors.toMap(PeriodAmountView::getPeriod, view -> normalizeAmount(view.getTotalAmount())));

        Map<LocalDate, BigDecimal> incomeTotals = incomeRecordRepository.sumByPeriod(
                        user,
                        filter.fromDate(),
                        filter.toDate(),
                        filter.fromPeriod(),
                        filter.toPeriod())
                .stream()
                .collect(Collectors.toMap(PeriodAmountView::getPeriod, view -> normalizeAmount(view.getTotalAmount())));

        SortedSet<LocalDate> periods = collectPeriods(filter, budgetByPeriod.keySet(), expenseTotals.keySet(), incomeTotals.keySet());

        List<BudgetResponseDto> responses = new ArrayList<>();
        for (LocalDate period : periods) {
            Budget budget = budgetByPeriod.get(period);
            BigDecimal actualIncome = incomeTotals.getOrDefault(period, normalizeAmount(null));
            BigDecimal actualExpense = expenseTotals.getOrDefault(period, normalizeAmount(null));
            responses.add(buildBudgetDto(period, budget, actualIncome, actualExpense));
        }

        responses.sort(Comparator.comparing(BudgetResponseDto::getMonth));
        return responses;
    }

    private List<Budget> findBudgets(User user, FinanceFilter filter) {
        List<Budget> budgets = budgetRepository.findByUser(user);
        return budgets.stream()
                .filter(budget -> (filter.fromPeriod() == null || !budget.getPeriod().isBefore(filter.fromPeriod()))
                        && (filter.toPeriod() == null || !budget.getPeriod().isAfter(filter.toPeriod())))
                .collect(Collectors.toList());
    }

    private SortedSet<LocalDate> collectPeriods(FinanceFilter filter,
                                                Iterable<LocalDate> budgetPeriods,
                                                Iterable<LocalDate> expensePeriods,
                                                Iterable<LocalDate> incomePeriods) {
        SortedSet<LocalDate> periods = new TreeSet<>();
        if (filter.month() != null) {
            periods.add(filter.month().atDay(1));
        } else if (filter.fromPeriod() != null && filter.toPeriod() != null) {
            LocalDate cursor = filter.fromPeriod();
            while (!cursor.isAfter(filter.toPeriod())) {
                periods.add(cursor);
                cursor = cursor.plusMonths(1);
            }
        }
        budgetPeriods.forEach(periods::add);
        expensePeriods.forEach(periods::add);
        incomePeriods.forEach(periods::add);

        if (periods.isEmpty()) {
            periods.add(YearMonth.now().atDay(1));
        }
        return periods;
    }

    private BudgetResponseDto buildBudgetDto(LocalDate period,
                                             Budget budget,
                                             BigDecimal actualIncome,
                                             BigDecimal actualExpense) {
        BudgetResponseDto dto = new BudgetResponseDto();
        dto.setMonth(formatPeriod(period));

        BigDecimal plannedIncome = budget != null ? normalizeAmount(budget.getPlannedIncome()) : normalizeAmount(null);
        BigDecimal plannedExpense = budget != null ? normalizeAmount(budget.getPlannedExpense()) : normalizeAmount(null);
        BigDecimal savingsGoal = budget != null ? normalizeAmount(budget.getSavingsGoal()) : normalizeAmount(null);

        dto.setId(budget != null ? budget.getId() : null);
        dto.setPlannedIncome(plannedIncome);
        dto.setPlannedExpense(plannedExpense);
        dto.setSavingsGoal(savingsGoal);
        dto.setNotes(budget != null ? budget.getNotes() : null);

        BigDecimal income = normalizeAmount(actualIncome);
        BigDecimal expense = normalizeAmount(actualExpense);

        dto.setActualIncome(income);
        dto.setActualExpense(expense);
        dto.setPlannedBalance(plannedIncome.subtract(plannedExpense));
        dto.setActualBalance(income.subtract(expense));
        dto.setRemainingToSpend(plannedExpense.subtract(expense));
        dto.setSavingsProgress(income.subtract(expense).subtract(savingsGoal));
        return dto;
    }

    private BigDecimal extractTotalForPeriod(List<PeriodAmountView> totals, LocalDate period) {
        return totals.stream()
                .filter(view -> view.getPeriod().equals(period))
                .map(view -> normalizeAmount(view.getTotalAmount()))
                .findFirst()
                .orElse(normalizeAmount(null));
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Пользователь не найден: " + username));
    }
}
