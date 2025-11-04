package com.traker.traker.repository;

import com.traker.traker.api.DefaultRepository;
import com.traker.traker.entity.DayLog;
import com.traker.traker.entity.TimeEntry;
import com.traker.traker.entity.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TimeEntryRepository extends DefaultRepository<TimeEntry, Long> {
    List<TimeEntry> findByDayLog(DayLog dayLog);

    List<TimeEntry> findByDayLogAndUserOrderByHourAscMinuteAsc(DayLog dayLog, User user);

    List<TimeEntry> findByDayLogAndUser(DayLog dayLog, User user);

    Optional<TimeEntry> findByDayLogAndHourAndMinuteAndUser(DayLog dayLog, int hour, int minute, User user);

    Optional<TimeEntry> findByIdAndUser(Long id, User user);

    @Query("""
        SELECT te FROM TimeEntry te
        WHERE te.dayLog = :dayLog
          AND te.user = :user
          AND ((te.hour * 60 + te.minute) < :endMinutes)
          AND (:startMinutes < (te.endHour * 60 + te.endMinute))
          AND (:excludeId IS NULL OR te.id <> :excludeId)
    """)
    List<TimeEntry> findOverlappingEntries(
            @Param("dayLog") DayLog dayLog,
            @Param("user") User user,
            @Param("startMinutes") int startMinutes,
            @Param("endMinutes") int endMinutes,
            @Param("excludeId") Long excludeId);
}
