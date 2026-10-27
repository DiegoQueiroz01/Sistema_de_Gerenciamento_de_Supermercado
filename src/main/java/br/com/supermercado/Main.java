package br.com.supermercado;

import br.com.supermercado.presentation.cli.CliApplication;
import br.com.supermercado.presentation.cli.InputReader;
import br.com.supermercado.presentation.cli.TerminalOutput;

import java.nio.charset.Charset;
import java.util.NoSuchElementException;
import java.util.Scanner;

// Início do programa. Os dados ficam em memória e o sistema já abre com dados de exemplo.
public class Main {
    private Main() {}

    public static void main(String[] args) {
        AppServices app = AppServices.inMemory();
        DemoData.loadProducts(app);
        DemoData.loadStore(app);
        DemoData.loadUsers(app);
        DemoData.loadCheckouts(app);

        Charset consoleCharset = Charset.forName(
                System.getProperty("stdout.encoding", Charset.defaultCharset().name()));
        TerminalOutput out = new TerminalOutput(System.out);
        try (Scanner scanner = new Scanner(System.in, consoleCharset)) {
            new CliApplication(app, new InputReader(scanner, out), out).run();
        } catch (NoSuchElementException e) {
            out.println("\nEntrada encerrada. Programa finalizado.");
        }
    }
}
