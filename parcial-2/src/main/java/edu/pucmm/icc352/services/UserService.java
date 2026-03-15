package edu.pucmm.icc352.services;

import edu.pucmm.icc352.models.Role;
import edu.pucmm.icc352.models.User;
import edu.pucmm.icc352.repositories.UserRepository;
import org.mindrot.jbcrypt.BCrypt;
import java.util.List;
import java.util.Optional;

public class UserService {

    private final UserRepository repo = new UserRepository();

    public User register(String email, String password, String name) {
        if (repo.existsByEmail(email))
            throw new IllegalArgumentException("El email ya esta registrado.");
        if (password.length() < 6)
            throw new IllegalArgumentException("La contrasena debe tener al menos 6 caracteres.");
        User user = new User(email, BCrypt.hashpw(password, BCrypt.gensalt()), name, Role.PARTICIPANT);
        return repo.save(user);
    }

    public User login(String email, String password) {
        Optional<User> opt = repo.findByEmail(email);
        if (opt.isEmpty())
            throw new IllegalArgumentException("Credenciales incorrectas.");
        User user = opt.get();
        if (user.isBlocked())
            throw new IllegalStateException("Tu cuenta ha sido bloqueada.");
        if (!BCrypt.checkpw(password, user.getPassword()))
            throw new IllegalArgumentException("Credenciales incorrectas.");
        return user;
    }

    public User setRole(Long targetUserId, Role newRole) {
        User user = repo.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));
        if (!user.isDeletable() && newRole != Role.ADMIN)
            throw new IllegalStateException("No se puede cambiar el rol del admin principal.");
        user.setRole(newRole);
        return repo.update(user);
    }

    public User setBlocked(Long userId, boolean blocked) {
        User user = repo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));
        if (!user.isDeletable())
            throw new IllegalStateException("No se puede bloquear al admin principal.");
        user.setBlocked(blocked);
        return repo.update(user);
    }

    public void delete(Long userId) {
        User user = repo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));
        if (!user.isDeletable())
            throw new IllegalStateException("Este usuario no puede ser eliminado.");
        repo.delete(user);
    }

    public Optional<User> findById(Long id)     { return repo.findById(id); }
    public Optional<User> findByEmail(String e)  { return repo.findByEmail(e); }
    public List<User> findAll()                  { return repo.findAll(); }
}