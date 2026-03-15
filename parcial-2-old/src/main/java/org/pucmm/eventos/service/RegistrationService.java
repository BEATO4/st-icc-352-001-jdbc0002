package org.pucmm.eventos.service;

import org.pucmm.eventos.model.*;
import org.pucmm.eventos.repository.EventRepository;
import org.pucmm.eventos.repository.RegistrationRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class RegistrationService {

    private final RegistrationRepository regRepo;
    private final EventRepository eventRepo;

    public RegistrationService(RegistrationRepository regRepo, EventRepository eventRepo) {
        this.regRepo   = regRepo;
        this.eventRepo = eventRepo;
    }

    public Registration register(Long eventId, Long userId) {
        Event event = eventRepo.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));

        if (event.getStatus() != EventStatus.PUBLISHED)
            throw new IllegalStateException("Event is not open for registration");

        if (regRepo.findByEventAndUser(eventId, userId).isPresent())
            throw new IllegalStateException("Already registered for this event");

        long current = regRepo.countByEventId(eventId);
        if (current >= event.getMaxCapacity())
            throw new IllegalStateException("Event has reached its maximum capacity");

        String qrToken = eventId + ":" + userId + ":" + UUID.randomUUID();
        return regRepo.saveNew(eventId, userId, qrToken);
    }

    public void cancel(Long eventId, Long userId) {
        Registration reg = regRepo.findByEventAndUser(eventId, userId)
                .orElseThrow(() -> new IllegalArgumentException("No active registration found"));

        Event event = reg.getEvent();
        if (event.getDateTime() != null && LocalDateTime.now().isAfter(event.getDateTime()))
            throw new IllegalStateException("Cannot cancel after the event has started");

        regRepo.delete(reg.getId());
    }

    public Registration markAttendance(String qrToken) {
        Registration reg = regRepo.findByQrToken(qrToken)
                .orElseThrow(() -> new IllegalArgumentException("QR code not recognized"));

        if (reg.isAttended())
            throw new IllegalStateException("Attendance already recorded for this registration");

        regRepo.markAttendance(reg.getId(), LocalDateTime.now());

        // Return a fresh copy with updated fields
        return regRepo.findById(reg.getId()).orElse(reg);
    }

    public Optional<Registration> findByEventAndUser(Long eventId, Long userId) {
        return regRepo.findByEventAndUser(eventId, userId);
    }

    public List<Registration> getEventRegistrations(Long eventId) {
        return regRepo.findByEventId(eventId);
    }
}