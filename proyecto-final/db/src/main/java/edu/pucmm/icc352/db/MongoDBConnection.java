package edu.pucmm.icc352.db;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

/**
 * Singleton class for MongoDB connection management
 */
public class MongoDBConnection {
    private static final Logger logger = LoggerFactory.getLogger(MongoDBConnection.class);
    private static MongoDBConnection instance;
    private MongoClient mongoClient;
    private MongoDatabase database;

    private static final String DEFAULT_CONNECTION_STRING = "mongodb://localhost:27017";
    private static final String DEFAULT_DATABASE_NAME = "survey_app";

    private MongoDBConnection(String connectionString, String databaseName) {
        try {
            // Configure POJO codec for automatic object mapping
            CodecRegistry pojoCodecRegistry = fromProviders(PojoCodecProvider.builder().automatic(true).build());
            CodecRegistry codecRegistry = fromRegistries(MongoClientSettings.getDefaultCodecRegistry(),
                    pojoCodecRegistry);

            // Build MongoDB client settings
            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(new ConnectionString(connectionString))
                    .codecRegistry(codecRegistry)
                    .build();

            mongoClient = MongoClients.create(settings);
            database = mongoClient.getDatabase(databaseName);

            logger.info("MongoDB connection established successfully to database: {}", databaseName);
        } catch (Exception e) {
            logger.error("Error connecting to MongoDB", e);
            throw new RuntimeException("Failed to connect to MongoDB", e);
        }
    }

    /**
     * Get singleton instance with default connection settings
     */
    public static synchronized MongoDBConnection getInstance() {
        String uri = System.getenv("MONGODB_URI") != null ? System.getenv("MONGODB_URI") : DEFAULT_CONNECTION_STRING;
        String db = System.getenv("MONGODB_DB") != null ? System.getenv("MONGODB_DB") : DEFAULT_DATABASE_NAME;
        return getInstance(uri, db);
    }

    /**
     * Get singleton instance with custom connection settings
     */
    public static synchronized MongoDBConnection getInstance(String connectionString, String databaseName) {
        if (instance == null) {
            instance = new MongoDBConnection(connectionString, databaseName);
        }
        return instance;
    }

    /**
     * Get the MongoDB database instance
     */
    public MongoDatabase getDatabase() {
        return database;
    }

    /**
     * Get the MongoDB client instance
     */
    public MongoClient getClient() {
        return mongoClient;
    }

    /**
     * Close the MongoDB connection
     */
    public void close() {
        if (mongoClient != null) {
            mongoClient.close();
            logger.info("MongoDB connection closed");
        }
    }
}
