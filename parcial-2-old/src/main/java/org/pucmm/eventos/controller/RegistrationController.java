package org.pucmm.eventos.controller;

import io.javalin.http.Context;
import org.pucmm.eventos.model.*;
import org.pucmm.eventos.service.RegistrationService;
import org.pucmm.eventos.util.QRUtil;
import org.pucmm.eventos.util.SessionUtil;

import java.util.*;
import java.util.stream.Collectors;

public class RegistrationController {

    private final RegistrationService regService;

    public RegistrationController(RegistrationService regService) { this.regService = regService; }

    // POST /api/events/:id/register
    public void register(Context ctx) {
        Long uid = SessionUtil.getUserId(ctx);
        if (uid == null) { ctx.status(401).json(AuthController.err("Unauthorized")); return; }

        long eventId = Long.parseLong(ctx.pathParam("id"));
        try {
            Registration reg = regService.register(eventId, uid);
            ctx.status(201).json(regToMap(reg, true));
        } catch (IllegalStateException e) {
            ctx.status(409).json(AuthController.err(e.getMessage()));
        } catch (IllegalArgumentException e) {
            ctx.status(404).json(AuthController.err(e.getMessage()));
        }
    }

    // DELETE /api/events/:id/register
    public void cancel(Context ctx) {
        Long uid = SessionUtil.getUserId(ctx);
        if (uid == null) { ctx.status(401).json(AuthController.err("Unauthorized")); return; }

        long eventId = Long.parseLong(ctx.pathParam("id"));
        try {
            regService.cancel(eventId, uid);
            ctx.json(Map.of("message", "Registration cancelled"));
        } catch (IllegalStateException e) {
            ctx.status(409).json(AuthController.err(e.getMessage()));
        } catch (IllegalArgumentException e) {
            ctx.status(404).json(AuthController.err(e.getMessage()));
        }
    }

    // GET /api/events/:id/myregistration
    public void myRegistration(Context ctx) {
        Long uid = SessionUtil.getUserId(ctx);
        if (uid == null) { ctx.status(404).json(Map.of("registered", false)); return; }

        long eventId = Long.parseLong(ctx.pathParam("id"));
        regService.findByEventAndUser(eventId, uid)
                .ifPresentOrElse(
                        reg -> ctx.json(regToMap(reg, true)),
                        () -> ctx.status(404).json(Map.of("registered", false))
                );
    }

    // GET /api/events/:id/registrations  (organizer / admin)
    public void listRegistrations(Context ctx) {
        String role = SessionUtil.getUserRole(ctx);
        if (!Role.ADMIN.name().equals(role) && !Role.ORGANIZER.name().equals(role)) {
            ctx.status(403).json(AuthController.err("Forbidden")); return;
        }
        long eventId = Long.parseLong(ctx.pathParam("id"));
        List<Map<String, Object>> result = regService.getEventRegistrations(eventId)
                .stream().map(r -> regToMap(r, false)).collect(Collectors.toList());
        ctx.json(result);
    }

    // POST /api/attendance/scan
    public void scanQR(Context ctx) {
        String role = SessionUtil.getUserRole(ctx);
        if (!Role.ADMIN.name().equals(role) && !Role.ORGANIZER.name().equals(role)) {
            ctx.status(403).json(AuthController.err("Forbidden")); return;
        }
        Map<?, ?> body = ctx.bodyAsClass(Map.class);
        String qrContent = body.get("qrContent") != null ? body.get("qrContent").toString() : null;
        if (qrContent == null) { ctx.status(400).json(AuthController.err("qrContent is required")); return; }

        try {
            Registration reg = regService.markAttendance(qrContent);
            Map<String, Object> res = new LinkedHashMap<>();
            res.put("success", true);
            res.put("message", "Attendance marked");
            res.put("username", reg.getUser().getUsername());
            res.put("email",    reg.getUser().getEmail());
            res.put("eventId",  reg.getEvent().getId());
            res.put("eventTitle", reg.getEvent().getTitle());
            res.put("attendedAt", reg.getAttendedAt() != null ? reg.getAttendedAt().toString() : null);
            ctx.json(res);
        } catch (IllegalStateException e) {
            ctx.status(409).json(AuthController.err(e.getMessage()));
        } catch (IllegalArgumentException e) {
            ctx.status(404).json(AuthController.err(e.getMessage()));
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Map<String, Object> regToMap(Registration r, boolean includeQR) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",           r.getId());
        m.put("registered",   true);
        m.put("qrToken",      r.getQrToken());
        m.put("registeredAt", r.getRegisteredAt().toString());
        m.put("attended",     r.isAttended());
        if (r.getAttendedAt() != null) m.put("attendedAt", r.getAttendedAt().toString());
        if (includeQR)  m.put("qrImage", QRUtil.generateQRBase64(r.getQrToken()));
        if (r.getUser() != null) {
            try {
                m.put("user", Map.of(
                        "id",       r.getUser().getId(),
                        "username", r.getUser().getUsername(),
                        "email",    r.getUser().getEmail()
                ));
            } catch (Exception ignored) {}
        }
        return m;
    }
}