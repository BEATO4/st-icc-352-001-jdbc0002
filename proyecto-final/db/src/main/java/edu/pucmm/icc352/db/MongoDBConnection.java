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

public class MongoDBConnection {
    private static final Logger logger = LoggerFactory.getLogger(MongoDBConnection.class);
    private static MongoDBConnection instance;
    private MongoClient mongoClient;
    private MongoDatabase database;

    private static final String DEFAULT_CONNECTION_STRING = "mongodb://localhost:27017";
    private static final String DEFAULT_DATABASE_NAME = "survey_app";

    private MongoDBConnection(String connectionString, String databaseName) {
        try {
            CodecRegistry pojoCodecRegistry = fromProviders(PojoCodecProvider.builder().automatic(true).build());
            CodecRegistry codecRegistry = fromRegistries(MongoClientSettings.getDefaultCodecRegistry(),
                    pojoCodecRegistry);

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

    public static synchronized MongoDBConnection getInstance() {
        String uri = System.getenv("MONGODB_URI") != null ? System.getenv("MONGODB_URI") : DEFAULT_CONNECTION_STRING;
        String db = System.getenv("MONGODB_DB") != null ? System.getenv("MONGODB_DB") : DEFAULT_DATABASE_NAME;
        return getInstance(uri, db);
    }

    public static synchronized MongoDBConnection getInstance(String connectionString, String databaseName) {
        if (instance == null) {
            instance = new MongoDBConnection(connectionString, databaseName);
        }
        return instance;
    }

    public MongoDatabase getDatabase() {
        return database;
    }

    public MongoClient getClient() {
        return mongoClient;
    }

    public void close() {
        if (mongoClient != null) {
            mongoClient.close();
            logger.info("MongoDB connection closed");
        }
    }
}
