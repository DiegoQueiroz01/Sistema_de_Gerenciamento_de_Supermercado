package br.com.supermercado.domain.user;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

// Usuário do sistema. A senha não fica guardada como texto,
// só o hash SHA-256 dela.
public class User {
    public static final int MIN_PASSWORD_LENGTH = 6;

    private final String username;
    private String name;
    private UserRole role;
    private String passwordHash;
    private boolean active = true;

    public User(String username, String name, String password, UserRole role) {
        this.username = normalizeUsername(username);
        if (!this.username.matches("[a-z0-9._]{3,20}")) {
            throw new IllegalArgumentException(
                    "O login deve ter de 3 a 20 caracteres: letras minúsculas, números, '.' ou '_'.");
        }
        edit(name, role);
        setPassword(password);
    }

    // Login sem espaços nas pontas e em minúsculas
    public static String normalizeUsername(String username) {
        if (username == null) {
            return "";
        }
        return username.trim().toLowerCase();
    }

    public void edit(String name, UserRole role) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("O nome do usuário não pode ser vazio.");
        }
        if (role == null) {
            throw new IllegalArgumentException("O perfil do usuário é obrigatório.");
        }
        this.name = name.trim();
        this.role = role;
    }

    public void setPassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException(
                    "A senha deve ter pelo menos " + MIN_PASSWORD_LENGTH + " caracteres.");
        }
        this.passwordHash = hash(password);
    }

    public boolean checkPassword(String password) {
        if (password == null) {
            return false;
        }
        return passwordHash.equals(hash(password));
    }

    // Gera o hash SHA-256 da senha em texto hexadecimal
    private static String hash(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : bytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 não está disponível.", e);
        }
    }

    public void activate() {
        active = true;
    }

    public void deactivate() {
        active = false;
    }

    public String username() { return username; }
    public String name() { return name; }
    public UserRole role() { return role; }
    public boolean isActive() { return active; }
}
