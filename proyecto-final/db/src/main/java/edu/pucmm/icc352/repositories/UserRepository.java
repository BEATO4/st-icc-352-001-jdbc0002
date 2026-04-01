package edu.pucmm.icc352.repositories;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import edu.pucmm.icc352.db.MongoDBConnection;
import edu.pucmm.icc352.models.User;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repository class for User database operations
 */
public class UserRepository {
    private static final Logger logger = LoggerFactory.getLogger(UserRepository.class);
    private final MongoCollection<User> collection;

    public UserRepository(MongoDatabase database) {
        this.collection = database.getCollection("users", User.class);
        createIndexes();
    }

    public UserRepository() {
        this(MongoDBConnection.getInstance().getDatabase());
    }

    /**
     * Create indexes for the users collection
     */
    private void createIndexes() {
        // Create unique index on username
        collection.createIndex(
                Indexes.ascending("username"),
                new IndexOptions().unique(true)
        );
        logger.info("Created unique index on username field");
    }

    /**
     * Create a new user
     */
    public User create(User user) {
        try {
            collection.insertOne(user);
            logger.info("User created: {}", user.getUsername());
            return user;
        } catch (Exception e) {
            logger.error("Error creating user: {}", user.getUsername(), e);
            throw new RuntimeException("Failed to create user", e);
        }
    }

    /**
     * Find user by ID
     */
    public Optional<User> findById(ObjectId id) {
        try {
            User user = collection.find(Filters.eq("_id", id)).first();
            return Optional.ofNullable(user);
        } catch (Exception e) {
            logger.error("Error finding user by ID: {}", id, e);
            return Optional.empty();
        }
    }

    /**
     * Find user by ID string
     */
    public Optional<User> findById(String id) {
        try {
            return findById(new ObjectId(id));
        } catch (IllegalArgumentException e) {
            logger.error("Invalid ObjectId format: {}", id);
            return Optional.empty();
        }
    }

    /**
     * Find user by username
     */
    public Optional<User> findByUsername(String username) {
        try {
            User user = collection.find(Filters.eq("username", username)).first();
            return Optional.ofNullable(user);
        } catch (Exception e) {
            logger.error("Error finding user by username: {}", username, e);
            return Optional.empty();
        }
    }

    /**
     * Find all users
     */
    public List<User> findAll() {
        try {
            return collection.find().into(new ArrayList<>());
        } catch (Exception e) {
            logger.error("Error finding all users", e);
            return new ArrayList<>();
        }
    }

    /**
     * Update a user
     */
    public boolean update(User user) {
        try {
            return collection.replaceOne(
                    Filters.eq("_id", user.getId()),
                    user
            ).getModifiedCount() > 0;
        } catch (Exception e) {
            logger.error("Error updating user: {}", user.getUsername(), e);
            return false;
        }
    }

    /**
     * Delete a user by ID
     */
    public boolean delete(ObjectId id) {
        try {
            return collection.deleteOne(Filters.eq("_id", id)).getDeletedCount() > 0;
        } catch (Exception e) {
            logger.error("Error deleting user by ID: {}", id, e);
            return false;
        }
    }

    /**
     * Delete a user by ID string
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
     * Check if username exists
     */
    public boolean existsByUsername(String username) {
        return collection.countDocuments(Filters.eq("username", username)) > 0;
    }

    /**
     * Count total users
     */
    public long count() {
        return collection.countDocuments();
    }
}
