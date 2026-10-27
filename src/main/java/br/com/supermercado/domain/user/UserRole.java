package br.com.supermercado.domain.user;

// Perfis de acesso. Cada um abre um menu diferente.
public enum UserRole {
    ADMIN("Administrador"),
    STOCKIST("Estoquista"),
    CASHIER("Operador de caixa");

    private final String label;

    UserRole(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
