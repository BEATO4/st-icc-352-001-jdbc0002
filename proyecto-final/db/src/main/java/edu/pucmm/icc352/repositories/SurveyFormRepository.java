package edu.pucmm.icc352.repositories;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Sorts;
import edu.pucmm.icc352.db.MongoDBConnection;
import edu.pucmm.icc352.models.SurveyForm;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repository class for SurveyForm database operations
 */
public class SurveyFormRepository {
    private static final Logger logger = LoggerFactory.getLogger(SurveyFormRepository.class);
    private final MongoCollection<SurveyForm> collection;

    public SurveyFormRepository(MongoDatabase database) {
        this.collection = database.getCollection("survey_forms", SurveyForm.class);
        createIndexes();
    }

    public SurveyFormRepository() {
        this(MongoDBConnection.getInstance().getDatabase());
    }

    /**
     * Create indexes for the survey_forms collection
     */
    private void createIndexes() {
        // Create index on userId for faster queries
        collection.createIndex(Indexes.ascending("userId"));
        // Create index on createdAt for sorting
        collection.createIndex(Indexes.descending("createdAt"));
        // Create 2dsphere index for geospatial queries (optional, for future use)
        collection.createIndex(Indexes.geo2dsphere("location"));
        logger.info("Created indexes on survey_forms collection");
    }

    /**
     * Create a new survey form
     */
    public SurveyForm create(SurveyForm form) {
        try {
            collection.insertOne(form);
            logger.info("Survey form created for user: {}", form.getUsername());
            return form;
        } catch (Exception e) {
            logger.error("Error creating survey form", e);
            throw new RuntimeException("Failed to create survey form", e);
        }
    }

    /**
     * Find survey form by ID
     */
    public Optional<SurveyForm> findById(ObjectId id) {
        try {
            SurveyForm form = collection.find(Filters.eq("_id", id)).first();
            return Optional.ofNullable(form);
        } catch (Exception e) {
            logger.error("Error finding survey form by ID: {}", id, e);
            return Optional.empty();
        }
    }

    /**
     * Find survey form by ID string
     */
    public Optional<SurveyForm> findById(String id) {
        try {
            return findById(new ObjectId(id));
        } catch (IllegalArgumentException e) {
            logger.error("Invalid ObjectId format: {}", id);
            return Optional.empty();
        }
    }

    /**
     * Find all survey forms
     */
    public List<SurveyForm> findAll() {
        try {
            return collection.find()
                    .sort(Sorts.descending("createdAt"))
                    .into(new ArrayList<>());
        } catch (Exception e) {
            logger.error("Error finding all survey forms", e);
            return new ArrayList<>();
        }
    }

    /**
     * Find all survey forms by user ID
     */
    public List<SurveyForm> findByUserId(String userId) {
        try {
            return collection.find(Filters.eq("userId", userId))
                    .sort(Sorts.descending("createdAt"))
                    .into(new ArrayList<>());
        } catch (Exception e) {
            logger.error("Error finding survey forms by user ID: {}", userId, e);
            return new ArrayList<>();
        }
    }

    /**
     * Find all survey forms by username
     */
    public List<SurveyForm> findByUsername(String username) {
        try {
            return collection.find(Filters.eq("username", username))
                    .sort(Sorts.descending("createdAt"))
                    .into(new ArrayList<>());
        } catch (Exception e) {
            logger.error("Error finding survey forms by username: {}", username, e);
            return new ArrayList<>();
        }
    }

    /**
     * Update a survey form
     */
    public boolean update(SurveyForm form) {
        try {
            return collection.replaceOne(
                    Filters.eq("_id", form.getId()),
                    form
            ).getModifiedCount() > 0;
        } catch (Exception e) {
            logger.error("Error updating survey form: {}", form.getId(), e);
            return false;
        }
    }

    /**
     * Delete a survey form by ID
     */
    public boolean delete(ObjectId id) {
        try {
            return collection.deleteOne(Filters.eq("_id", id)).getDeletedCount() > 0;
        } catch (Exception e) {
            logger.error("Error deleting survey form by ID: {}", id, e);
            return false;
        }
    }

    /**
     * Delete a survey form by ID string
     */
    public boolean delete(String id) {
        try {
            return delete(new ObjectId(id));
        } catch (IllegalArgumentException e) {
            logger.error("Invalid ObjectId format: {}", id);
            return false;
        }
    }

    /**
     * Count total survey forms
     */
    public long count() {
        return collection.countDocuments();
    }

    /**
     * Count survey forms by user ID
     */
    public long countByUserId(String userId) {
        return collection.countDocuments(Filters.eq("userId", userId));
    }

    /**
     * Find forms with geolocation data
     */
    public List<SurveyForm> findAllWithLocation() {
        try {
            return collection.find(
                    Filters.and(
                            Filters.ne("latitude", null),
                            Filters.ne("longitude", null)
                    )
            ).sort(Sorts.descending("createdAt"))
             .into(new ArrayList<>());
        } catch (Exception e) {
            logger.error("Error finding survey forms with location", e);
            return new ArrayList<>();
        }
    }
}
