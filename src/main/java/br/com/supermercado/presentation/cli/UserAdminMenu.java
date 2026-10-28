package br.com.supermercado.presentation.cli;

import br.com.supermercado.application.auth.AuthService;
import br.com.supermercado.domain.user.User;
import br.com.supermercado.domain.user.UserRole;

import java.util.List;

// Cadastro de usuários (só o administrador) e troca da própria senha (todos os perfis)
public class UserAdminMenu {
    private final InputReader in;
    private final TerminalOutput out;
    private final AuthService auth;
    private final User currentUser;

    public UserAdminMenu(InputReader in, TerminalOutput out, AuthService auth, User currentUser) {
        this.in = in;
        this.out = out;
        this.auth = auth;
        this.currentUser = currentUser;
    }

    public void run() {
        int option;
        do {
            option = in.readMenuOption("USUÁRIOS", "Listar usuários", "Cadastrar usuário",
                    "Editar nome ou perfil", "Redefinir senha", "Desativar usuário", "Reativar usuário");
            try {
                switch (option) {
                    case 1:
                        list();
                        break;
                    case 2:
                        register();
                        break;
                    case 3:
                        edit();
                        break;
                    case 4:
                        resetPassword();
                        break;
                    case 5:
                        auth.deactivate(currentUser.username(), in.readRequired("Login do usuário: "));
                        out.success("Usuário desativado.");
                        break;
                    case 6:
                        auth.activate(in.readRequired("Login do usuário: "));
                        out.success("Usuário reativado.");
                        break;
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                out.error(e.getMessage());
            }
        } while (option != 0);
    }

    // Usado no menu de todos os perfis
    public static void changeOwnPassword(InputReader in, TerminalOutput out, AuthService auth, User user) {
        out.title("Alterar Minha Senha");
        try {
            String current = in.readRequired("Senha atual: ");
            String password = readNewPassword(in, out);
            if (password == null) {
                return;
            }
            auth.changePassword(user.username(), current, password);
            out.success("Senha alterada.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            out.error(e.getMessage());
        }
    }

    private void list() {
        out.title("Usuários");
        List<User> users = auth.listUsers();
        String format = "  %-20s %-30s %-18s %s%n";
        StringBuilder sb = new StringBuilder(String.format(format, "LOGIN", "NOME", "PERFIL", "SITUAÇÃO"));
        sb.append("  ").append("-".repeat(80)).append('\n');
        for (User u : users) {
            sb.append(String.format(format, u.username(), TerminalOutput.cut(u.name(), 30), u.role().label(),
                    u.isActive() ? "ativo" : "DESATIVADO"));
        }
        sb.append("  Total: ").append(users.size()).append(" usuário(s)");
        out.println(sb.toString());
    }

    private void register() {
        out.title("Novo Usuário");
        String username = in.readRequired("Login (3 a 20 caracteres: a-z, 0-9, '.', '_'): ");
        String name = in.readRequired("Nome completo: ");
        UserRole role = chooseRole();
        if (role == null) {
            return;
        }
        String password = readNewPassword(in, out);
        if (password == null) {
            return;
        }
        User user = auth.register(username, name, password, role);
        out.success("Usuário '" + user.username() + "' cadastrado como " + role.label() + ".");
    }

    private void edit() {
        out.title("Editar Usuário");
        User user = auth.findByUsername(in.readRequired("Login do usuário: "));
        if (user == null) {
            out.error("Usuário não encontrado.");
            return;
        }
        String name = in.readLine("Nome [" + user.name() + "]: ");
        UserRole role = user.role();
        if (in.confirm("Alterar o perfil (atual: " + role.label() + ")?")) {
            UserRole chosen = chooseRole();
            if (chosen != null) {
                role = chosen;
            }
        }
        if (name.isEmpty()) {
            name = user.name();
        }
        auth.edit(user.username(), name, role);
        out.success("Usuário atualizado.");
    }

    private void resetPassword() {
        out.title("Redefinir Senha");
        String username = in.readRequired("Login do usuário: ");
        if (auth.findByUsername(username) == null) {
            out.error("Usuário não encontrado.");
            return;
        }
        String password = readNewPassword(in, out);
        if (password == null) {
            return;
        }
        auth.resetPassword(username, password);
        out.success("Senha redefinida.");
    }

    private UserRole chooseRole() {
        UserRole[] roles = UserRole.values();
        for (int i = 0; i < roles.length; i++) {
            out.println("  " + (i + 1) + ") " + roles[i].label());
        }
        out.println("  0) Cancelar");
        int n = in.readInt("Perfil: ", 0, roles.length);
        if (n == 0) {
            return null;
        }
        return roles[n - 1];
    }

    // Pede a senha nova duas vezes. Devolve null se não conferir.
    private static String readNewPassword(InputReader in, TerminalOutput out) {
        String password = in.readRequired("Nova senha (mínimo " + User.MIN_PASSWORD_LENGTH + " caracteres): ");
        if (password.length() < User.MIN_PASSWORD_LENGTH) {
            out.error("A senha deve ter pelo menos " + User.MIN_PASSWORD_LENGTH + " caracteres.");
            return null;
        }
        if (!password.equals(in.readRequired("Repita a nova senha: "))) {
            out.error("As senhas não conferem.");
            return null;
        }
        return password;
    }
}
