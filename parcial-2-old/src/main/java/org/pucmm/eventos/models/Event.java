package org.pucmm.eventos.models;

import org.pucmm.eventos.enums.EventStatus;
import org.pucmm.eventos.enums.EventType;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private LocalDateTime eventDate;       // Fecha y hora del evento

    @Column(nullable = false, length = 250)
    private String location;               // Lugar del evento

    @Column(nullable = false)
    private int maxCapacity;               // Cupo máximo

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventStatus status = EventStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventType type = EventType.OTRO;

    // Organizador que creó el evento
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizer_id", nullable = false)
    private User organizer;

    // Inscripciones al evento
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Registration> registrations = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    // ─── Lifecycle ────────────────────────────────────────────────────────────
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt  = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ─── Constructors ─────────────────────────────────────────────────────────
    public Event() {}

    public Event(String title, String description, LocalDateTime eventDate,
                 String location, int maxCapacity, EventType type, User organizer) {
        this.title       = title;
        this.description = description;
        this.eventDate   = eventDate;
        this.location    = location;
        this.maxCapacity = maxCapacity;
        this.type        = type;
        this.organizer   = organizer;
    }

    // ─── Getters & Setters ────────────────────────────────────────────────────
    public Long getId()                               { return id; }
    public void setId(Long id)                        { this.id = id; }

    public String getTitle()                          { return title; }
    public void setTitle(String title)                { this.title = title; }

    public String getDescription()                    { return description; }
    public void setDescription(String description)    { this.description = description; }

    public LocalDateTime getEventDate()               { return eventDate; }
    public void setEventDate(LocalDateTime eventDate) { this.eventDate = eventDate; }

    public String getLocation()                       { return location; }
    public void setLocation(String location)          { this.location = location; }

    public int getMaxCapacity()                       { return maxCapacity; }
    public void setMaxCapacity(int maxCapacity)       { this.maxCapacity = maxCapacity; }

    public EventStatus getStatus()                    { return status; }
    public void setStatus(EventStatus status)         { this.status = status; }

    public EventType getType()                        { return type; }
    public void setType(EventType type)               { this.type = type; }

    public User getOrganizer()                        { return organizer; }
    public void setOrganizer(User organizer)          { this.organizer = organizer; }

    public List<Registration> getRegistrations()      { return registrations; }

    public LocalDateTime getCreatedAt()               { return createdAt; }
    public LocalDateTime getUpdatedAt()               { return updatedAt; }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /** Inscripciones activas (no canceladas) */
    public long getActiveRegistrationsCount() {
        return registrations.stream()
                .filter(r -> !r.isCancelled())
                .count();
    }

    /** Cupos disponibles */
    public int getAvailableSpots() {
        return maxCapacity - (int) getActiveRegistrationsCount();
    }

    /** ¿Hay cupo? */
    public boolean hasAvailableSpots() {
        return getAvailableSpots() > 0;
    }

    public boolean isPublished()  { return EventStatus.PUBLISHED.equals(this.status); }
    public boolean isCancelled()  { return EventStatus.CANCELLED.equals(this.status); }
    public boolean isDraft()      { return EventStatus.DRAFT.equals(this.status); }

    /** El evento ya ocurrió */
    public boolean hasPassed() {
        return LocalDateTime.now().isAfter(this.eventDate);
    }

    @Override
    public String toString() {
        return "Event{id=" + id + ", title='" + title + "', status=" + status + "}";
    }
}