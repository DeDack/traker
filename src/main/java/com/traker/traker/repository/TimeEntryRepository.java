package com.traker.traker.repository;

import com.traker.traker.entity.DayLog;
import com.traker.traker.entity.TimeEntry;
import com.traker.traker.api.DefaultRepository;
import com.traker.traker.entity.User;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TimeEntryRepository extends DefaultRepository<TimeEntry, Long> {
    Optional<TimeEntry> findByDayLogAndHour(DayLog dayLog, int hour);
    List<TimeEntry> findByDayLog(DayLog dayLog);

    List<TimeEntry> findByDayLogAndUser(DayLog dayLog, User user);
    Optional<TimeEntry> findByDayLogAndHourAndUser(DayLog dayLog, int hour, User user);
}
