package org.pucmm.eventos.repository;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.pucmm.eventos.model.Event;
import org.pucmm.eventos.model.EventStatus;

import java.util.List;
import java.util.Optional;

public class EventRepository {

    private final SessionFactory sf;

    public EventRepository(SessionFactory sf) { this.sf = sf; }

    public Event save(Event event) {
        try (Session s = sf.openSession()) {
            Transaction tx = s.beginTransaction();
            if (event.getId() == null) {
                s.persist(event);
            } else {
                event = s.merge(event);
            }
            tx.commit();
            return event;
        }
    }

    /** Saves a new event whose createdBy is identified by userId (avoids detached-entity issues). */
    public Event saveNew(Event event, Long createdByUserId) {
        try (Session s = sf.openSession()) {
            Transaction tx = s.beginTransaction();
            event.setCreatedBy(s.getReference(org.pucmm.eventos.model.User.class, createdByUserId));
            s.persist(event);
            tx.commit();
            // reload with eager associations
            return s.get(Event.class, event.getId());
        }
    }

    public Optional<Event> findById(Long id) {
        try (Session s = sf.openSession()) {
            // EAGER fetch avoids LazyInitializationException outside the session
            return s.createQuery(
                    "SELECT e FROM Event e LEFT JOIN FETCH e.createdBy WHERE e.id = :id",
                    Event.class
            ).setParameter("id", id).uniqueResultOptional();
        }
    }

    public List<Event> findAll() {
        try (Session s = sf.openSession()) {
            return s.createQuery(
                    "SELECT e FROM Event e LEFT JOIN FETCH e.createdBy ORDER BY e.createdAt DESC",
                    Event.class
            ).list();
        }
    }

    public List<Event> findByStatus(EventStatus status) {
        try (Session s = sf.openSession()) {
            return s.createQuery(
                    "SELECT e FROM Event e LEFT JOIN FETCH e.createdBy " +
                            "WHERE e.status = :st ORDER BY e.dateTime ASC",
                    Event.class
            ).setParameter("st", status).list();
        }
    }

    public void delete(Long id) {
        try (Session s = sf.openSession()) {
            Transaction tx = s.beginTransaction();
            Event ref = s.get(Event.class, id);
            if (ref != null) s.remove(ref);
            tx.commit();
        }
    }

    public Event update(Long id, java.util.function.Consumer<Event> updater) {
        try (Session s = sf.openSession()) {
            Transaction tx = s.beginTransaction();
            Event event = s.get(Event.class, id);
            if (event == null) throw new IllegalArgumentException("Event not found: " + id);
            updater.accept(event);
            tx.commit();
            // re-fetch with createdBy
            return s.createQuery(
                    "SELECT e FROM Event e LEFT JOIN FETCH e.createdBy WHERE e.id = :id",
                    Event.class
            ).setParameter("id", event.getId()).uniqueResult();
        }
    }
}