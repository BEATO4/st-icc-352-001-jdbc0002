package edu.pucmm.icc352.repositories;

import edu.pucmm.icc352.models.Attendance;
import java.util.List;
import java.util.Optional;

public class AttendanceRepository extends BaseRepository<Attendance> {

    public AttendanceRepository() { super(Attendance.class); }

    public boolean existsByRegistration(Long registrationId) {
        try (var s = openSession()) {
            Long count = s.createQuery(
                            "SELECT COUNT(a) FROM Attendance a WHERE a.registration.id = :id", Long.class)
                    .setParameter("id", registrationId)
                    .uniqueResult();
            return count != null && count > 0;
        }
    }

    // Para las estadísticas: asistencia agrupada por hora
    public List<Object[]> countByHour(Long eventId) {
        try (var s = openSession()) {
            return s.createQuery(
                            "SELECT HOUR(a.scannedAt), COUNT(a) FROM Attendance a " +
                                    "WHERE a.registration.event.id = :id GROUP BY HOUR(a.scannedAt) ORDER BY 1",
                            Object[].class)
                    .setParameter("id", eventId)
                    .list();
        }
    }

    public Optional<Attendance> findByRegistration(Long registrationId) {
        try (var s = openSession()) {
            return s.createQuery(
                            "FROM Attendance WHERE registration.id = :id", Attendance.class)
                    .setParameter("id", registrationId)
                    .uniqueResultOptional();
        }
    }
}
