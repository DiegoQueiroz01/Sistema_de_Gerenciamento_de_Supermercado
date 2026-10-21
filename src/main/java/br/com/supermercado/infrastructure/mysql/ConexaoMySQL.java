package br.com.supermercado.infrastructure.mysql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Centraliza a conexão com o MySQL. Configure MERCADO_DB_URL, MERCADO_DB_USUARIO e MERCADO_DB_SENHA. */
public final class ConexaoMySQL {
    private ConexaoMySQL() {}
    public static Connection abrir() throws SQLException {
        String url = System.getenv().getOrDefault("MERCADO_DB_URL", "jdbc:mysql://localhost:3306/mercado_facil?useUnicode=true&characterEncoding=UTF-8&serverTimezone=America/Sao_Paulo");
        String usuario = System.getenv().getOrDefault("MERCADO_DB_USUARIO", "root");
        String senha = System.getenv().getOrDefault("MERCADO_DB_SENHA", "");
        return DriverManager.getConnection(url, usuario, senha);
    }
}
