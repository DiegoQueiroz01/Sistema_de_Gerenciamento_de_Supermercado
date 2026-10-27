package br.com.supermercado.application.auth;

import br.com.supermercado.domain.user.User;
import br.com.supermercado.domain.user.UserRole;
import br.com.supermercado.repositories.UserRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Autenticação e gestão de usuários. Regras:
 * - login único (sem diferenciar maiúsculas);
 * - usuário inativo não entra no sistema;
 * - sempre deve existir ao menos um administrador ativo;
 * - ninguém desativa a própria conta.
 */
public final class AuthService {
    public static final String DEFAULT_ADMIN_USERNAME = "admin";
    public static final String DEFAULT_ADMIN_PASSWORD = "admin123";

    private final UserRepository users;

    public AuthService(UserRepository users) {
        this.users = Objects.requireNonNull(users);
    }

    /** Cria o administrador padrão se ainda não houver nenhum usuário. Devolve true se criou. */
    public boolean ensureDefaultAdmin() {
        if (!users.findAll().isEmpty()) {
            return false;
        }
        users.save(new User(DEFAULT_ADMIN_USERNAME, "Administrador", DEFAULT_ADMIN_PASSWORD, UserRole.ADMIN));
        return true;
    }

    /** Devolve o usuário autenticado. A mensagem de erro não revela se o login existe. */
    public User login(String username, String password) {
        Optional<User> found = users.findByUsername(username);
        if (found.isEmpty() || !found.get().checkPassword(password)) {
            throw new IllegalArgumentException("Login ou senha inválidos.");
        }
        if (!found.get().isActive()) {
            throw new IllegalStateException("Usuário desativado. Procure um administrador.");
        }
        return found.get();
    }

    public User register(String username, String name, String password, UserRole role) {
        User user = new User(username, name, password, role);
        if (users.existsByUsername(user.username())) {
            throw new IllegalStateException("Login já cadastrado: " + user.username());
        }
        users.save(user);
        return user;
    }

    public void edit(String username, String name, UserRole role) {
        User user = requireUser(username);
        if (user.role() == UserRole.ADMIN && role != UserRole.ADMIN && user.isActive()
                && activeAdmins() == 1) {
            throw new IllegalStateException("Este é o único administrador ativo: o perfil não pode mudar.");
        }
        user.edit(name, role);
        users.save(user);
    }

    /** Troca da própria senha: exige a senha atual. */
    public void changePassword(String username, String currentPassword, String newPassword) {
        User user = requireUser(username);
        if (!user.checkPassword(currentPassword)) {
            throw new IllegalArgumentException("A senha atual não confere.");
        }
        user.setPassword(newPassword);
        users.save(user);
    }

    /** Redefinição feita pelo administrador (não exige a senha atual). */
    public void resetPassword(String username, String newPassword) {
        User user = requireUser(username);
        user.setPassword(newPassword);
        users.save(user);
    }

    public void deactivate(String actingUsername, String username) {
        User user = requireUser(username);
        if (user.username().equals(User.normalizeUsername(actingUsername))) {
            throw new IllegalStateException("Você não pode desativar a sua própria conta.");
        }
        if (!user.isActive()) {
            throw new IllegalStateException("O usuário já está desativado: " + user.username());
        }
        if (user.role() == UserRole.ADMIN && activeAdmins() == 1) {
            throw new IllegalStateException("Não é possível desativar o único administrador ativo.");
        }
        user.deactivate();
        users.save(user);
    }

    public void activate(String username) {
        User user = requireUser(username);
        if (user.isActive()) {
            throw new IllegalStateException("O usuário já está ativo: " + user.username());
        }
        user.activate();
        users.save(user);
    }

    public Optional<User> findByUsername(String username) {
        return users.findByUsername(username);
    }

    /** Usuários em ordem de login. */
    public List<User> listUsers() {
        List<User> list = new ArrayList<>(users.findAll());
        list.sort(Comparator.comparing(User::username));
        return list;
    }

    private long activeAdmins() {
        return users.findAll().stream().filter(u -> u.isActive() && u.role() == UserRole.ADMIN).count();
    }

    private User requireUser(String username) {
        return users.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado: " + username));
    }
}
