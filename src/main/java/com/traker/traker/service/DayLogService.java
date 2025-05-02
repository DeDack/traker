package com.traker.traker.service;

import com.traker.traker.entity.DayLog;
import com.traker.traker.entity.TimeEntry;
import com.traker.traker.repository.DayLogRepository;
import com.traker.traker.repository.TimeEntryRepository;
import com.traker.traker.dto.entity.TimeEntryDto;
import com.traker.traker.mapper.TimeEntryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Сервисный класс для управления сущностями DayLog и TimeEntry.
 */
@Service
@RequiredArgsConstructor
public class DayLogService {
    private final DayLogRepository dayLogRepository;
    private final TimeEntryRepository timeEntryRepository;
    private final TimeEntryMapper timeEntryMapper;

    /**
     * Получает список объектов TimeEntryDto для заданной даты.
     *
     * @param date строка даты в формате (yyyy-MM-dd)
     * @return список объектов TimeEntryDto
     */
    public List<TimeEntryDto> getTimeEntriesByDate(String date) {
        LocalDate localDate = parseDate(date);
        Optional<DayLog> dayLog = dayLogRepository.findByDate(localDate);
        return dayLog.map(dl -> timeEntryRepository.findByDayLog(dl).stream()
                        .map(timeEntryMapper::toDto)
                        .collect(Collectors.toList()))
                .orElseGet(List::of);
    }

    /**
     * Обновляет или создает TimeEntry для заданной даты.
     *
     * @param date строка даты в формате (yyyy-MM-dd)
     * @param timeEntryDto объект DTO, содержащий детали записи времени
     * @return обновленный или созданный объект TimeEntryDto
     */
    @Transactional
    public TimeEntryDto updateTimeEntry(String date, TimeEntryDto timeEntryDto) {
        LocalDate localDate = parseDate(date);
        DayLog dayLog = dayLogRepository.findByDate(localDate).orElseGet(() -> createNewDayLog(localDate));

        // Получение часа из DTO
        int hour = timeEntryDto.getHour();

        // Проверка существования записи времени
        TimeEntry timeEntry = timeEntryRepository.findByDayLogAndHour(dayLog, hour)
                .orElseGet(() -> createNewTimeEntry(dayLog, hour));

        // Обновление существующей записи времени
        timeEntryMapper.updateEntityFromDto(timeEntryDto, timeEntry);

        // Сохранение записи времени в рамках транзакции
        TimeEntry savedTimeEntry = timeEntryRepository.save(timeEntry);

        // Возвращение обновленного объекта DTO
        return timeEntryMapper.toDto(savedTimeEntry);
    }

    /**
     * Парсит строку даты в объект LocalDate.
     *
     * @param date строка даты в формате (yyyy-MM-dd)
     * @return объект LocalDate
     */
    private LocalDate parseDate(String date) {
        return LocalDate.parse(date);
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
     * Создает новый объект TimeEntry для заданного DayLog и часа.
     *
     * @param dayLog объект DayLog
     * @param hour час записи времени
     * @return новый объект TimeEntry
     */
    private TimeEntry createNewTimeEntry(DayLog dayLog, int hour) {
        TimeEntry newTimeEntry = new TimeEntry();
        newTimeEntry.setDayLog(dayLog);
        newTimeEntry.setHour(hour);
        return newTimeEntry;
    }
}