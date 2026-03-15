package edu.pucmm.icc352.repositories;

import edu.pucmm.icc352.config.HibernateConfig;
import org.hibernate.Session;
import org.hibernate.Transaction;
import java.util.List;
import java.util.Optional;

public abstract class BaseRepository<T> {

    private final Class<T> type;

    protected BaseRepository(Class<T> type) {
        this.type = type;
    }

    protected Session openSession() {
        return HibernateConfig.getSessionFactory().openSession();
    }

    public T save(T entity) {
        try (Session s = openSession()) {
            Transaction tx = s.beginTransaction();
            s.persist(entity);
            tx.commit();
            return entity;
        }
    }

    public T update(T entity) {
        try (Session s = openSession()) {
            Transaction tx = s.beginTransaction();
            T merged = s.merge(entity);
            tx.commit();
            return merged;
        }
    }

    public void delete(T entity) {
        try (Session s = openSession()) {
            Transaction tx = s.beginTransaction();
            s.remove(s.contains(entity) ? entity : s.merge(entity));
            tx.commit();
        }
    }

    public Optional<T> findById(Long id) {
        try (Session s = openSession()) {
            return Optional.ofNullable(s.get(type, id));
        }
    }

    public List<T> findAll() {
        try (Session s = openSession()) {
            return s.createQuery("FROM " + type.getSimpleName(), type).list();
        }
    }
}
