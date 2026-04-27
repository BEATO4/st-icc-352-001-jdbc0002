package edu.pucmm.icc352.backend.services;

import edu.pucmm.icc352.models.SurveyForm;
import edu.pucmm.icc352.repositories.SurveyFormRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;


public class SurveyFormService {
    private static final Logger logger = LoggerFactory.getLogger(SurveyFormService.class);
    private final SurveyFormRepository formRepository;

    public SurveyFormService(SurveyFormRepository formRepository) {
        this.formRepository = formRepository;
    }

    public SurveyFormService() {
        this(new SurveyFormRepository());
    }

    public SurveyForm createForm(SurveyForm form) {
        try {
            if (form.getName() == null || form.getName().trim().isEmpty()) {
                throw new IllegalArgumentException("Nombre es requerido");
            }
            if (form.getSector() == null || form.getSector().trim().isEmpty()) {
                throw new IllegalArgumentException("Sector es requerido");
            }
            if (form.getEducationalLevel() == null || form.getEducationalLevel().trim().isEmpty()) {
                throw new IllegalArgumentException("Nivel educativo es requerido");
            }
            if (form.getUserId() == null || form.getUserId().trim().isEmpty()) {
                throw new IllegalArgumentException("User ID es requerido");
            }

            return formRepository.create(form);
        } catch (Exception e) {
            logger.error("Error creating survey form", e);
            throw new RuntimeException("Failed to create survey form: " + e.getMessage(), e);
        }
    }


    public Optional<SurveyForm> getFormById(String id) {
        return formRepository.findById(id);
    }


    public List<SurveyForm> getAllForms() {
        return formRepository.findAll();
    }


    public List<SurveyForm> getFormsByUserId(String userId) {
        return formRepository.findByUserId(userId);
    }


    public List<SurveyForm> getFormsByUsername(String username) {
        return formRepository.findByUsername(username);
    }

    public List<SurveyForm> getFormsWithLocation() {
        return formRepository.findAllWithLocation();
    }


    public boolean updateForm(SurveyForm form) {
        try {
            return formRepository.update(form);
        } catch (Exception e) {
            logger.error("Error updating survey form", e);
            return false;
        }
    }


    public boolean deleteForm(String id) {
        try {
            return formRepository.delete(id);
        } catch (Exception e) {
            logger.error("Error deleting survey form", e);
            return false;
        }
    }


    public long getFormCount() {
        return formRepository.count();
    }


    public long getFormCountByUserId(String userId) {
        return formRepository.countByUserId(userId);
    }


    public boolean isValidEducationalLevel(String level) {
        if (level == null)
            return false;
        String value = level.trim().toUpperCase();
        return switch (value) {
            case "BASICO", "MEDIO", "GRADO_UNIVERSITARIO", "POSTGRADO", "DOCTORADO" -> true;
            default -> false;
        };
    }
}
