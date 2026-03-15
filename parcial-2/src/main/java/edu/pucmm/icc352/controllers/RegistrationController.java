package edu.pucmm.icc352.controllers;

import edu.pucmm.icc352.models.Attendance;
import edu.pucmm.icc352.models.Registration;
import edu.pucmm.icc352.models.User;
import edu.pucmm.icc352.services.RegistrationService;
import edu.pucmm.icc352.services.UserService;
import io.javalin.apibuilder.ApiBuilder;
import java.util.List;
import java.util.Map;

public class RegistrationController {

    private static final RegistrationService svc     = new RegistrationService();
    private static final UserService         userSvc = new UserService();

    @SuppressWarnings("unchecked")
    public static void register() {

        // Mis inscripciones - ANTES de /api/registrations/{eventId}
        ApiBuilder.get("/api/registrations/my", ctx -> {
            Long userId = ctx.sessionAttribute("userId");
            if (userId == null) {
                ctx.status(401).json(Map.of("error", "No autenticado."));
                return;
            }
            List<Registration> regs = svc.findByUser(userId);
            ctx.json(regs);
        });

        // Imagen QR - ANTES de rutas con path params genericos
        ApiBuilder.get("/api/registrations/qr/{token}/image", ctx -> {
            Long userId = ctx.sessionAttribute("userId");
            if (userId == null) {
                ctx.status(401);
                return;
            }
            String token = ctx.pathParam("token");
            byte[] qrBytes = svc.generateQRImage(token);
            ctx.contentType("image/png").result(qrBytes);
        });

        // Lista de inscritos a un evento - solo ORGANIZER o ADMIN
        ApiBuilder.get("/api/registrations/event/{eventId}", ctx -> {
            String role = ctx.sessionAttribute("role");
            if (role == null || "PARTICIPANT".equals(role)) {
                ctx.status(403).json(Map.of("error", "Sin permiso."));
                return;
            }
            long eventId = Long.parseLong(ctx.pathParam("eventId"));
            List<Registration> regs = svc.findByEvent(eventId);
            ctx.json(regs);
        });

        // Inscribirse a un evento
        ApiBuilder.post("/api/registrations/{eventId}", ctx -> {
            Long userId = ctx.sessionAttribute("userId");
            if (userId == null) {
                ctx.status(401).json(Map.of("error", "No autenticado."));
                return;
            }
            long eventId = Long.parseLong(ctx.pathParam("eventId"));
            User user = userSvc.findById(userId).orElse(null);
            if (user == null) {
                ctx.status(404).json(Map.of("error", "Usuario no encontrado."));
                return;
            }
            Registration reg = svc.register(user, eventId);
            ctx.status(201).json(Map.of(
                    "registrationId", reg.getId(),
                    "qrToken",        reg.getQrToken(),
                    "message",        "Inscripcion exitosa."
            ));
        });

        // Cancelar inscripcion
        ApiBuilder.delete("/api/registrations/{eventId}", ctx -> {
            Long userId = ctx.sessionAttribute("userId");
            if (userId == null) {
                ctx.status(401).json(Map.of("error", "No autenticado."));
                return;
            }
            long eventId = Long.parseLong(ctx.pathParam("eventId"));
            svc.cancel(userId, eventId);
            ctx.json(Map.of("message", "Inscripcion cancelada."));
        });

        // Escanear QR y marcar asistencia - solo ORGANIZER o ADMIN
        ApiBuilder.post("/api/registrations/scan", ctx -> {
            Long userId = ctx.sessionAttribute("userId");
            String role = ctx.sessionAttribute("role");
            if (userId == null) {
                ctx.status(401).json(Map.of("error", "No autenticado."));
                return;
            }
            if ("PARTICIPANT".equals(role)) {
                ctx.status(403).json(Map.of("error", "Sin permiso."));
                return;
            }
            Map<String, Object> b = (Map<String, Object>) ctx.bodyAsClass(Map.class);
            String token = (String) b.get("qrToken");
            Attendance att = svc.markAttendance(token);
            ctx.status(201).json(Map.of(
                    "message",    "Asistencia registrada.",
                    "scannedAt",  att.getScannedAt().toString(),
                    "attendeeId", att.getRegistration().getUser().getId(),
                    "attendee",   att.getRegistration().getUser().getName()
            ));
        });
    }
}