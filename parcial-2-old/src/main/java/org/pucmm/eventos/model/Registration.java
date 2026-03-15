package org.pucmm.eventos.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "registrations",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "event_id"}))
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

    @Column(unique = true, nullable = false)
    private String qrToken;

    private LocalDateTime registeredAt = LocalDateTime.now();

    @Column(nullable = false)
    private boolean attended = false;

    private LocalDateTime attendedAt;

    public Registration() {}

    // ── Getters & Setters ────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Event getEvent() { return event; }
    public void setEvent(Event event) { this.event = event; }

    public String getQrToken() { return qrToken; }
    public void setQrToken(String qrToken) { this.qrToken = qrToken; }

    public LocalDateTime getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(LocalDateTime registeredAt) { this.registeredAt = registeredAt; }

    public boolean isAttended() { return attended; }
    public void setAttended(boolean attended) { this.attended = attended; }

    public LocalDateTime getAttendedAt() { return attendedAt; }
    public void setAttendedAt(LocalDateTime attendedAt) { this.attendedAt = attendedAt; }
}