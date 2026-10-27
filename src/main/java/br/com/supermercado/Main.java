package br.com.supermercado;

import br.com.supermercado.infrastructure.mysql.ConexaoMySQL;
import br.com.supermercado.presentation.cli.CliApplication;
import br.com.supermercado.presentation.cli.InputReader;
import br.com.supermercado.presentation.cli.TerminalOutput;

import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * Ponto de entrada.
 * - Sem argumentos: tudo em memória, com dados de exemplo (as estruturas de dados do projeto).
 * - Com "--mysql": produtos, categorias e movimentações de estoque são gravados no MySQL.
 */
public final class Main {
    private Main() {}

    public static void main(String[] args) {
        boolean mysql = Arrays.asList(args).contains("--mysql");
        AppServices app;
        String storage;
        if (mysql) {
            try (Connection ignored = ConexaoMySQL.abrir()) {
                // só confere se o banco está acessível antes de abrir os menus
            } catch (SQLException e) {
                System.err.println("Não foi possível conectar ao MySQL: " + e.getMessage());
                System.err.println("Confira se o serviço MySQL está ativo, se o banco foi criado e se as "
                        + "variáveis MERCADO_DB_USUARIO e MERCADO_DB_SENHA estão corretas.");
                System.exit(1);
                return;
            }
            app = AppServices.withMySQL();
            storage = "MySQL (produtos e estoque) + memória (demais dados)";
        } else {
            app = AppServices.inMemory();
            DemoData.loadProducts(app);
            storage = "memória, com dados de exemplo (use --mysql para o banco)";
        }
        DemoData.loadUsers(app);
        DemoData.loadCheckouts(app);

        Charset consoleCharset = Charset.forName(
                System.getProperty("stdout.encoding", Charset.defaultCharset().name()));
        TerminalOutput out = new TerminalOutput(System.out);
        try (Scanner scanner = new Scanner(System.in, consoleCharset)) {
            new CliApplication(app, new InputReader(scanner, out), out, storage).run();
        } catch (NoSuchElementException e) {
            out.println("\nEntrada encerrada. Programa finalizado.");
        }
    }
}
