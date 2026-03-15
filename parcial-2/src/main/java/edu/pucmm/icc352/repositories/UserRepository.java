package edu.pucmm.icc352.repositories;

import edu.pucmm.icc352.models.Role;
import edu.pucmm.icc352.models.User;
import java.util.List;
import java.util.Optional;

public class UserRepository extends BaseRepository<User> {

    public UserRepository() { super(User.class); }

    public Optional<User> findByEmail(String email) {
        try (var s = openSession()) {
            return s.createQuery("FROM User WHERE email = :e", User.class)
                    .setParameter("e", email)
                    .uniqueResultOptional();
        }
    }

    public List<User> findByRole(Role role) {
        try (var s = openSession()) {
            return s.createQuery("FROM User WHERE role = :r", User.class)
                    .setParameter("r", role)
                    .list();
        }
    }

    public boolean existsByEmail(String email) {
        try (var s = openSession()) {
            Long count = s.createQuery(
                            "SELECT COUNT(u) FROM User u WHERE u.email = :e", Long.class)
                    .setParameter("e", email).uniqueResult();
            return count != null && count > 0;
        }
    }
}