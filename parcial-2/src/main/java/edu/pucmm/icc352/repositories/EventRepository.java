package edu.pucmm.icc352.repositories;

import edu.pucmm.icc352.models.Event;
import edu.pucmm.icc352.models.EventStatus;
import java.util.List;

public class EventRepository extends BaseRepository<Event> {

    public EventRepository() { super(Event.class); }

    // Solo eventos publicados (vista de participantes)
    public List<Event> findPublished() {
        try (var s = openSession()) {
            return s.createQuery(
                            "FROM Event WHERE status = :s ORDER BY dateTime ASC", Event.class)
                    .setParameter("s", EventStatus.PUBLISHED)
                    .list();
        }
    }

    // Todos los eventos de un organizador
    public List<Event> findByOrganizer(Long organizerId) {
        try (var s = openSession()) {
            return s.createQuery(
                            "FROM Event WHERE organizer.id = :id ORDER BY dateTime DESC", Event.class)
                    .setParameter("id", organizerId)
                    .list();
        }
    }

    // Conteo de inscritos para validar cupo
    public long countRegistrations(Long eventId) {
        try (var s = openSession()) {
            return s.createQuery(
                            "SELECT COUNT(r) FROM Registration r WHERE r.event.id = :id", Long.class)
                    .setParameter("id", eventId)
                    .uniqueResult();
        }
    }

    // Conteo de asistentes (para estadísticas)
    public long countAttendance(Long eventId) {
        try (var s = openSession()) {
            return s.createQuery(
                            "SELECT COUNT(a) FROM Attendance a WHERE a.registration.event.id = :id", Long.class)
                    .setParameter("id", eventId)
                    .uniqueResult();
        }
    }
}