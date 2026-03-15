package org.pucmm.eventos.controller;

import io.javalin.http.Context;
import org.pucmm.eventos.model.*;
import org.pucmm.eventos.service.EventService;
import org.pucmm.eventos.util.SessionUtil;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) { this.eventService = eventService; }

    // GET /api/events
    public void list(Context ctx) {
        String role = SessionUtil.getUserRole(ctx);
        boolean isStaff = Role.ADMIN.name().equals(role) || Role.ORGANIZER.name().equals(role);
        List<Event> events = isStaff ? eventService.getAll() : eventService.getPublished();
        ctx.json(events.stream().map(e -> toMap(e, eventService.countRegistrations(e.getId()))).toList());
    }

    // GET /api/events/:id
    public void get(Context ctx) {
        long id = id(ctx);
        Event e = require(id);
        String role = SessionUtil.getUserRole(ctx);
        boolean isStaff = Role.ADMIN.name().equals(role) || Role.ORGANIZER.name().equals(role);
        if (!isStaff && e.getStatus() != EventStatus.PUBLISHED) {
            ctx.status(404).json(AuthController.err("Event not found")); return;
        }
        ctx.json(toMap(e, eventService.countRegistrations(id)));
    }

    // POST /api/events
    public void create(Context ctx) {
        Long uid = SessionUtil.getUserId(ctx);
        String role = SessionUtil.getUserRole(ctx);
        if (uid == null || (!Role.ADMIN.name().equals(role) && !Role.ORGANIZER.name().equals(role))) {
            ctx.status(403).json(AuthController.err("Forbidden")); return;
        }
        Map<?, ?> b = ctx.bodyAsClass(Map.class);
        try {
            Event e = eventService.create(
                    str(b, "title"), str(b, "description"),
                    parseDate(str(b, "dateTime")), str(b, "location"),
                    num(b, "maxCapacity"), uid
            );
            ctx.status(201).json(toMap(e, 0));
        } catch (Exception ex) {
            ctx.status(400).json(AuthController.err(ex.getMessage()));
        }
    }

    // PUT /api/events/:id
    public void update(Context ctx) {
        Long uid = SessionUtil.getUserId(ctx); if (uid == null) { ctx.status(401).json(AuthController.err("Unauthorized")); return; }
        String role = SessionUtil.getUserRole(ctx);
        if (!Role.ADMIN.name().equals(role) && !Role.ORGANIZER.name().equals(role)) {
            ctx.status(403).json(AuthController.err("Forbidden")); return;
        }
        Map<?, ?> b = ctx.bodyAsClass(Map.class);
        try {
            Event e = eventService.update(id(ctx),
                    str(b, "title"), str(b, "description"),
                    parseDate(str(b, "dateTime")), str(b, "location"),
                    num(b, "maxCapacity"), uid, Role.valueOf(role));
            ctx.json(toMap(e, eventService.countRegistrations(e.getId())));
        } catch (SecurityException ex) {
            ctx.status(403).json(AuthController.err(ex.getMessage()));
        } catch (Exception ex) {
            ctx.status(400).json(AuthController.err(ex.getMessage()));
        }
    }

    // POST /api/events/:id/publish
    public void publish(Context ctx)   { setStatus(ctx, EventStatus.PUBLISHED); }
    // POST /api/events/:id/unpublish
    public void unpublish(Context ctx) { setStatus(ctx, EventStatus.DRAFT); }
    // POST /api/events/:id/cancel
    public void cancel(Context ctx)    { setStatus(ctx, EventStatus.CANCELLED); }

    // DELETE /api/events/:id
    public void delete(Context ctx) {
        if (!Role.ADMIN.name().equals(SessionUtil.getUserRole(ctx))) {
            ctx.status(403).json(AuthController.err("Admin only")); return;
        }
        try { eventService.delete(id(ctx)); ctx.json(Map.of("message", "Event deleted")); }
        catch (Exception ex) { ctx.status(404).json(AuthController.err(ex.getMessage())); }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void setStatus(Context ctx, EventStatus status) {
        Long uid  = SessionUtil.getUserId(ctx);
        String role = SessionUtil.getUserRole(ctx);
        if (uid == null || (!Role.ADMIN.name().equals(role) && !Role.ORGANIZER.name().equals(role))) {
            ctx.status(403).json(AuthController.err("Forbidden")); return;
        }
        try {
            Event e = eventService.changeStatus(id(ctx), status, uid, Role.valueOf(role));
            ctx.json(toMap(e, eventService.countRegistrations(e.getId())));
        } catch (SecurityException ex) { ctx.status(403).json(AuthController.err(ex.getMessage())); }
        catch (Exception ex)           { ctx.status(400).json(AuthController.err(ex.getMessage())); }
    }

    private Event require(long id) {
        return eventService.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Event not found"));
    }

    public static Map<String, Object> toMap(Event e, long regCount) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",                e.getId());
        m.put("title",             e.getTitle());
        m.put("description",       e.getDescription());
        m.put("dateTime",          e.getDateTime() != null ? e.getDateTime().toString() : null);
        m.put("location",          e.getLocation());
        m.put("maxCapacity",       e.getMaxCapacity());
        m.put("status",            e.getStatus().name());
        m.put("registrationCount", regCount);
        m.put("createdAt",         e.getCreatedAt() != null ? e.getCreatedAt().toString() : null);
        if (e.getCreatedBy() != null) {
            m.put("createdBy", Map.of("id", e.getCreatedBy().getId(),
                    "username", e.getCreatedBy().getUsername()));
        }
        return m;
    }

    private static long     id(Context ctx) { return Long.parseLong(ctx.pathParam("id")); }
    private static String   str(Map<?, ?> m, String k) { Object v = m.get(k); return v == null ? null : v.toString(); }
    private static int      num(Map<?, ?> m, String k) { Object v = m.get(k); return v == null ? 0 : ((Number) v).intValue(); }
    private static LocalDateTime parseDate(String s) {
        if (s == null || s.isBlank()) throw new IllegalArgumentException("dateTime is required");
        return LocalDateTime.parse(s, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}