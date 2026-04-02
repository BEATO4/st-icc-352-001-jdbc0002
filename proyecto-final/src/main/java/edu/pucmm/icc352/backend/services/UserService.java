package edu.pucmm.icc352.backend.services;

import edu.pucmm.icc352.models.User;
import edu.pucmm.icc352.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * Service for user management operations
 */
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserService() {
        this(new UserRepository());
    }

    /**
     * Get user by ID
     */
    public Optional<User> getUserById(String id) {
        return userRepository.findById(id);
    }

    /**
     * Get user by username
     */
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * Get all users
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Update user
     */
    public boolean updateUser(User user) {
        try {
            return userRepository.update(user);
        } catch (Exception e) {
            logger.error("Error updating user", e);
            return false;
        }
    }

    /**
     * Delete user
     */
    public boolean deleteUser(String id) {
        try {
            return userRepository.delete(id);
        } catch (Exception e) {
            logger.error("Error deleting user", e);
            return false;
        }
    }

    /**
     * Check if username exists
     */
    public boolean usernameExists(String username) {
        return userRepository.existsByUsername(username);
    }

    /**
     * Get total user count
     */
    public long getUserCount() {
        return userRepository.count();
    }
}
