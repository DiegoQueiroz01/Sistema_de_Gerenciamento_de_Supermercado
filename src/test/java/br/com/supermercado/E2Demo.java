package br.com.supermercado;

import br.com.supermercado.application.checkout.QueueService;
import br.com.supermercado.application.reporting.DashboardService;
import br.com.supermercado.presentation.cli.InputReader;
import br.com.supermercado.presentation.cli.QueueMenu;
import br.com.supermercado.presentation.cli.TerminalOutput;

import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * Demonstração isolada de caixas e filas, sem login.
 * Abre o menu de caixas com 5 caixas (3 abertos), 7 clientes e 2 atendimentos.
 */
public final class E2Demo {
    private E2Demo() {}

    static QueueService createSeededQueues() {
        QueueService queues = new QueueService();
        for (int i = 0; i < 5; i++) {
            queues.createCheckout();
        }
        queues.openCheckout("1");
        queues.openCheckout("2");
        queues.openCheckout("3");

        // Chegadas no passado para os tempos de espera não ficarem zerados.
        LocalDateTime start = LocalDateTime.now().minusMinutes(10);
        String[] names = {"Ana", "Bruno", "Carla", "Daniel", "Elisa", "Fábio", "Gabriela"};
        for (int i = 0; i < names.length; i++) {
            boolean priority = i == 2; // Carla é prioritária
            queues.registerArrival(names[i], priority, start.plusMinutes(i));
        }
        LocalDateTime now = LocalDateTime.now();
        queues.startNextAttendance("1", now);
        queues.startNextAttendance("2", now);
        return queues;
    }

    public static void main(String[] args) {
        Charset consoleCharset = Charset.forName(
                System.getProperty("stdout.encoding", Charset.defaultCharset().name()));
        QueueService queues = createSeededQueues();
        try (Scanner scanner = new Scanner(System.in, consoleCharset)) {
            TerminalOutput out = new TerminalOutput(System.out);
            new QueueMenu(new InputReader(scanner, out), out, queues, new DashboardService(queues)).run();
        } catch (NoSuchElementException e) {
            System.out.println("\nEntrada encerrada.");
        }
    }
}
