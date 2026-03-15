package org.pucmm.eventos.repository;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.pucmm.eventos.model.Registration;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class RegistrationRepository {

    private final SessionFactory sf;

    public RegistrationRepository(SessionFactory sf) { this.sf = sf; }

    /**
     * Persists a brand-new registration using getReference() to avoid
     * detached-entity errors on the user/event associations.
     */
    public Registration saveNew(Long eventId, Long userId, String qrToken) {
        try (Session s = sf.openSession()) {
            Transaction tx = s.beginTransaction();

            Registration reg = new Registration();
            reg.setUser(s.getReference(org.pucmm.eventos.model.User.class, userId));
            reg.setEvent(s.getReference(org.pucmm.eventos.model.Event.class, eventId));
            reg.setQrToken(qrToken);

            s.persist(reg);
            tx.commit();

            Long newId = reg.getId();
            // Reload with EAGER associations inside the still-open session
            return s.createQuery(
                    "SELECT r FROM Registration r " +
                            "JOIN FETCH r.user JOIN FETCH r.event WHERE r.id = :id",
                    Registration.class
            ).setParameter("id", newId).uniqueResult();
        }
    }

    /** Marks attendance using a bulk HQL UPDATE — no detached-entity issues. */
    public void markAttendance(Long registrationId, LocalDateTime attendedAt) {
        try (Session s = sf.openSession()) {
            Transaction tx = s.beginTransaction();
            s.createMutationQuery(
                    "UPDATE Registration SET attended = true, attendedAt = :at WHERE id = :id"
            ).setParameter("at", attendedAt).setParameter("id", registrationId).executeUpdate();
            tx.commit();
        }
    }

    public Optional<Registration> findById(Long id) {
        try (Session s = sf.openSession()) {
            return s.createQuery(
                    "SELECT r FROM Registration r JOIN FETCH r.user JOIN FETCH r.event WHERE r.id = :id",
                    Registration.class
            ).setParameter("id", id).uniqueResultOptional();
        }
    }

    public Optional<Registration> findByEventAndUser(Long eventId, Long userId) {
        try (Session s = sf.openSession()) {
            return s.createQuery(
                    "SELECT r FROM Registration r JOIN FETCH r.user JOIN FETCH r.event " +
                            "WHERE r.event.id = :eid AND r.user.id = :uid",
                    Registration.class
            ).setParameter("eid", eventId).setParameter("uid", userId).uniqueResultOptional();
        }
    }

    public Optional<Registration> findByQrToken(String qrToken) {
        try (Session s = sf.openSession()) {
            return s.createQuery(
                    "SELECT r FROM Registration r JOIN FETCH r.user JOIN FETCH r.event " +
                            "WHERE r.qrToken = :tok",
                    Registration.class
            ).setParameter("tok", qrToken).uniqueResultOptional();
        }
    }

    public List<Registration> findByEventId(Long eventId) {
        try (Session s = sf.openSession()) {
            return s.createQuery(
                    "SELECT r FROM Registration r JOIN FETCH r.user " +
                            "WHERE r.event.id = :eid ORDER BY r.registeredAt DESC",
                    Registration.class
            ).setParameter("eid", eventId).list();
        }
    }

    public List<Registration> findByUserId(Long userId) {
        try (Session s = sf.openSession()) {
            return s.createQuery(
                    "SELECT r FROM Registration r JOIN FETCH r.event " +
                            "WHERE r.user.id = :uid ORDER BY r.registeredAt DESC",
                    Registration.class
            ).setParameter("uid", userId).list();
        }
    }

    public long countByEventId(Long eventId) {
        try (Session s = sf.openSession()) {
            Long c = s.createQuery(
                    "SELECT COUNT(r) FROM Registration r WHERE r.event.id = :eid", Long.class
            ).setParameter("eid", eventId).uniqueResult();
            return c != null ? c : 0;
        }
    }

    public void delete(Long id) {
        try (Session s = sf.openSession()) {
            Transaction tx = s.beginTransaction();
            s.createMutationQuery("DELETE FROM Registration WHERE id = :id")
                    .setParameter("id", id).executeUpdate();
            tx.commit();
        }
    }

    public void deleteByEventId(Long eventId) {
        try (Session s = sf.openSession()) {
            Transaction tx = s.beginTransaction();
            s.createMutationQuery("DELETE FROM Registration WHERE event.id = :eid")
                    .setParameter("eid", eventId).executeUpdate();
            tx.commit();
        }
    }
}