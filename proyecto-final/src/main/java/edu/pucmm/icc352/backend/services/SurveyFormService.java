package edu.pucmm.icc352.backend.services;

import edu.pucmm.icc352.models.SurveyForm;
import edu.pucmm.icc352.repositories.SurveyFormRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * Service for survey form operations
 */
public class SurveyFormService {
    private static final Logger logger = LoggerFactory.getLogger(SurveyFormService.class);
    private final SurveyFormRepository formRepository;

    public SurveyFormService(SurveyFormRepository formRepository) {
        this.formRepository = formRepository;
    }

    public SurveyFormService() {
        this(new SurveyFormRepository());
    }

    /**
     * Create a new survey form
     */
    public SurveyForm createForm(SurveyForm form) {
        try {
            // Validate required fields
            if (form.getName() == null || form.getName().trim().isEmpty()) {
                throw new IllegalArgumentException("Name is required");
            }
            if (form.getSector() == null || form.getSector().trim().isEmpty()) {
                throw new IllegalArgumentException("Sector is required");
            }
            if (form.getEducationalLevel() == null || form.getEducationalLevel().trim().isEmpty()) {
                throw new IllegalArgumentException("Educational level is required");
            }
            if (form.getUserId() == null || form.getUserId().trim().isEmpty()) {
                throw new IllegalArgumentException("User ID is required");
            }

            return formRepository.create(form);
        } catch (Exception e) {
            logger.error("Error creating survey form", e);
            throw new RuntimeException("Failed to create survey form: " + e.getMessage(), e);
        }
    }

    /**
     * Get survey form by ID
     */
    public Optional<SurveyForm> getFormById(String id) {
        return formRepository.findById(id);
    }

    /**
     * Get all survey forms
     */
    public List<SurveyForm> getAllForms() {
        return formRepository.findAll();
    }

    /**
     * Get all survey forms by user ID
     */
    public List<SurveyForm> getFormsByUserId(String userId) {
        return formRepository.findByUserId(userId);
    }

    /**
     * Get all survey forms by username
     */
    public List<SurveyForm> getFormsByUsername(String username) {
        return formRepository.findByUsername(username);
    }

    /**
     * Get all forms with geolocation data
     */
    public List<SurveyForm> getFormsWithLocation() {
        return formRepository.findAllWithLocation();
    }

    /**
     * Update survey form
     */
    public boolean updateForm(SurveyForm form) {
        try {
            return formRepository.update(form);
        } catch (Exception e) {
            logger.error("Error updating survey form", e);
            return false;
        }
    }

    /**
     * Delete survey form
     */
    public boolean deleteForm(String id) {
        try {
            return formRepository.delete(id);
        } catch (Exception e) {
            logger.error("Error deleting survey form", e);
            return false;
        }
    }

    /**
     * Get total form count
     */
    public long getFormCount() {
        return formRepository.count();
    }

    /**
     * Get form count by user ID
     */
    public long getFormCountByUserId(String userId) {
        return formRepository.countByUserId(userId);
    }

    /**
     * Validate educational level
     */
    public boolean isValidEducationalLevel(String level) {
        return level != null && (
                level.equalsIgnoreCase("PRIMARY") ||
                level.equalsIgnoreCase("SECONDARY") ||
                level.equalsIgnoreCase("UNIVERSITY_DEGREE") ||
                level.equalsIgnoreCase("POSTGRADUATE") ||
                level.equalsIgnoreCase("DOCTORAL")
        );
    }
}
