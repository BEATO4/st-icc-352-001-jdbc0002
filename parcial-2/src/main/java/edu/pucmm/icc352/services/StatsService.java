package edu.pucmm.icc352.services;

import edu.pucmm.icc352.models.Event;
import edu.pucmm.icc352.repositories.AttendanceRepository;
import edu.pucmm.icc352.repositories.EventRepository;
import edu.pucmm.icc352.repositories.RegistrationRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class StatsService {

    private final EventRepository        eventRepo = new EventRepository();
    private final RegistrationRepository regRepo   = new RegistrationRepository();
    private final AttendanceRepository   attRepo   = new AttendanceRepository();

    /**
     * Returns a full statistics map for an event.
     */
    public Map<String, Object> getEventStats(long eventId) {
        Event event = eventRepo.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Evento no encontrado."));

        long totalRegistrations = regRepo.findByEvent(eventId).size();
        long totalAttendances   = eventRepo.countAttendance(eventId);
        long absentees          = Math.max(0, totalRegistrations - totalAttendances);
        double attendanceRate   = totalRegistrations > 0
                ? Math.round((totalAttendances * 100.0 / totalRegistrations) * 100.0) / 100.0
                : 0.0;

        // Hour-by-hour breakdown (0–23)
        Map<Integer, Long> byHour = new LinkedHashMap<>();
        for (int h = 0; h < 24; h++) byHour.put(h, 0L);
        List<Object[]> rows = attRepo.countByHour(eventId);
        for (Object[] row : rows) {
            int  hour  = ((Number) row[0]).intValue();
            long count = ((Number) row[1]).longValue();
            byHour.put(hour, count);
        }

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("eventId",            eventId);
        stats.put("eventTitle",         event.getTitle());
        stats.put("totalRegistrations", totalRegistrations);
        stats.put("totalAttendances",   totalAttendances);
        stats.put("absentees",          absentees);
        stats.put("attendanceRate",     attendanceRate);
        stats.put("maxCapacity",        event.getMaxCapacity());
        stats.put("attendanceByHour",   byHour);
        return stats;
    }
}