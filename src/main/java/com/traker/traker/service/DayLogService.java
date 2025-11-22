package com.traker.traker.service;

import com.traker.traker.dto.StatusDto;
import com.traker.traker.dto.TimeEntryDto;
import com.traker.traker.entity.DayLog;
import com.traker.traker.entity.Status;
import com.traker.traker.entity.TimeEntry;
import com.traker.traker.entity.User;
import com.traker.traker.exception.StatusNotFoundException;
import com.traker.traker.mapper.TimeEntryMapper;
import com.traker.traker.repository.DayLogRepository;
import com.traker.traker.repository.StatusRepository;
import com.traker.traker.repository.TimeEntryRepository;
import com.traker.traker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Сервисный класс для управления сущностями DayLog и TimeEntry.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DayLogService {
    private final DayLogRepository dayLogRepository;
    private final TimeEntryRepository timeEntryRepository;
    private final TimeEntryMapper timeEntryMapper;
    private final StatusRepository statusRepository;
    private final UserRepository userRepository;

    /**
     * Получает список объектов TimeEntryDto для заданной даты и текущего пользователя.
     *
     * @param date строка даты в формате (yyyy-MM-dd)
     * @return список объектов TimeEntryDto
     */
    public List<TimeEntryDto> getTimeEntriesByDate(String date) {
        LocalDate localDate = parseDate(date);
        User currentUser = getCurrentUser();
        Optional<DayLog> dayLog = dayLogRepository.findByDate(localDate);
        return dayLog.map(dl -> timeEntryRepository.findByDayLogAndUserOrderByHourAscMinuteAsc(dl, currentUser).stream()
                        .sorted(Comparator.comparingInt(TimeEntry::getHour)
                                .thenComparingInt(TimeEntry::getMinute))
                        .map(timeEntryMapper::toDto)
                        .collect(Collectors.toList()))
                .orElseGet(List::of);
    }

    /**
     * Обновляет или создает TimeEntry для заданной даты и текущего пользователя.
     *
     * @param date строка даты в формате (yyyy-MM-dd)
     * @param timeEntryDto объект DTO, содержащий детали записи времени
     * @return обновленный или созданный объект TimeEntryDto
     */
    @Transactional
    public TimeEntryDto updateTimeEntry(String date, TimeEntryDto timeEntryDto) {
        Logger logger = LoggerFactory.getLogger(this.getClass());

        logger.info("Начало метода updateTimeEntry для даты: {} и объекта timeEntryDto: {}", date, timeEntryDto);

        LocalDate localDate = parseDate(date);
        logger.info("Дата после парсинга: {}", localDate);

        User currentUser = getCurrentUser();
        logger.info("Текущий пользователь: {}", currentUser.getUsername());

        DayLog dayLog = dayLogRepository.findByDate(localDate)
                .orElseGet(() -> {
                    logger.info("DayLog не найден для даты: {}. Создание нового DayLog.", localDate);
                    return createNewDayLog(localDate);
                });

        validateInterval(timeEntryDto.getHour(), timeEntryDto.getMinute(), timeEntryDto.getEndHour(), timeEntryDto.getEndMinute());

        TimeEntry timeEntry = resolveExistingEntry(dayLog, currentUser, timeEntryDto);

        int startMinutes = toMinutes(timeEntryDto.getHour(), timeEntryDto.getMinute());
        int endMinutes = toMinutes(timeEntryDto.getEndHour(), timeEntryDto.getEndMinute());

        Long excludeId = timeEntry.getId();
        boolean hasOverlap = !timeEntryRepository
                .findOverlappingEntries(dayLog, currentUser, startMinutes, endMinutes, excludeId)
                .isEmpty();
        if (hasOverlap) {
            throw new IllegalArgumentException("Выбранный интервал пересекается с уже существующей активностью");
        }

        timeEntry.setDayLog(dayLog);
        timeEntry.setHour(timeEntryDto.getHour());
        timeEntry.setMinute(timeEntryDto.getMinute());
        timeEntry.setEndHour(timeEntryDto.getEndHour());
        timeEntry.setEndMinute(timeEntryDto.getEndMinute());
        timeEntry.setWorked(timeEntryDto.isWorked());
        timeEntry.setComment(StringUtils.hasText(timeEntryDto.getComment()) ? timeEntryDto.getComment().trim() : null);
        timeEntry.setStatus(resolveStatus(timeEntryDto.getStatus(), currentUser));
        timeEntry.setUser(currentUser);

        TimeEntry savedTimeEntry = timeEntryRepository.save(timeEntry);
        logger.info("Сохраненный TimeEntry: {}", savedTimeEntry);

        return timeEntryMapper.toDto(savedTimeEntry);
    }

    @Transactional
    public void deleteTimeEntry(String date, int hour, int minute) {
        LocalDate localDate = parseDate(date);
        User currentUser = getCurrentUser();
        dayLogRepository.findByDate(localDate).ifPresent(dayLog ->
                timeEntryRepository.findByDayLogAndHourAndMinuteAndUser(dayLog, hour, minute, currentUser)
                        .ifPresent(entry -> {
                            timeEntryRepository.delete(entry);
                            cleanupDayLogIfEmpty(dayLog);
                        }));
    }

    @Transactional
    public void deleteTimeEntry(Long entryId) {
        if (entryId == null) {
            return;
        }
        User currentUser = getCurrentUser();
        TimeEntry entry = timeEntryRepository.findByIdAndUser(entryId, currentUser)
                .orElseThrow(() -> new IllegalArgumentException("Запись не найдена или недоступна"));
        DayLog dayLog = entry.getDayLog();
        timeEntryRepository.delete(entry);
        cleanupDayLogIfEmpty(dayLog);
    }

    /**
     * Парсит строку даты в объект LocalDate.
     *
     * @param date строка даты в формате (yyyy-MM-dd)
     * @return объект LocalDate
     */
    private LocalDate parseDate(String date) {
        try {
            return LocalDate.parse(date);
        } catch (Exception e) {
            log.error("Неверный формат даты: {}", date, e);
            throw new IllegalArgumentException("Неверный формат даты: " + date, e);
        }
    }

    /**
     * Создает новый объект DayLog для заданной даты.
     *
     * @param date объект LocalDate
     * @return новый объект DayLog
     */
    private DayLog createNewDayLog(LocalDate date) {
        DayLog newDayLog = new DayLog();
        newDayLog.setDate(date);
        return dayLogRepository.save(newDayLog);
    }

    /**
     * Создает новый объект TimeEntry для заданного DayLog, времени и пользователя.
     *
     * @param dayLog объект DayLog
     * @param hour   час записи времени
     * @param minute минута записи времени
     * @param user   текущий пользователь
     * @return новый объект TimeEntry
     */
    private TimeEntry createNewTimeEntry(DayLog dayLog, int hour, int minute, User user) {
        TimeEntry newTimeEntry = new TimeEntry();
        newTimeEntry.setDayLog(dayLog);
        newTimeEntry.setHour(hour);
        newTimeEntry.setMinute(minute);
        newTimeEntry.setEndHour(Math.min(23, hour + 1));
        newTimeEntry.setEndMinute(minute);
        newTimeEntry.setUser(user);
        return newTimeEntry;
    }

    private TimeEntry resolveExistingEntry(DayLog dayLog, User user, TimeEntryDto timeEntryDto) {
        if (timeEntryDto.getId() != null) {
            return timeEntryRepository.findByIdAndUser(timeEntryDto.getId(), user)
                    .orElseThrow(() -> new IllegalArgumentException("Запись не найдена или недоступна"));
        }
        return timeEntryRepository
                .findByDayLogAndHourAndMinuteAndUser(dayLog, timeEntryDto.getHour(), timeEntryDto.getMinute(), user)
                .orElseGet(() -> createNewTimeEntry(dayLog, timeEntryDto.getHour(), timeEntryDto.getMinute(), user));
    }

    private Status resolveStatus(StatusDto statusDto, User user) {
        if (statusDto == null) {
            return ensureDefaultStatus(user);
        }

        if (statusDto.getId() != null) {
            return statusRepository.findByIdAndUser(statusDto.getId(), user)
                    .orElseThrow(() -> new StatusNotFoundException(statusDto.getId()));
        }

        if (StringUtils.hasText(statusDto.getName())) {
            return statusRepository.findByNameAndUser(statusDto.getName(), user)
                    .orElseThrow(() -> new StatusNotFoundException(statusDto.getName()));
        }

        return ensureDefaultStatus(user);
    }

    private Status ensureDefaultStatus(User user) {
        return statusRepository.findByNameAndUser("Default", user)
                .orElseGet(() -> {
                    Status defaultStatus = new Status();
                    defaultStatus.setName("Default");
                    defaultStatus.setUser(user);
                    return statusRepository.save(defaultStatus);
                });
    }

    private void validateInterval(int startHour, int startMinute, int endHour, int endMinute) {
        if (!isValidTime(startHour, startMinute) || !isValidTime(endHour, endMinute)) {
            throw new IllegalArgumentException("Некорректно указано время начала или окончания");
        }
        if (endHour < startHour || (endHour == startHour && endMinute <= startMinute)) {
            throw new IllegalArgumentException("Время окончания должно быть позже времени начала");
        }
    }

    private boolean isValidTime(int hour, int minute) {
        return hour >= 0 && hour <= 23 && minute >= 0 && minute <= 59;
    }

    private int toMinutes(int hour, int minute) {
        return hour * 60 + minute;
    }

    private void cleanupDayLogIfEmpty(DayLog dayLog) {
        if (dayLog == null) {
            return;
        }
        if (timeEntryRepository.findByDayLog(dayLog).isEmpty()) {
            dayLogRepository.delete(dayLog);
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
