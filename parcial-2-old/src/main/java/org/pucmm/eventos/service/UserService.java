package org.pucmm.eventos.service;

import org.pucmm.eventos.model.Role;
import org.pucmm.eventos.model.User;
import org.pucmm.eventos.repository.UserRepository;
import org.pucmm.eventos.util.PasswordUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private final UserRepository repo;

    public UserService(UserRepository repo) { this.repo = repo; }

    // ── Startup ──────────────────────────────────────────────────────────────

    public void createDefaultAdmin() {
        if (!repo.existsSystemAdmin()) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setEmail("admin@pucmm.edu.do");
            admin.setPassword(PasswordUtil.hash("Admin123!"));
            admin.setRole(Role.ADMIN);
            admin.setSystemAdmin(true);
            repo.save(admin);
            log.info("Default admin created  →  username: admin  |  password: Admin123!");
        }
    }

    // ── Auth ─────────────────────────────────────────────────────────────────

    public Optional<User> authenticate(String username, String password) {
        return repo.findByUsername(username)
                .filter(u -> !u.isBlocked())
                .filter(u -> PasswordUtil.verify(password, u.getPassword()));
    }

    public User register(String username, String email, String password) {
        if (repo.existsByUsername(username)) throw new IllegalArgumentException("Username already taken");
        if (repo.existsByEmail(email))       throw new IllegalArgumentException("Email already registered");
        if (password == null || password.length() < 6)
            throw new IllegalArgumentException("Password must be at least 6 characters");

        User u = new User();
        u.setUsername(username.trim());
        u.setEmail(email.trim().toLowerCase());
        u.setPassword(PasswordUtil.hash(password));
        u.setRole(Role.PARTICIPANT);
        return repo.save(u);
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    public Optional<User> findById(Long id)   { return repo.findById(id); }
    public List<User>     findAll()           { return repo.findAll(); }

    // ── Admin ops ─────────────────────────────────────────────────────────────

    public User blockUser(Long id) {
        User u = require(id);
        if (u.isSystemAdmin()) throw new IllegalArgumentException("Cannot block the system admin");
        u.setBlocked(true);
        return repo.save(u);
    }

    public User unblockUser(Long id) {
        User u = require(id);
        u.setBlocked(false);
        return repo.save(u);
    }

    public User updateRole(Long id, Role role) {
        User u = require(id);
        if (u.isSystemAdmin()) throw new IllegalArgumentException("Cannot change system admin role");
        if (role == Role.ADMIN) throw new IllegalArgumentException("Cannot elevate to ADMIN via this operation");
        u.setRole(role);
        return repo.save(u);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private User require(Long id) {
        return repo.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
}