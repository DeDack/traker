package com.traker.traker.service;

import com.traker.traker.entity.DayLog;
import com.traker.traker.entity.Status;
import com.traker.traker.entity.TimeEntry;
import com.traker.traker.entity.User;
import com.traker.traker.exception.StatusNotFoundException;
import com.traker.traker.repository.DayLogRepository;
import com.traker.traker.repository.StatusRepository;
import com.traker.traker.repository.TimeEntryRepository;
import com.traker.traker.repository.UserRepository;
import com.traker.traker.dto.TimeEntryDto;
import com.traker.traker.mapper.TimeEntryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
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
        return dayLog.map(dl -> timeEntryRepository.findByDayLogAndUser(dl, currentUser).stream()
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

        int hour = timeEntryDto.getHour();
        logger.info("Час из объекта timeEntryDto: {}", hour);

        TimeEntry timeEntry = timeEntryRepository.findByDayLogAndHourAndUser(dayLog, hour, currentUser)
                .orElseGet(() -> {
                    logger.info("TimeEntry не найден для DayLog: {}, часа: {} и пользователя: {}. Создание нового TimeEntry.", dayLog, hour, currentUser.getUsername());
                    return createNewTimeEntry(dayLog, hour, currentUser);
                });

        Status status;
        if (timeEntryDto.getStatus() != null && timeEntryDto.getStatus().getName() != null) {
            String statusName = timeEntryDto.getStatus().getName();
            logger.info("Статус, указанный в timeEntryDto: {}", statusName);
            status = statusRepository.findByNameAndUser(statusName, currentUser)
                    .orElseThrow(() -> new StatusNotFoundException(statusName));
        } else {
            logger.info("Статус не указан в timeEntryDto. Использование статуса по умолчанию.");
            status = statusRepository.findByNameAndUser("Default", currentUser)
                    .orElseGet(() -> {
                        logger.info("Статус по умолчанию 'Default' не найден. Создание нового.");
                        Status defaultStatus = new Status();
                        defaultStatus.setName("Default");
                        defaultStatus.setUser(currentUser);
                        return statusRepository.save(defaultStatus);
                    });
        }

        timeEntryMapper.updateEntityFromDto(timeEntryDto, timeEntry);
        timeEntry.setStatus(status);
        timeEntry.setUser(currentUser);

        TimeEntry savedTimeEntry = timeEntryRepository.save(timeEntry);
        logger.info("Сохраненный TimeEntry: {}", savedTimeEntry);

        return timeEntryMapper.toDto(savedTimeEntry);
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
     * Создает новый объект TimeEntry для заданного DayLog, часа и пользователя.
     *
     * @param dayLog объект DayLog
     * @param hour час записи времени
     * @param user текущий пользователь
     * @return новый объект TimeEntry
     */
    private TimeEntry createNewTimeEntry(DayLog dayLog, int hour, User user) {
        TimeEntry newTimeEntry = new TimeEntry();
        newTimeEntry.setDayLog(dayLog);
        newTimeEntry.setHour(hour);
        newTimeEntry.setUser(user);
        return newTimeEntry;
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