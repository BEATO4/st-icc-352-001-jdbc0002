package edu.pucmm.icc352.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "registrations",
        // Esta constraint en la BD evita inscripciones duplicadas a nivel de DB
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "event_id"})
)
public class Registration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    // Token único que se codifica en el QR
    // Formato: eventId:userId:uuid  — fácil de validar al escanear
    @Column(nullable = false, unique = true)
    private String qrToken;

    @Column(nullable = false, updatable = false)
    private LocalDateTime registeredAt = LocalDateTime.now();

    // ── Constructores ──────────────────────────────────────────
    public Registration() {}

    public Registration(User user, Event event) {
        this.user = user;
        this.event = event;
        // Genera el token automáticamente al crear la inscripción
        this.qrToken = event.getId() + ":" + user.getId() + ":" + UUID.randomUUID();
    }

    // ── Getters ────────────────────────────────────────────────
    public Long getId()                      { return id; }
    public User getUser()                    { return user; }
    public Event getEvent()                  { return event; }
    public String getQrToken()               { return qrToken; }
    public LocalDateTime getRegisteredAt()   { return registeredAt; }
}