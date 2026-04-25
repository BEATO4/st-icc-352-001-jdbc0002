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


    private void createIndexes() {
        collection.createIndex(Indexes.ascending("userId"));
        collection.createIndex(Indexes.descending("createdAt"));
        collection.createIndex(Indexes.ascending("latitude"));
        collection.createIndex(Indexes.ascending("longitude"));
        logger.info("Created indexes on survey_forms collection");
    }

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

    public Optional<SurveyForm> findById(ObjectId id) {
        try {
            SurveyForm form = collection.find(Filters.eq("_id", id)).first();
            return Optional.ofNullable(form);
        } catch (Exception e) {
            logger.error("Error finding survey form by ID: {}", id, e);
            return Optional.empty();
        }
    }

    public Optional<SurveyForm> findById(String id) {
        try {
            return findById(new ObjectId(id));
        } catch (IllegalArgumentException e) {
            logger.error("Invalid ObjectId format: {}", id);
            return Optional.empty();
        }
    }

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

    public boolean delete(ObjectId id) {
        try {
            return collection.deleteOne(Filters.eq("_id", id)).getDeletedCount() > 0;
        } catch (Exception e) {
            logger.error("Error deleting survey form by ID: {}", id, e);
            return false;
        }
    }


    public boolean delete(String id) {
        try {
            return delete(new ObjectId(id));
        } catch (IllegalArgumentException e) {
            logger.error("Invalid ObjectId format: {}", id);
            return false;
        }
    }

    public long count() {
        return collection.countDocuments();
    }

    public long countByUserId(String userId) {
        return collection.countDocuments(Filters.eq("userId", userId));
    }

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
