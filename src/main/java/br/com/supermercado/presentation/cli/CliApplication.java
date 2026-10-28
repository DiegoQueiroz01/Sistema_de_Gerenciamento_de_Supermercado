package br.com.supermercado.presentation.cli;

import br.com.supermercado.AppServices;
import br.com.supermercado.domain.user.User;

// Tela inicial do sistema: faz o login e abre o menu do perfil do usuário
public class CliApplication {
    private static final int MAX_LOGIN_ATTEMPTS = 3;

    private final AppServices app;
    private final InputReader in;
    private final TerminalOutput out;
    private final MenuRouter router;

    public CliApplication(AppServices app, InputReader in, TerminalOutput out) {
        this.app = app;
        this.in = in;
        this.out = out;
        this.router = new MenuRouter(app, in, out);
    }

    public void run() {
        out.println("==========================================");
        out.println("  " + app.store().current().name().toUpperCase() + " - Gerenciamento de Supermercado");
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

    // Até 3 tentativas. Devolve null se errar todas.
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
