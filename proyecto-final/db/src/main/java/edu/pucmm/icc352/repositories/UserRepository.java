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

    private void createIndexes() {
        collection.createIndex(
                Indexes.ascending("username"),
                new IndexOptions().unique(true)
        );
        logger.info("Created unique index on username field");
    }

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

    public Optional<User> findById(ObjectId id) {
        try {
            User user = collection.find(Filters.eq("_id", id)).first();
            return Optional.ofNullable(user);
        } catch (Exception e) {
            logger.error("Error finding user by ID: {}", id, e);
            return Optional.empty();
        }
    }

    public Optional<User> findById(String id) {
        try {
            return findById(new ObjectId(id));
        } catch (IllegalArgumentException e) {
            logger.error("Invalid ObjectId format: {}", id);
            return Optional.empty();
        }
    }


    public Optional<User> findByUsername(String username) {
        try {
            User user = collection.find(Filters.eq("username", username)).first();
            return Optional.ofNullable(user);
        } catch (Exception e) {
            logger.error("Error finding user by username: {}", username, e);
            return Optional.empty();
        }
    }


    public List<User> findAll() {
        try {
            return collection.find().into(new ArrayList<>());
        } catch (Exception e) {
            logger.error("Error finding all users", e);
            return new ArrayList<>();
        }
    }

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

    public boolean delete(ObjectId id) {
        try {
            return collection.deleteOne(Filters.eq("_id", id)).getDeletedCount() > 0;
        } catch (Exception e) {
            logger.error("Error deleting user by ID: {}", id, e);
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

    public boolean existsByUsername(String username) {
        return collection.countDocuments(Filters.eq("username", username)) > 0;
    }

    public long count() {
        return collection.countDocuments();
    }
}
