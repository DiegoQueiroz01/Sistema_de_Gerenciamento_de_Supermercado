package br.com.supermercado.application.auth;

import br.com.supermercado.domain.user.User;
import br.com.supermercado.domain.user.UserRole;
import br.com.supermercado.repositories.UserRepository;

import java.util.List;

// Login e cadastro de usuários. Regras:
// - o login não pode se repetir (sem diferenciar maiúsculas);
// - usuário desativado não consegue entrar;
// - sempre tem que existir pelo menos um administrador ativo;
// - ninguém pode desativar a própria conta.
public class AuthService {
    public static final String DEFAULT_ADMIN_USERNAME = "admin";
    public static final String DEFAULT_ADMIN_PASSWORD = "admin123";

    private final UserRepository users;

    public AuthService(UserRepository users) {
        this.users = users;
    }

    // Cria o admin padrão se ainda não existir nenhum usuário. Devolve true se criou.
    public boolean ensureDefaultAdmin() {
        if (!users.findAll().isEmpty()) {
            return false;
        }
        users.save(new User(DEFAULT_ADMIN_USERNAME, "Administrador", DEFAULT_ADMIN_PASSWORD, UserRole.ADMIN));
        return true;
    }

    // A mensagem de erro é a mesma para login ou senha errados, para não mostrar quais logins existem
    public User login(String username, String password) {
        User user = users.findByUsername(username);
        if (user == null || !user.checkPassword(password)) {
            throw new IllegalArgumentException("Login ou senha inválidos.");
        }
        if (!user.isActive()) {
            throw new IllegalStateException("Usuário desativado. Procure um administrador.");
        }
        return user;
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
        User user = getUser(username);
        boolean losingAdmin = user.role() == UserRole.ADMIN && role != UserRole.ADMIN;
        if (losingAdmin && user.isActive() && countActiveAdmins() == 1) {
            throw new IllegalStateException("Este é o único administrador ativo: o perfil não pode mudar.");
        }
        user.edit(name, role);
        users.save(user);
    }

    // Troca feita pelo próprio usuário: precisa da senha atual
    public void changePassword(String username, String currentPassword, String newPassword) {
        User user = getUser(username);
        if (!user.checkPassword(currentPassword)) {
            throw new IllegalArgumentException("A senha atual não confere.");
        }
        user.setPassword(newPassword);
        users.save(user);
    }

    // Troca feita pelo administrador: não precisa da senha atual
    public void resetPassword(String username, String newPassword) {
        User user = getUser(username);
        user.setPassword(newPassword);
        users.save(user);
    }

    public void deactivate(String loggedUsername, String username) {
        User user = getUser(username);
        if (user.username().equals(User.normalizeUsername(loggedUsername))) {
            throw new IllegalStateException("Você não pode desativar a sua própria conta.");
        }
        if (!user.isActive()) {
            throw new IllegalStateException("O usuário já está desativado: " + user.username());
        }
        if (user.role() == UserRole.ADMIN && countActiveAdmins() == 1) {
            throw new IllegalStateException("Não é possível desativar o único administrador ativo.");
        }
        user.deactivate();
        users.save(user);
    }

    public void activate(String username) {
        User user = getUser(username);
        if (user.isActive()) {
            throw new IllegalStateException("O usuário já está ativo: " + user.username());
        }
        user.activate();
        users.save(user);
    }

    // Devolve null se não encontrar
    public User findByUsername(String username) {
        return users.findByUsername(username);
    }

    // Usuários em ordem alfabética de login
    public List<User> listUsers() {
        List<User> list = users.findAll();
        list.sort((a, b) -> a.username().compareTo(b.username()));
        return list;
    }

    private int countActiveAdmins() {
        int count = 0;
        for (User u : users.findAll()) {
            if (u.isActive() && u.role() == UserRole.ADMIN) {
                count++;
            }
        }
        return count;
    }

    private User getUser(String username) {
        User user = users.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("Usuário não encontrado: " + username);
        }
        return user;
    }
}
