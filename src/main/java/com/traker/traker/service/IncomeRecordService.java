package com.traker.traker.service;

import com.traker.traker.dto.income.IncomeBatchCreateRequestDto;
import com.traker.traker.dto.income.IncomeRecordRequestDto;
import com.traker.traker.dto.income.IncomeRecordResponseDto;
import com.traker.traker.dto.income.IncomeSummaryDto;
import com.traker.traker.entity.IncomeCategory;
import com.traker.traker.entity.IncomeRecord;
import com.traker.traker.entity.User;
import com.traker.traker.exception.IncomeCategoryNotFoundException;
import com.traker.traker.mapper.IncomeRecordMapper;
import com.traker.traker.repository.IncomeCategoryRepository;
import com.traker.traker.repository.IncomeRecordRepository;
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
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.traker.traker.utils.FinanceUtils.FinanceFilter;
import static com.traker.traker.utils.FinanceUtils.buildFilter;
import static com.traker.traker.utils.FinanceUtils.formatPeriod;
import static com.traker.traker.utils.FinanceUtils.normalizeAmount;
import static com.traker.traker.utils.FinanceUtils.parseOptionalPeriod;

@Service
@RequiredArgsConstructor
public class IncomeRecordService {

    private final IncomeRecordRepository incomeRecordRepository;
    private final IncomeCategoryRepository incomeCategoryRepository;
    private final IncomeRecordMapper incomeRecordMapper;
    private final UserRepository userRepository;

    @Transactional
    public List<IncomeRecordResponseDto> createBatch(IncomeBatchCreateRequestDto request) {
        User currentUser = getCurrentUser();
        YearMonth defaultPeriod = parseOptionalPeriod(request.getDefaultPeriod());

        List<IncomeRecord> records = request.getIncomes().stream()
                .map(dto -> mapToEntity(dto, defaultPeriod, currentUser))
                .collect(Collectors.toList());

        List<IncomeRecord> saved = incomeRecordRepository.saveAll(records);
        return incomeRecordMapper.toDtoList(saved);
    }

    @Transactional(readOnly = true)
    public List<IncomeRecordResponseDto> getIncomes(String fromDate, String toDate, String month) {
        FinanceFilter filter = buildFilter(fromDate, toDate, month);
        User currentUser = getCurrentUser();
        List<IncomeRecord> records = incomeRecordRepository.findByUserAndFilter(
                currentUser,
                filter.fromDate(),
                filter.toDate(),
                filter.fromPeriod(),
                filter.toPeriod());
        return incomeRecordMapper.toDtoList(records);
    }

    @Transactional(readOnly = true)
    public IncomeSummaryDto getSummary(String fromDate, String toDate, String month) {
        FinanceFilter filter = buildFilter(fromDate, toDate, month);
        User currentUser = getCurrentUser();

        List<CategoryAmountView> totalsByCategory = incomeRecordRepository.sumByCategory(
                currentUser,
                filter.fromDate(),
                filter.toDate(),
                filter.fromPeriod(),
                filter.toPeriod());

        List<PeriodAmountView> totalsByPeriod = incomeRecordRepository.sumByPeriod(
                currentUser,
                filter.fromDate(),
                filter.toDate(),
                filter.fromPeriod(),
                filter.toPeriod());

        List<CategoryPeriodAmountView> categoryPeriodTotals = incomeRecordRepository.sumByCategoryAndPeriod(
                currentUser,
                filter.fromDate(),
                filter.toDate(),
                filter.fromPeriod(),
                filter.toPeriod());

        IncomeSummaryDto summaryDto = new IncomeSummaryDto();
        summaryDto.setTotalsByCategory(totalsByCategory.stream()
                .map(view -> new IncomeSummaryDto.CategoryTotalDto(
                        view.getCategoryId(),
                        view.getCategoryName(),
                        normalizeAmount(view.getTotalAmount())))
                .collect(Collectors.toList()));

        summaryDto.setTotalsByMonth(totalsByPeriod.stream()
                .map(view -> new IncomeSummaryDto.MonthlyTotalDto(
                        formatPeriod(view.getPeriod()),
                        normalizeAmount(view.getTotalAmount())))
                .sorted(Comparator.comparing(IncomeSummaryDto.MonthlyTotalDto::getPeriod))
                .collect(Collectors.toList()));

        summaryDto.setCategoryMonthlyTotals(buildCategoryMonthlySummary(categoryPeriodTotals));

        BigDecimal totalAmount = summaryDto.getTotalsByCategory().stream()
                .map(IncomeSummaryDto.CategoryTotalDto::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        summaryDto.setTotalAmount(normalizeAmount(totalAmount));
        return summaryDto;
    }

    private List<IncomeSummaryDto.CategoryMonthlySummaryDto> buildCategoryMonthlySummary(List<CategoryPeriodAmountView> categoryPeriodTotals) {
        Map<Long, IncomeSummaryDto.CategoryMonthlySummaryDto> grouped = new LinkedHashMap<>();
        for (CategoryPeriodAmountView view : categoryPeriodTotals) {
            IncomeSummaryDto.CategoryMonthlySummaryDto summary = grouped.computeIfAbsent(
                    view.getCategoryId(),
                    key -> new IncomeSummaryDto.CategoryMonthlySummaryDto(
                            view.getCategoryId(),
                            view.getCategoryName(),
                            new ArrayList<>()));
            summary.getMonthlyTotals().add(new IncomeSummaryDto.MonthlyTotalDto(
                    formatPeriod(view.getPeriod()),
                    normalizeAmount(view.getTotalAmount())));
        }

        return grouped.values().stream()
                .peek(summary -> summary.getMonthlyTotals().sort(Comparator.comparing(IncomeSummaryDto.MonthlyTotalDto::getPeriod)))
                .collect(Collectors.toList());
    }

    private IncomeRecord mapToEntity(IncomeRecordRequestDto dto, YearMonth defaultPeriod, User currentUser) {
        IncomeCategory category = incomeCategoryRepository.findByIdAndUser(dto.getCategoryId(), currentUser)
                .orElseThrow(() -> new IncomeCategoryNotFoundException(dto.getCategoryId()));

        LocalDate incomeDate = dto.getIncomeDate();
        YearMonth period = resolvePeriod(dto, defaultPeriod, incomeDate);

        IncomeRecord record = new IncomeRecord();
        record.setUser(currentUser);
        record.setCategory(category);
        record.setTitle(dto.getTitle());
        record.setDescription(dto.getDescription());
        record.setAmount(normalizeAmount(dto.getAmount()));
        record.setPeriod(period.atDay(1));
        record.setIncomeDate(incomeDate);
        return record;
    }

    private YearMonth resolvePeriod(IncomeRecordRequestDto dto, YearMonth defaultPeriod, LocalDate incomeDate) {
        if (incomeDate != null) {
            return YearMonth.from(incomeDate);
        }
        if (dto.getPeriod() != null && !dto.getPeriod().isBlank()) {
            return parseOptionalPeriod(dto.getPeriod());
        }
        if (defaultPeriod != null) {
            return defaultPeriod;
        }
        throw new IllegalArgumentException("Для дохода необходимо указать дату или месяц");
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Пользователь не найден: " + username));
    }
}
