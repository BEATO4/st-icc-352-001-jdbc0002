package edu.pucmm.icc352.backend.controllers;

import edu.pucmm.icc352.backend.dto.SurveyFormRequest;
import edu.pucmm.icc352.backend.middleware.JwtAuthFilter;
import edu.pucmm.icc352.backend.services.SurveyFormService;
import edu.pucmm.icc352.models.SurveyForm;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SurveyFormController {
    private static final Logger logger = LoggerFactory.getLogger(SurveyFormController.class);
    private final SurveyFormService surveyFormService;

    public SurveyFormController(SurveyFormService surveyFormService) {
        this.surveyFormService = surveyFormService;
    }

    public SurveyFormController() {
        this(new SurveyFormService());
    }

    public void create(Context ctx) {
        try {
            String userId   = ctx.attribute("userId");
            String username = ctx.attribute("username");

            SurveyFormRequest request = ctx.bodyAsClass(SurveyFormRequest.class);

            if (!surveyFormService.isValidEducationalLevel(request.getEducationalLevel())) {
                ctx.status(400).json(Map.of(
                        "success", false,
                        "message", "Nivel educativo inválido. Debe ser uno de: BASICO, MEDIO, GRADO_UNIVERSITARIO, POSTGRADO, DOCTORADO"
                ));
                return;
            }

            SurveyForm form = new SurveyForm(
                    request.getName(),
                    request.getSector(),
                    request.getEducationalLevel(),
                    request.getLatitude(),
                    request.getLongitude(),
                    request.getPhotoBase64(),
                    userId,
                    username
            );

            SurveyForm created = surveyFormService.createForm(form);

            ctx.status(201).json(Map.of(
                    "success", true,
                    "message", "Survey form created successfully",
                    "form", Map.of("id", created.getIdAsString() != null ? created.getIdAsString() : "")
            ));
        } catch (IllegalArgumentException e) {
            ctx.status(400).json(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error creating survey form", e);
            ctx.status(500).json(Map.of("success", false, "message", "Internal server error: " + e.getMessage()));
        }
    }


    public void getAll(Context ctx) {
        try {
            // Para pruebas y sincronización con gRPC, mostrar TODOS los formularios
            List<SurveyForm> forms = surveyFormService.getAllForms();

            ctx.json(Map.of(
                    "success", true,
                    "count", forms.size(),
                    "forms", forms.stream().map(this::toMapSummary).toList()
            ));
        } catch (Exception e) {
            logger.error("Error retrieving survey forms", e);
            ctx.status(500).json(Map.of("success", false, "message", "Internal server error: " + e.getMessage()));
        }
    }

    public void getById(Context ctx) {
        try {
            String id = ctx.pathParam("id");
            Optional<SurveyForm> formOpt = surveyFormService.getFormById(id);

            if (formOpt.isEmpty()) {
                ctx.status(404).json(Map.of("success", false, "message", "Survey form not found"));
                return;
            }

            SurveyForm form = formOpt.get();

            if (!JwtAuthFilter.isAdmin(ctx)) {
                String userId = ctx.attribute("userId");
                if (!form.getUserId().equals(userId)) {
                    ctx.status(403).json(Map.of("success", false, "message", "Access denied"));
                    return;
                }
            }

            ctx.json(Map.of("success", true, "form", toMap(form)));
        } catch (Exception e) {
            logger.error("Error retrieving survey form by ID", e);
            ctx.status(500).json(Map.of("success", false, "message", "Internal server error: " + e.getMessage()));
        }
    }

    public void getByUserId(Context ctx) {
        try {
            String targetUserId = ctx.pathParam("userId");

            if (!JwtAuthFilter.isAdmin(ctx)) {
                String requestingUserId = ctx.attribute("userId");
                if (!targetUserId.equals(requestingUserId)) {
                    ctx.status(403).json(Map.of("success", false, "message", "Access denied"));
                    return;
                }
            }

            List<SurveyForm> forms = surveyFormService.getFormsByUserId(targetUserId);
            ctx.json(Map.of(
                    "success", true,
                    "count", forms.size(),
                    "forms", forms.stream().map(this::toMapSummary).toList()
            ));
        } catch (Exception e) {
            logger.error("Error retrieving forms by user ID", e);
            ctx.status(500).json(Map.of("success", false, "message", "Internal server error: " + e.getMessage()));
        }
    }

    public void getWithLocation(Context ctx) {
        try {
            JwtAuthFilter.requireAdmin(ctx);
            if (ctx.status().getCode() == 403) return;

            List<SurveyForm> forms = surveyFormService.getFormsWithLocation();
            ctx.json(Map.of(
                    "success", true,
                    "count", forms.size(),
                    "forms", forms.stream().map(this::toMapSummary).toList()
            ));
        } catch (Exception e) {
            logger.error("Error retrieving forms with location", e);
            ctx.status(500).json(Map.of("success", false, "message", "Internal server error: " + e.getMessage()));
        }
    }

    public void update(Context ctx) {
        try {
            String id = ctx.pathParam("id");
            Optional<SurveyForm> formOpt = surveyFormService.getFormById(id);

            if (formOpt.isEmpty()) {
                ctx.status(404).json(Map.of("success", false, "message", "Survey form not found"));
                return;
            }

            SurveyForm form = formOpt.get();

            if (!JwtAuthFilter.isAdmin(ctx)) {
                String userId = ctx.attribute("userId");
                if (!form.getUserId().equals(userId)) {
                    ctx.status(403).json(Map.of("success", false, "message", "Access denied"));
                    return;
                }
            }

            SurveyFormRequest request = ctx.bodyAsClass(SurveyFormRequest.class);

            if (request.getName() != null && !request.getName().isBlank())
                form.setName(request.getName());
            if (request.getSector() != null && !request.getSector().isBlank())
                form.setSector(request.getSector());
            if (request.getEducationalLevel() != null && !request.getEducationalLevel().isBlank()) {
                if (!surveyFormService.isValidEducationalLevel(request.getEducationalLevel())) {
                    ctx.status(400).json(Map.of(
                            "success", false,
                            "message", "Nivel educativo inválido. Debe ser uno de: BASICO, MEDIO, GRADO_UNIVERSITARIO, POSTGRADO, DOCTORADO"
                    ));
                    return;
                }
                form.setEducationalLevel(request.getEducationalLevel());
            }
            if (request.getLatitude() != null)  form.setLatitude(request.getLatitude());
            if (request.getLongitude() != null) form.setLongitude(request.getLongitude());
            if (request.getPhotoBase64() != null) form.setPhotoBase64(request.getPhotoBase64());

            boolean updated = surveyFormService.updateForm(form);

            if (updated) {
                ctx.json(Map.of("success", true, "message", "Survey form updated successfully", "form", toMap(form)));
            } else {
                ctx.status(500).json(Map.of("success", false, "message", "Failed to update survey form"));
            }
        } catch (Exception e) {
            logger.error("Error updating survey form", e);
            ctx.status(500).json(Map.of("success", false, "message", "Internal server error: " + e.getMessage()));
        }
    }

    public void delete(Context ctx) {
        try {
            String id = ctx.pathParam("id");
            Optional<SurveyForm> formOpt = surveyFormService.getFormById(id);

            if (formOpt.isEmpty()) {
                ctx.status(404).json(Map.of("success", false, "message", "Survey form not found"));
                return;
            }

            if (!JwtAuthFilter.isAdmin(ctx)) {
                String userId = ctx.attribute("userId");
                if (!formOpt.get().getUserId().equals(userId)) {
                    ctx.status(403).json(Map.of("success", false, "message", "Access denied"));
                    return;
                }
            }

            boolean deleted = surveyFormService.deleteForm(id);

            if (deleted) {
                ctx.json(Map.of("success", true, "message", "Survey form deleted successfully"));
            } else {
                ctx.status(500).json(Map.of("success", false, "message", "Failed to delete survey form"));
            }
        } catch (Exception e) {
            logger.error("Error deleting survey form", e);
            ctx.status(500).json(Map.of("success", false, "message", "Internal server error: " + e.getMessage()));
        }
    }

    private Map<String, Object> toMap(SurveyForm f) {
        return Map.ofEntries(
                Map.entry("id",               f.getIdAsString() != null ? f.getIdAsString() : ""),
                Map.entry("name",             f.getName() != null ? f.getName() : ""),
                Map.entry("sector",           f.getSector() != null ? f.getSector() : ""),
                Map.entry("educationalLevel", f.getEducationalLevel() != null ? f.getEducationalLevel() : ""),
                Map.entry("latitude",         f.getLatitude() != null ? f.getLatitude() : 0.0),
                Map.entry("longitude",        f.getLongitude() != null ? f.getLongitude() : 0.0),
                Map.entry("photoBase64",      f.getPhotoBase64() != null ? f.getPhotoBase64() : ""),
                Map.entry("userId",           f.getUserId() != null ? f.getUserId() : ""),
                Map.entry("username",         f.getUsername() != null ? f.getUsername() : ""),
                Map.entry("synced",           f.isSynced()),
                Map.entry("createdAt",        f.getCreatedAt() != null ? f.getCreatedAt().toString() : ""),
                Map.entry("updatedAt",        f.getUpdatedAt() != null ? f.getUpdatedAt().toString() : "")
        );
    }

    private Map<String, Object> toMapSummary(SurveyForm f) {
        return Map.ofEntries(
                Map.entry("id",               f.getIdAsString() != null ? f.getIdAsString() : ""),
                Map.entry("name",             f.getName() != null ? f.getName() : ""),
                Map.entry("sector",           f.getSector() != null ? f.getSector() : ""),
                Map.entry("educationalLevel", f.getEducationalLevel() != null ? f.getEducationalLevel() : ""),
                Map.entry("latitude",         f.getLatitude() != null ? f.getLatitude() : 0.0),
                Map.entry("longitude",        f.getLongitude() != null ? f.getLongitude() : 0.0),
                Map.entry("hasPhoto",         f.getPhotoBase64() != null && !f.getPhotoBase64().isEmpty()),
                Map.entry("userId",           f.getUserId() != null ? f.getUserId() : ""),
                Map.entry("username",         f.getUsername() != null ? f.getUsername() : ""),
                Map.entry("synced",           f.isSynced()),
                Map.entry("createdAt",        f.getCreatedAt() != null ? f.getCreatedAt().toString() : ""),
                Map.entry("updatedAt",        f.getUpdatedAt() != null ? f.getUpdatedAt().toString() : "")
        );
    }
}