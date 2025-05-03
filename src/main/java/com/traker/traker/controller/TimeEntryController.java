package com.traker.traker.controller;

import com.traker.traker.controller.api.TimeEntryControllerApi;
import com.traker.traker.service.TimeEntryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TimeEntryController implements TimeEntryControllerApi {

    private final TimeEntryService timeEntryService;

    /**
     * Эндпоинт для получения общего количества отработанных часов за указанную дату.
     *
     * @param date дата, для которой нужно получить общее количество отработанных часов, в формате yyyy-MM-dd
     * @return общее количество отработанных часов за указанную дату
     */
    @Override
    public int getDailyStats(@RequestParam("date") String date) {
        return timeEntryService.getTotalHoursWorked(date);
    }
}