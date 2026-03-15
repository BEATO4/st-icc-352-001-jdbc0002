package org.pucmm.eventos.service;

import org.pucmm.eventos.model.*;
import org.pucmm.eventos.repository.EventRepository;
import org.pucmm.eventos.repository.RegistrationRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class EventService {

    private final EventRepository eventRepo;
    private final RegistrationRepository regRepo;

    public EventService(EventRepository eventRepo, RegistrationRepository regRepo) {
        this.eventRepo = eventRepo;
        this.regRepo   = regRepo;
    }

    public Event create(String title, String description, LocalDateTime dateTime,
                        String location, int maxCapacity, Long createdByUserId) {
        Event e = new Event();
        e.setTitle(title);
        e.setDescription(description);
        e.setDateTime(dateTime);
        e.setLocation(location);
        e.setMaxCapacity(maxCapacity);
        e.setStatus(EventStatus.DRAFT);
        return eventRepo.saveNew(e, createdByUserId);
    }

    public Event update(Long id, String title, String description, LocalDateTime dateTime,
                        String location, int maxCapacity, Long requestorId, Role requestorRole) {
        Event existing = require(id);
        checkOwnership(existing, requestorId, requestorRole);
        return eventRepo.update(id, ev -> {
            ev.setTitle(title);
            ev.setDescription(description);
            ev.setDateTime(dateTime);
            ev.setLocation(location);
            ev.setMaxCapacity(maxCapacity);
        });
    }

    public Event changeStatus(Long id, EventStatus newStatus, Long requestorId, Role requestorRole) {
        Event existing = require(id);
        checkOwnership(existing, requestorId, requestorRole);
        return eventRepo.update(id, ev -> ev.setStatus(newStatus));
    }

    public void delete(Long id) {
        regRepo.deleteByEventId(id);
        eventRepo.delete(id);
    }

    public List<Event> getPublished()   { return eventRepo.findByStatus(EventStatus.PUBLISHED); }
    public List<Event> getAll()         { return eventRepo.findAll(); }
    public Optional<Event> findById(Long id) { return eventRepo.findById(id); }
    public long countRegistrations(Long eventId) { return regRepo.countByEventId(eventId); }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Event require(Long id) {
        return eventRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + id));
    }

    private void checkOwnership(Event event, Long userId, Role role) {
        if (role == Role.ADMIN) return;
        if (!event.getCreatedBy().getId().equals(userId))
            throw new SecurityException("You do not own this event");
    }
}