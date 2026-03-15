package edu.pucmm.icc352.services;

import edu.pucmm.icc352.models.*;
import edu.pucmm.icc352.repositories.EventRepository;
import edu.pucmm.icc352.repositories.UserRepository;
import java.time.LocalDateTime;
import java.util.List;

public class EventService {

    private final EventRepository repo     = new EventRepository();
    private final UserRepository  userRepo = new UserRepository();

    public Event create(String title, String description, LocalDateTime dateTime,
                        String location, int maxCapacity, Long organizerId) {
        User organizer = userRepo.findById(organizerId)
                .orElseThrow(() -> new IllegalArgumentException("Organizador no encontrado."));
        if (dateTime.isBefore(LocalDateTime.now()))
            throw new IllegalArgumentException("La fecha del evento debe ser futura.");
        if (maxCapacity < 1)
            throw new IllegalArgumentException("El cupo debe ser al menos 1.");
        return repo.save(new Event(title, description, dateTime, location, maxCapacity, organizer));
    }

    public Event update(Long eventId, Long requesterId, String title, String description,
                        LocalDateTime dateTime, String location, int maxCapacity) {
        Event event = getEditableEvent(eventId, requesterId);
        event.setTitle(title);
        event.setDescription(description);
        event.setDateTime(dateTime);
        event.setLocation(location);
        event.setMaxCapacity(maxCapacity);
        return repo.update(event);
    }

    public Event setPublished(Long eventId, Long requesterId, boolean publish) {
        Event event = getEditableEvent(eventId, requesterId);
        if (event.getStatus() == EventStatus.CANCELLED)
            throw new IllegalStateException("No se puede publicar un evento cancelado.");
        event.setStatus(publish ? EventStatus.PUBLISHED : EventStatus.DRAFT);
        return repo.update(event);
    }

    public Event cancel(Long eventId, Long requesterId) {
        Event event = getEditableEvent(eventId, requesterId);
        event.setStatus(EventStatus.CANCELLED);
        return repo.update(event);
    }

    public void adminDelete(Long eventId) {
        Event event = repo.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Evento no encontrado."));
        repo.delete(event);
    }

    public List<Event> findPublished()           { return repo.findPublished(); }
    public List<Event> findAll()                 { return repo.findAll(); }
    public List<Event> findByOrganizer(Long id)  { return repo.findByOrganizer(id); }

    public Event findById(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Evento no encontrado."));
    }

    private Event getEditableEvent(Long eventId, Long requesterId) {
        Event event = repo.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Evento no encontrado."));
        User requester = userRepo.findById(requesterId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));
        boolean isAdmin = requester.getRole() == Role.ADMIN;
        boolean isOwner = event.getOrganizer().getId().equals(requesterId);
        if (!isAdmin && !isOwner)
            throw new SecurityException("No tienes permiso para modificar este evento.");
        return event;
    }
}