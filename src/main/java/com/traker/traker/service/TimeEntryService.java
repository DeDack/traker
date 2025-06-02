package com.traker.traker.service;

import com.traker.traker.entity.DayLog;
import com.traker.traker.entity.TimeEntry;
import com.traker.traker.entity.User;
import com.traker.traker.repository.DayLogRepository;
import com.traker.traker.repository.TimeEntryRepository;
import com.traker.traker.repository.UserRepository;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Сервис для управления записями времени и расчета статистики.
 */
@Service
@RequiredArgsConstructor
public class TimeEntryService {

    private final TimeEntryRepository timeEntryRepository;
    private final DayLogRepository dayLogRepository;
    private final UserRepository userRepository;

    /**
     * Рассчитывает общее количество отработанных часов за указанную дату для текущего пользователя.
     *
     * @param dateStr дата в виде строки в формате yyyy-MM-dd
     * @return общее количество отработанных часов за указанную дату
     * @throws IllegalArgumentException если формат даты неверный
     */
    public int getTotalHoursWorked(String dateStr) {
        LocalDate date = parseDate(dateStr);
        User currentUser = getCurrentUser();

        Optional<DayLog> dayLogOptional = dayLogRepository.findByDate(date);

        if (dayLogOptional.isEmpty()) {
            return 0;
        }

        DayLog dayLog = dayLogOptional.get();

        List<TimeEntry> entries = timeEntryRepository.findByDayLogAndUser(dayLog, currentUser);

        return (int) entries.stream()
                .filter(TimeEntry::isWorked)
                .count();
    }

    /**
     * Преобразует строку даты в объект LocalDate.
     *
     * @param dateStr строка даты для преобразования
     * @return объект LocalDate
     * @throws IllegalArgumentException если формат даты неверный
     */
    private LocalDate parseDate(String dateStr) {
        try {
            return LocalDate.parse(dateStr);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Неверный формат даты: " + dateStr, e);
        }
    }

    /**
     * Возвращает текущего аутентифицированного пользователя.
     *
     * @return Текущий пользователь
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Пользователь не найден: " + username));
    }
}