package br.com.supermercado;

import br.com.supermercado.application.auth.AuthService;
import br.com.supermercado.domain.user.User;
import br.com.supermercado.domain.user.UserRole;
import br.com.supermercado.infrastructure.memory.InMemoryUserRepository;

/**
 * Testes de usuários e login sem dependências externas.
 * Execução: mvn test-compile exec:java -Dexec.mainClass=br.com.supermercado.UserTests -Dexec.classpathScope=test
 */
public class UserTests {
    static int passed = 0, failed = 0;

    interface Action { void run() throws Exception; }

    static void check(String name, boolean condition) {
        if (condition) { passed++; System.out.println("PASSOU  " + name); }
        else { failed++; System.out.println("FALHOU  " + name); }
    }

    static boolean throwsType(Class<? extends Exception> type, Action action) {
        try { action.run(); return false; } catch (Exception e) { return type.isInstance(e); }
    }

    static int run() {
        passed = 0;
        failed = 0;
        InMemoryUserRepository repo = new InMemoryUserRepository();
        AuthService auth = new AuthService(repo);

        check("CU01 cria administrador padrão quando não há usuários", auth.ensureDefaultAdmin());
        check("CU02 não recria o administrador padrão", !auth.ensureDefaultAdmin());
        User admin = auth.login("ADMIN", AuthService.DEFAULT_ADMIN_PASSWORD);
        check("CU03 login ignora maiúsculas no usuário", admin.role() == UserRole.ADMIN);
        check("CU04 senha errada recusada", throwsType(IllegalArgumentException.class,
                () -> auth.login("admin", "errada")));
        check("CU05 login inexistente recusado", throwsType(IllegalArgumentException.class,
                () -> auth.login("ninguem", "123456")));

        User carlos = auth.register("carlos", "Carlos Oliveira", "estoque1", UserRole.STOCKIST);
        check("CU06 cadastra usuário", carlos.isActive() && auth.listUsers().size() == 2);
        check("CU07 senha não fica em texto puro", carlos.checkPassword("estoque1")
                && !carlos.checkPassword("Estoque1"));
        check("CU08 login duplicado recusado", throwsType(IllegalStateException.class,
                () -> auth.register("Carlos", "Outro", "123456", UserRole.CASHIER)));
        check("CU09 senha curta recusada", throwsType(IllegalArgumentException.class,
                () -> auth.register("marta", "Marta", "123", UserRole.CASHIER)));
        check("CU10 login inválido recusado", throwsType(IllegalArgumentException.class,
                () -> auth.register("a b", "X", "123456", UserRole.CASHIER)));
        check("CU11 nome vazio recusado", throwsType(IllegalArgumentException.class,
                () -> auth.register("marta", " ", "123456", UserRole.CASHIER)));

        auth.deactivate("admin", "carlos");
        check("CU12 usuário desativado não entra", throwsType(IllegalStateException.class,
                () -> auth.login("carlos", "estoque1")));
        auth.activate("carlos");
        check("CU13 usuário reativado entra", auth.login("carlos", "estoque1") == carlos);
        check("CU14 não desativa a própria conta", throwsType(IllegalStateException.class,
                () -> auth.deactivate("admin", "admin")));
        check("CU15 não desativa o único administrador", throwsType(IllegalStateException.class,
                () -> auth.deactivate("carlos", "admin")));
        check("CU16 único administrador não perde o perfil", throwsType(IllegalStateException.class,
                () -> auth.edit("admin", "Admin", UserRole.CASHIER)));

        auth.register("juliana", "Juliana Silva", "admin456", UserRole.ADMIN);
        auth.edit("admin", "Admin Antigo", UserRole.CASHIER);
        check("CU17 com outro administrador o perfil pode mudar",
                auth.findByUsername("admin").orElseThrow().role() == UserRole.CASHIER);

        check("CU18 troca de senha exige a senha atual", throwsType(IllegalArgumentException.class,
                () -> auth.changePassword("carlos", "errada", "novaSenha")));
        auth.changePassword("carlos", "estoque1", "novaSenha");
        check("CU19 troca de senha", auth.login("carlos", "novaSenha") == carlos
                && throwsType(IllegalArgumentException.class, () -> auth.login("carlos", "estoque1")));
        auth.resetPassword("carlos", "redefinida");
        check("CU20 redefinição pelo administrador", auth.login("carlos", "redefinida") == carlos);
        check("CU21 lista em ordem de login",
                auth.listUsers().get(0).username().equals("admin")
                        && auth.listUsers().get(2).username().equals("juliana"));

        System.out.println("\nResultado: " + passed + " passaram, " + failed + " falharam.");
        return failed;
    }

    public static void main(String[] args) {
        System.exit(run() == 0 ? 0 : 1);
    }
}
