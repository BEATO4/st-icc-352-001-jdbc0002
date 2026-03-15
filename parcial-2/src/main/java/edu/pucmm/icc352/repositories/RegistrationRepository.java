package edu.pucmm.icc352.repositories;

import edu.pucmm.icc352.models.Registration;
import java.util.List;
import java.util.Optional;

public class RegistrationRepository extends BaseRepository<Registration> {

    public RegistrationRepository() { super(Registration.class); }

    public Optional<Registration> findByToken(String token) {
        try (var s = openSession()) {
            return s.createQuery(
                            "FROM Registration WHERE qrToken = :t", Registration.class)
                    .setParameter("t", token)
                    .uniqueResultOptional();
        }
    }

    public Optional<Registration> findByUserAndEvent(Long userId, Long eventId) {
        try (var s = openSession()) {
            return s.createQuery(
                            "FROM Registration WHERE user.id = :u AND event.id = :e", Registration.class)
                    .setParameter("u", userId)
                    .setParameter("e", eventId)
                    .uniqueResultOptional();
        }
    }

    public List<Registration> findByEvent(Long eventId) {
        try (var s = openSession()) {
            return s.createQuery(
                            "FROM Registration WHERE event.id = :id ORDER BY registeredAt ASC", Registration.class)
                    .setParameter("id", eventId)
                    .list();
        }
    }

    public List<Registration> findByUser(Long userId) {
        try (var s = openSession()) {
            return s.createQuery(
                            "FROM Registration WHERE user.id = :id ORDER BY registeredAt DESC", Registration.class)
                    .setParameter("id", userId)
                    .list();
        }
    }
}
