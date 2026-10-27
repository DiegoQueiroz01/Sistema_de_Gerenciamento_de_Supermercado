package br.com.supermercado.domain.user;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Usuário do sistema. A senha nunca é guardada em texto: guarda-se o hash SHA-256
 * da senha combinada com um "sal" aleatório próprio do usuário.
 */
public final class User {
    public static final int MIN_PASSWORD_LENGTH = 6;
    private static final Pattern USERNAME = Pattern.compile("[a-z0-9._]{3,20}");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final String username;
    private String name;
    private UserRole role;
    private String salt;
    private String passwordHash;
    private boolean active = true;

    public User(String username, String name, String password, UserRole role) {
        this.username = normalizeUsername(username);
        if (!USERNAME.matcher(this.username).matches()) {
            throw new IllegalArgumentException(
                    "O login deve ter de 3 a 20 caracteres: letras minúsculas, números, '.' ou '_'.");
        }
        edit(name, role);
        setPassword(password);
    }

    public static String normalizeUsername(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }

    public void edit(String name, UserRole role) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("O nome do usuário não pode ser vazio.");
        }
        this.name = name.trim();
        this.role = Objects.requireNonNull(role, "O perfil do usuário é obrigatório.");
    }

    public void setPassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException(
                    "A senha deve ter pelo menos " + MIN_PASSWORD_LENGTH + " caracteres.");
        }
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        this.salt = HexFormat.of().formatHex(bytes);
        this.passwordHash = hash(salt, password);
    }

    public boolean checkPassword(String password) {
        return password != null && MessageDigest.isEqual(
                passwordHash.getBytes(StandardCharsets.UTF_8),
                hash(salt, password).getBytes(StandardCharsets.UTF_8));
    }

    private static String hash(String salt, String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] result = digest.digest((salt + password).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(result);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível nesta JVM.", e);
        }
    }

    public void activate() { active = true; }

    public void deactivate() { active = false; }

    public String username() { return username; }
    public String name() { return name; }
    public UserRole role() { return role; }
    public boolean isActive() { return active; }
}
