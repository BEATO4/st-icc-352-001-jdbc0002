package org.pucmm.eventos.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Representa la inscripción de un participante a un evento.
 * Contiene el token único para el código QR y registra la asistencia.
 */
@Entity
@Table(
        name = "registrations",
        uniqueConstraints = {
                // Un usuario no puede inscribirse dos veces al mismo evento
                @UniqueConstraint(columnNames = {"participant_id", "event_id"})
        }
)
public class Registration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_id", nullable = false)
    private User participant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    /**
     * Token único que se codifica en el QR.
     * Formato: eventId|userId|uuid
     */
    @Column(nullable = false, unique = true, length = 500)
    private String qrToken;

    @Column(nullable = false, updatable = false)
    private LocalDateTime registeredAt;

    /** ¿El participante asistió al evento? */
    @Column(nullable = false)
    private boolean attended = false;

    /** Momento exacto en que se escaneó/validó el QR */
    @Column
    private LocalDateTime attendedAt;

    /** El participante canceló su inscripción */
    @Column(nullable = false)
    private boolean cancelled = false;

    @Column
    private LocalDateTime cancelledAt;

    // ─── Lifecycle ────────────────────────────────────────────────────────────
    @PrePersist
    protected void onCreate() {
        this.registeredAt = LocalDateTime.now();
        // Genera el token QR automáticamente si no fue asignado antes
        if (this.qrToken == null || this.qrToken.isBlank()) {
            generateQrToken();
        }
    }

    // ─── Constructors ─────────────────────────────────────────────────────────
    public Registration() {}

    public Registration(User participant, Event event) {
        this.participant = participant;
        this.event       = event;
        generateQrToken();
    }

    // ─── QR Token ─────────────────────────────────────────────────────────────

    /**
     * Genera un token único codificando: eventId|userId|uuid
     * Este valor será el contenido del código QR.
     */
    public void generateQrToken() {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        // El token se puede construir antes de persistir si los IDs no están disponibles
        // Por eso también se llama desde el constructor y se regenera en @PrePersist si es necesario
        this.qrToken = (event != null ? event.getId() : "?") + "|"
                + (participant != null ? participant.getId() : "?") + "|"
                + uuid;
    }

    /**
     * Regenera el token con los IDs reales (llamar después de persistir por primera vez).
     */
    public void refreshQrToken() {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        this.qrToken = event.getId() + "|" + participant.getId() + "|" + uuid;
    }

    // ─── Attendance ───────────────────────────────────────────────────────────

    /**
     * Marca la asistencia del participante.
     * Lanza excepción si ya fue marcado o si la inscripción está cancelada.
     */
    public void markAttendance() {
        if (this.cancelled) {
            throw new IllegalStateException("No se puede marcar asistencia: la inscripción está cancelada.");
        }
        if (this.attended) {
            throw new IllegalStateException("La asistencia ya fue registrada anteriormente.");
        }
        this.attended   = true;
        this.attendedAt = LocalDateTime.now();
    }

    /** Cancela la inscripción */
    public void cancel() {
        if (this.attended) {
            throw new IllegalStateException("No se puede cancelar: el participante ya asistió.");
        }
        this.cancelled   = true;
        this.cancelledAt = LocalDateTime.now();
    }

    // ─── Getters & Setters ────────────────────────────────────────────────────
    public Long getId()                                   { return id; }
    public void setId(Long id)                            { this.id = id; }

    public User getParticipant()                          { return participant; }
    public void setParticipant(User participant)          { this.participant = participant; }

    public Event getEvent()                               { return event; }
    public void setEvent(Event event)                     { this.event = event; }

    public String getQrToken()                            { return qrToken; }
    public void setQrToken(String qrToken)                { this.qrToken = qrToken; }

    public LocalDateTime getRegisteredAt()                { return registeredAt; }

    public boolean isAttended()                           { return attended; }
    public void setAttended(boolean attended)             { this.attended = attended; }

    public LocalDateTime getAttendedAt()                  { return attendedAt; }
    public void setAttendedAt(LocalDateTime attendedAt)   { this.attendedAt = attendedAt; }

    public boolean isCancelled()                          { return cancelled; }
    public void setCancelled(boolean cancelled)           { this.cancelled = cancelled; }

    public LocalDateTime getCancelledAt()                 { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }

    // ─── Helpers ──────────────────────────────────────────────────────────────
    public boolean isActive() { return !this.cancelled; }

    @Override
    public String toString() {
        return "Registration{id=" + id
                + ", participant=" + (participant != null ? participant.getUsername() : "null")
                + ", event=" + (event != null ? event.getTitle() : "null")
                + ", attended=" + attended + "}";
    }
}