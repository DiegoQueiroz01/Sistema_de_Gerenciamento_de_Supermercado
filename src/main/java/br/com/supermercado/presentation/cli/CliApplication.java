package br.com.supermercado.presentation.cli;

import br.com.supermercado.AppServices;
import br.com.supermercado.domain.user.User;

import java.util.Objects;

/** Ciclo da aplicação no terminal: tela inicial, login e menu do perfil até o usuário sair. */
public final class CliApplication {
    private static final int MAX_LOGIN_ATTEMPTS = 3;

    private final AppServices app;
    private final InputReader in;
    private final TerminalOutput out;
    private final MenuRouter router;
    private final String storageDescription;

    public CliApplication(AppServices app, InputReader in, TerminalOutput out, String storageDescription) {
        this.app = Objects.requireNonNull(app);
        this.in = Objects.requireNonNull(in);
        this.out = Objects.requireNonNull(out);
        this.router = new MenuRouter(app, in, out);
        this.storageDescription = storageDescription;
    }

    public void run() {
        out.println("==========================================");
        out.println("  " + app.store().current().name().toUpperCase() + " - Gerenciamento de Supermercado");
        out.println("  Armazenamento: " + storageDescription);
        out.println("==========================================");
        int option;
        do {
            option = in.readMenuWithZeroLabel("TELA INICIAL", "Encerrar o programa", "Entrar (login)");
            if (option == 1) {
                User user = login();
                if (user != null) {
                    out.success("Bem-vindo(a), " + user.name() + " (" + user.role().label() + ").");
                    router.open(user);
                    out.println("  Sessão de " + user.name() + " encerrada.");
                }
            }
        } while (option != 0);
        out.println("Programa encerrado. Até logo!");
    }

    /** Até 3 tentativas; devolve null se todas falharem. */
    private User login() {
        out.title("Login");
        for (int attempt = 1; attempt <= MAX_LOGIN_ATTEMPTS; attempt++) {
            String username = in.readRequired("Login: ");
            String password = in.readRequired("Senha: ");
            try {
                return app.auth().login(username, password);
            } catch (IllegalArgumentException | IllegalStateException e) {
                out.error(e.getMessage() + " (tentativa " + attempt + " de " + MAX_LOGIN_ATTEMPTS + ")");
            }
        }
        out.warning("Número máximo de tentativas atingido.");
        return null;
    }
}
