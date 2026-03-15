package org.pucmm.eventos.repository;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.pucmm.eventos.model.User;

import java.util.List;
import java.util.Optional;

public class UserRepository {

    private final SessionFactory sf;

    public UserRepository(SessionFactory sf) { this.sf = sf; }

    public User save(User user) {
        try (Session s = sf.openSession()) {
            Transaction tx = s.beginTransaction();
            if (user.getId() == null) {
                s.persist(user);
            } else {
                user = s.merge(user);
            }
            tx.commit();
            return user;
        }
    }

    public Optional<User> findById(Long id) {
        try (Session s = sf.openSession()) {
            return Optional.ofNullable(s.get(User.class, id));
        }
    }

    public Optional<User> findByUsername(String username) {
        try (Session s = sf.openSession()) {
            return s.createQuery("FROM User WHERE username = :u", User.class)
                    .setParameter("u", username).uniqueResultOptional();
        }
    }

    public List<User> findAll() {
        try (Session s = sf.openSession()) {
            return s.createQuery("FROM User ORDER BY createdAt DESC", User.class).list();
        }
    }

    public boolean existsByUsername(String username) {
        try (Session s = sf.openSession()) {
            Long c = s.createQuery("SELECT COUNT(u) FROM User u WHERE u.username = :u", Long.class)
                    .setParameter("u", username).uniqueResult();
            return c != null && c > 0;
        }
    }

    public boolean existsByEmail(String email) {
        try (Session s = sf.openSession()) {
            Long c = s.createQuery("SELECT COUNT(u) FROM User u WHERE u.email = :e", Long.class)
                    .setParameter("e", email).uniqueResult();
            return c != null && c > 0;
        }
    }

    public boolean existsSystemAdmin() {
        try (Session s = sf.openSession()) {
            Long c = s.createQuery("SELECT COUNT(u) FROM User u WHERE u.systemAdmin = true", Long.class)
                    .uniqueResult();
            return c != null && c > 0;
        }
    }
}