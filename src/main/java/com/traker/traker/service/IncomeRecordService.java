package com.traker.traker.service;

import com.traker.traker.dto.income.IncomeBatchCreateRequestDto;
import com.traker.traker.dto.income.IncomeBatchUpdateRequestDto;
import com.traker.traker.dto.income.IncomeRecordRequestDto;
import com.traker.traker.dto.income.IncomeRecordResponseDto;
import com.traker.traker.dto.income.IncomeRecordUpdateDto;
import com.traker.traker.dto.income.IncomeSummaryDto;
import com.traker.traker.entity.IncomeCategory;
import com.traker.traker.entity.IncomeRecord;
import com.traker.traker.entity.User;
import com.traker.traker.exception.IncomeCategoryNotFoundException;
import com.traker.traker.exception.IncomeRecordNotFoundException;
import com.traker.traker.mapper.IncomeRecordMapper;
import com.traker.traker.repository.IncomeCategoryRepository;
import com.traker.traker.repository.IncomeRecordRepository;
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

    @Transactional
    public IncomeRecordResponseDto updateIncome(Long id, IncomeRecordRequestDto request) {
        User currentUser = getCurrentUser();
        IncomeRecord record = incomeRecordRepository.findByIdAndUser(id, currentUser)
                .orElseThrow(() -> new IncomeRecordNotFoundException(id));

        applyFullUpdate(record, request, currentUser);
        IncomeRecord saved = incomeRecordRepository.save(record);
        return incomeRecordMapper.toDto(saved);
    }

    @Transactional
    public List<IncomeRecordResponseDto> updateIncomes(IncomeBatchUpdateRequestDto request) {
        User currentUser = getCurrentUser();
        List<IncomeRecordUpdateDto> updates = request.getRecords();
        if (updates == null || updates.isEmpty()) {
            return List.of();
        }

        Set<Long> ids = updates.stream()
                .map(IncomeRecordUpdateDto::getId)
                .collect(Collectors.toSet());

        Map<Long, IncomeRecord> existing = incomeRecordRepository.findByUserAndIdIn(currentUser, new ArrayList<>(ids)).stream()
                .collect(Collectors.toMap(IncomeRecord::getId, Function.identity()));

        for (IncomeRecordUpdateDto updateDto : updates) {
            IncomeRecord record = existing.get(updateDto.getId());
            if (record == null) {
                throw new IncomeRecordNotFoundException(updateDto.getId());
            }
            applyPartialUpdate(record, updateDto, currentUser);
        }

        incomeRecordRepository.saveAll(existing.values());
        return updates.stream()
                .map(update -> incomeRecordMapper.toDto(existing.get(update.getId())))
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteIncome(Long id) {
        User currentUser = getCurrentUser();
        IncomeRecord record = incomeRecordRepository.findByIdAndUser(id, currentUser)
                .orElseThrow(() -> new IncomeRecordNotFoundException(id));
        incomeRecordRepository.delete(record);
    }

    @Transactional
    public void deleteIncomes(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        User currentUser = getCurrentUser();
        List<IncomeRecord> records = incomeRecordRepository.findByUserAndIdIn(currentUser, ids);
        long requested = ids.stream().distinct().count();
        if (records.size() != requested) {
            Set<Long> foundIds = records.stream().map(IncomeRecord::getId).collect(Collectors.toSet());
            Long missing = ids.stream().filter(id -> !foundIds.contains(id)).findFirst().orElse(null);
            throw new IncomeRecordNotFoundException(missing);
        }
        incomeRecordRepository.deleteAll(records);
    }

    @Transactional(readOnly = true)
    public List<IncomeRecordResponseDto> getIncomes(String fromDate, String toDate, String month, List<Long> categoryIds) {
        FinanceFilter filter = buildFilter(fromDate, toDate, month);
        User currentUser = getCurrentUser();
        List<IncomeRecord> records = incomeRecordRepository.findByUserAndFilter(
                currentUser,
                filter.fromDate(),
                filter.toDate(),
                filter.fromPeriod(),
                filter.toPeriod(),
                normalizeCategoryFilter(categoryIds));
        return incomeRecordMapper.toDtoList(records);
    }

    @Transactional(readOnly = true)
    public IncomeSummaryDto getSummary(String fromDate, String toDate, String month, List<Long> categoryIds) {
        FinanceFilter filter = buildFilter(fromDate, toDate, month);
        User currentUser = getCurrentUser();

        List<IncomeRecord> records = incomeRecordRepository.findByUserAndFilter(
                currentUser,
                filter.fromDate(),
                filter.toDate(),
                filter.fromPeriod(),
                filter.toPeriod(),
                normalizeCategoryFilter(categoryIds));

        Map<LocalDate, BigDecimal> totalsByPeriod = new LinkedHashMap<>();
        Map<Long, CategoryAggregation> categoryAggregations = new LinkedHashMap<>();

        for (IncomeRecord record : records) {
            BigDecimal amount = normalizeAmount(record.getAmount());
            totalsByPeriod.merge(record.getPeriod(), amount, BigDecimal::add);

            Long categoryId = record.getCategory().getId();
            CategoryAggregation aggregation = categoryAggregations.computeIfAbsent(
                    categoryId,
                    id -> new CategoryAggregation(categoryId, record.getCategory().getName()));
            aggregation.addAmount(record.getPeriod(), amount);
        }

        BigDecimal totalAmount = records.stream()
                .map(IncomeRecord::getAmount)
                .map(amount -> normalizeAmount(amount))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        IncomeSummaryDto summaryDto = new IncomeSummaryDto();
        BigDecimal normalizedTotal = normalizeAmount(totalAmount);
        summaryDto.setTotalAmount(normalizedTotal);

        summaryDto.setTotalsByCategory(categoryAggregations.values().stream()
                .sorted(Comparator.comparing(CategoryAggregation::getCategoryName, String.CASE_INSENSITIVE_ORDER))
                .map(aggregation -> new IncomeSummaryDto.CategoryTotalDto(
                        aggregation.getCategoryId(),
                        aggregation.getCategoryName(),
                        normalizeAmount(aggregation.getTotal()),
                        calculatePercentage(normalizeAmount(aggregation.getTotal()), normalizedTotal)))
                .collect(Collectors.toList()));

        summaryDto.setTotalsByMonth(totalsByPeriod.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new IncomeSummaryDto.MonthlyTotalDto(
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

    private List<IncomeSummaryDto.CategoryMonthlySummaryDto> buildCategoryMonthlySummary(Map<Long, CategoryAggregation> aggregations) {
        return aggregations.values().stream()
                .sorted(Comparator.comparing(CategoryAggregation::getCategoryName, String.CASE_INSENSITIVE_ORDER))
                .map(aggregation -> {
                    List<IncomeSummaryDto.MonthlyTotalDto> monthlyTotals = aggregation.getMonthlyTotals().entrySet().stream()
                            .sorted(Map.Entry.comparingByKey())
                            .map(entry -> new IncomeSummaryDto.MonthlyTotalDto(
                                    formatPeriod(entry.getKey()),
                                    normalizeAmount(entry.getValue())))
                            .collect(Collectors.toList());
                    return new IncomeSummaryDto.CategoryMonthlySummaryDto(
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

    private IncomeRecord mapToEntity(IncomeRecordRequestDto dto, YearMonth defaultPeriod, User currentUser) {
        IncomeCategory category = incomeCategoryRepository.findByIdAndUser(dto.getCategoryId(), currentUser)
                .orElseThrow(() -> new IncomeCategoryNotFoundException(dto.getCategoryId()));

        LocalDate incomeDate = dto.getIncomeDate();
        YearMonth period = resolvePeriod(dto, defaultPeriod, incomeDate);

        IncomeRecord record = new IncomeRecord();
        record.setUser(currentUser);
        record.setCategory(category);
        record.setTitle(dto.getTitle().trim());
        record.setDescription(normalizeDescription(dto.getDescription()));
        record.setAmount(normalizeAmount(dto.getAmount()));
        record.setPeriod(period.atDay(1));
        record.setIncomeDate(incomeDate);
        return record;
    }

    private void applyFullUpdate(IncomeRecord record, IncomeRecordRequestDto dto, User currentUser) {
        IncomeCategory category = incomeCategoryRepository.findByIdAndUser(dto.getCategoryId(), currentUser)
                .orElseThrow(() -> new IncomeCategoryNotFoundException(dto.getCategoryId()));

        LocalDate incomeDate = dto.getIncomeDate();
        YearMonth period = resolvePeriod(dto, null, incomeDate);

        record.setCategory(category);
        record.setTitle(dto.getTitle().trim());
        record.setDescription(normalizeDescription(dto.getDescription()));
        record.setAmount(normalizeAmount(dto.getAmount()));
        record.setIncomeDate(incomeDate);
        record.setPeriod(period.atDay(1));
    }

    private void applyPartialUpdate(IncomeRecord record, IncomeRecordUpdateDto dto, User currentUser) {
        if (dto.getTitle() != null) {
            if (dto.getTitle().isBlank()) {
                throw new IllegalArgumentException("Название дохода не может быть пустым");
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
            IncomeCategory category = incomeCategoryRepository.findByIdAndUser(dto.getCategoryId(), currentUser)
                    .orElseThrow(() -> new IncomeCategoryNotFoundException(dto.getCategoryId()));
            record.setCategory(category);
        }

        if (dto.getIncomeDate() != null) {
            LocalDate incomeDate = dto.getIncomeDate();
            record.setIncomeDate(incomeDate);
            record.setPeriod(incomeDate.withDayOfMonth(1));
        } else if (dto.getPeriod() != null) {
            if (dto.getPeriod().isBlank()) {
                throw new IllegalArgumentException("Период должен быть указан в формате yyyy-MM");
            }
            YearMonth period = parsePeriod(dto.getPeriod());
            record.setIncomeDate(null);
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

    private YearMonth resolvePeriod(IncomeRecordRequestDto dto, YearMonth defaultPeriod, LocalDate incomeDate) {
        if (incomeDate != null) {
            return YearMonth.from(incomeDate);
        }
        if (dto.getPeriod() != null && !dto.getPeriod().isBlank()) {
            return parsePeriod(dto.getPeriod());
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
