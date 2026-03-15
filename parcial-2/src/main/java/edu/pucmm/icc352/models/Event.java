package edu.pucmm.icc352.models;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private LocalDateTime dateTime;

    @Column(nullable = false, length = 200)
    private String location;

    @Column(nullable = false)
    private int maxCapacity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventStatus status = EventStatus.DRAFT;

    // El organizador que creó este evento
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "organizer_id", nullable = false)
    private User organizer;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // ── Constructores ──────────────────────────────────────────
    public Event() {}

    public Event(String title, String description, LocalDateTime dateTime,
                 String location, int maxCapacity, User organizer) {
        this.title = title;
        this.description = description;
        this.dateTime = dateTime;
        this.location = location;
        this.maxCapacity = maxCapacity;
        this.organizer = organizer;
    }

    // ── Getters y Setters ──────────────────────────────────────
    public Long getId()                         { return id; }
    public String getTitle()                    { return title; }
    public void setTitle(String t)              { this.title = t; }
    public String getDescription()              { return description; }
    public void setDescription(String d)        { this.description = d; }
    public LocalDateTime getDateTime()          { return dateTime; }
    public void setDateTime(LocalDateTime dt)   { this.dateTime = dt; }
    public String getLocation()                 { return location; }
    public void setLocation(String l)           { this.location = l; }
    public int getMaxCapacity()                 { return maxCapacity; }
    public void setMaxCapacity(int m)           { this.maxCapacity = m; }
    public EventStatus getStatus()              { return status; }
    public void setStatus(EventStatus s)        { this.status = s; }
    public User getOrganizer()                  { return organizer; }
    public void setOrganizer(User o)            { this.organizer = o; }
    public LocalDateTime getCreatedAt()         { return createdAt; }
}