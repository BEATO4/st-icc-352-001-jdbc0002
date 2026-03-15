package org.pucmm.eventos.service;

import org.pucmm.eventos.model.Registration;
import org.pucmm.eventos.repository.RegistrationRepository;

import java.util.*;
import java.util.stream.Collectors;

public class StatsService {

    private final RegistrationRepository regRepo;

    public StatsService(RegistrationRepository regRepo) { this.regRepo = regRepo; }

    public Map<String, Object> getEventStats(Long eventId) {
        List<Registration> all = regRepo.findByEventId(eventId);
        List<Registration> attended = all.stream().filter(Registration::isAttended).toList();

        long total     = all.size();
        long totalAtt  = attended.size();
        double pct     = total > 0 ? Math.round((double) totalAtt / total * 1000.0) / 10.0 : 0.0;

        // Registrations by day  (label = yyyy-MM-dd)
        Map<String, Long> regByDay = new TreeMap<>(
                all.stream().collect(Collectors.groupingBy(
                        r -> r.getRegisteredAt().toLocalDate().toString(),
                        Collectors.counting()
                ))
        );

        // Attendance by hour (label = "HH:00")
        Map<Integer, Long> attByHourRaw = attended.stream()
                .filter(r -> r.getAttendedAt() != null)
                .collect(Collectors.groupingBy(
                        r -> r.getAttendedAt().getHour(),
                        Collectors.counting()
                ));
        Map<String, Long> attByHour = new TreeMap<>();
        attByHourRaw.forEach((h, c) -> attByHour.put(String.format("%02d:00", h), c));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalRegistered",     total);
        result.put("totalAttended",        totalAtt);
        result.put("attendancePercentage", pct);
        result.put("registrationsByDay",   toChartData(regByDay));
        result.put("attendanceByHour",     toChartData(attByHour));
        return result;
    }

    private List<Map<String, Object>> toChartData(Map<String, Long> data) {
        return data.entrySet().stream()
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("label", e.getKey());
                    m.put("value", e.getValue());
                    return m;
                })
                .collect(Collectors.toList());
    }
}