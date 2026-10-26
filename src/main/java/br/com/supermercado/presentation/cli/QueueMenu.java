package br.com.supermercado.presentation.cli;

import br.com.supermercado.application.checkout.QueueService;
import br.com.supermercado.application.checkout.QueueService.CustomerAssignment;
import br.com.supermercado.application.reporting.DashboardService;
import br.com.supermercado.application.reporting.DashboardService.CheckoutSummary;
import br.com.supermercado.application.reporting.DashboardService.QueueOverview;
import br.com.supermercado.domain.checkout.Checkout;
import br.com.supermercado.domain.checkout.Customer;
import br.com.supermercado.domain.checkout.ServiceRecord;

import java.io.PrintStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

/**
 * Menu de caixas e filas: visão geral, abrir/fechar caixas, chegada de clientes,
 * atendimento, consulta de filas, transferência e histórico.
 * Usa Scanner/PrintStream diretamente, como os menus do E.1, enquanto InputReader e
 * TerminalOutput não forem implementados.
 */
public final class QueueMenu {
    private static final DateTimeFormatter HOUR = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final Scanner scanner;
    private final PrintStream out;
    private final QueueService queues;
    private final DashboardService dashboard;

    public QueueMenu(Scanner scanner, PrintStream out, QueueService queues,
                     DashboardService dashboard) {
        this.scanner = scanner;
        this.out = out;
        this.queues = queues;
        this.dashboard = dashboard;
    }

    public void run() {
        int option;
        do {
            option = readMenuOption("CAIXAS E FILAS", "Visão geral", "Adicionar caixa",
                    "Abrir caixa", "Fechar caixa (redistribui clientes)",
                    "Novo cliente (menor fila)", "Atender próximo cliente",
                    "Concluir atendimento atual", "Consultar fila de um caixa",
                    "Transferir cliente entre filas", "Histórico de atendimentos",
                    "Qual caixa tem a menor fila?", "Remover caixa fechado");
            try {
                switch (option) {
                    case 1 -> overview();
                    case 2 -> success("Caixa " + queues.createCheckout().id() + " criado (fechado).");
                    case 3 -> success("Caixa " + queues.openCheckout(readRequired("Número do caixa: ")).id()
                            + " aberto.");
                    case 4 -> closeCheckout();
                    case 5 -> newCustomer();
                    case 6 -> attendNext();
                    case 7 -> finishAttendance();
                    case 8 -> showQueue();
                    case 9 -> transfer();
                    case 10 -> history();
                    case 11 -> shortestQueue();
                    case 12 -> removeCheckout();
                    default -> { }
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                error(e.getMessage());
            }
        } while (option != 0);
    }

    private void overview() {
        QueueOverview o = dashboard.queueOverview();
        title("Visão Geral");
        println("Caixas abertos ....... " + o.openCheckouts() + " de " + o.totalCheckouts());
        println("Clientes aguardando .. " + o.waitingCustomers());
        println("Total atendidos ...... " + o.totalAttendances());
        println("Espera média ......... " + format(o.averageWait()));
        println("Mais atendimentos .... " + o.busiestCheckoutId().map(id -> "CAIXA " + id).orElse("-"));
        println("Menor fila ........... " + o.shortestQueueCheckoutId().map(id -> "CAIXA " + id).orElse("-"));
        println("---------------------------------");
        for (CheckoutSummary c : o.checkouts()) {
            StringBuilder line = new StringBuilder("CAIXA " + c.checkoutId()
                    + " [" + (c.open() ? "ABERTO" : "FECHADO") + "]");
            if (c.open()) {
                line.append(" aguardando: ").append(c.waiting())
                        .append(" | atendidos: ").append(c.attendances())
                        .append(" | espera média: ").append(format(c.averageWait()));
                c.customerInService().ifPresent(customer ->
                        line.append(" | em atendimento: ").append(customer.ticket()));
            }
            println(line.toString());
        }
    }

    private void closeCheckout() {
        String id = readRequired("Número do caixa: ");
        queues.closeCheckout(id);
        success("Caixa " + id + " fechado; clientes pendentes foram redistribuídos.");
    }

    private void newCustomer() {
        String name = readLine("Nome do cliente: ");
        boolean priority = confirm("Prioritário?");
        CustomerAssignment a = queues.registerArrival(name, priority, LocalDateTime.now());
        success("Senha " + a.customer().ticket() + " -> CAIXA " + a.checkoutId()
                + " (entrada " + hour(a.customer().arrivalTime()) + ")");
    }

    private void attendNext() {
        String id = readRequired("Número do caixa: ");
        Optional<ServiceRecord> record = queues.startNextAttendance(id, LocalDateTime.now());
        if (record.isEmpty()) {
            warning("Fila vazia. O atendimento anterior (se houver) foi concluído.");
            return;
        }
        success("Chamando " + record.get().customer() + " | espera "
                + format(record.get().calculateWaitTime()));
    }

    private void finishAttendance() {
        String id = readRequired("Número do caixa: ");
        queues.finishAttendance(id).ifPresentOrElse(
                customer -> success("Atendimento de " + customer + " concluído."),
                () -> warning("Não há atendimento em andamento neste caixa."));
    }

    private void showQueue() {
        Checkout c = requireCheckout(readRequired("Número do caixa: "));
        title("CAIXA " + c.id() + " [" + (c.isOpen() ? "ABERTO" : "FECHADO") + "] - aguardando: "
                + c.waitingCount() + " de " + c.queueCapacity());
        c.customerInService().ifPresent(customer -> println("  Em atendimento: " + customer));
        List<Customer> waiting = c.waitingCustomers();
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < waiting.size(); i++) {
            Customer customer = waiting.get(i);
            println("  #" + (i + 1) + " " + customer + " | entrada " + hour(customer.arrivalTime())
                    + " | esperando " + format(Duration.between(customer.arrivalTime(), now)));
        }
    }

    private void transfer() {
        String source = readRequired("Caixa de origem: ");
        String ticket = readRequired("Senha do cliente (ex.: S001): ");
        String destination = readRequired("Caixa de destino: ");
        queues.transferCustomer(ticket, source, destination);
        success("Cliente " + ticket.toUpperCase() + " transferido para a fila do caixa "
                + destination + ".");
    }

    private void history() {
        String id = readLine("Caixa (vazio = todos): ");
        List<ServiceRecord> records = id.isEmpty() ? queues.serviceRecords() : queues.serviceRecordsOf(id);
        title("Histórico de Atendimentos");
        if (records.isEmpty()) {
            println("Nenhum atendimento registrado.");
            return;
        }
        for (int i = records.size() - 1; i >= 0; i--) { // mais recentes primeiro
            ServiceRecord r = records.get(i);
            println(r.customer() + " | CX " + r.checkoutId()
                    + " | entrada " + hour(r.arrivalTime())
                    + " | atendimento " + hour(r.serviceStartedAt())
                    + " | espera " + format(r.calculateWaitTime()));
        }
    }

    private void shortestQueue() {
        queues.shortestOpenCheckout().ifPresentOrElse(
                c -> println("Menor fila: CAIXA " + c.id() + " com " + c.waitingCount() + " aguardando."),
                () -> warning("Nenhum caixa aberto com espaço."));
    }

    private void removeCheckout() {
        String id = readRequired("Número do caixa: ");
        queues.removeCheckout(id);
        success("Caixa " + id + " removido.");
    }

    private Checkout requireCheckout(String id) {
        return queues.checkout(id)
                .orElseThrow(() -> new IllegalArgumentException("Caixa não encontrado: " + id));
    }

    // ================================================================ formatação
    static String format(Duration duration) {
        long total = Math.max(0, duration.getSeconds());
        return (total / 60) + "m " + String.format("%02d", total % 60) + "s";
    }

    private static String hour(LocalDateTime dateTime) {
        return dateTime == null ? "--:--:--" : dateTime.format(HOUR);
    }

    // ================================================================ entrada e saída
    private void println(String text) { out.println(text); }

    private void error(String text) { out.println("  [ERRO] " + text); }

    private void success(String text) { out.println("  [OK] " + text); }

    private void warning(String text) { out.println("  [!] " + text); }

    private void title(String title) {
        out.println();
        out.println("=== " + title + " ===");
    }

    private String readLine(String prompt) {
        out.print(prompt);
        out.flush();
        return scanner.nextLine().trim();
    }

    private String readRequired(String prompt) {
        while (true) {
            String text = readLine(prompt);
            if (!text.isEmpty()) return text;
            error("Este campo é obrigatório.");
        }
    }

    private boolean confirm(String prompt) {
        while (true) {
            String text = readLine(prompt + " (s/n): ").toLowerCase();
            if (text.equals("s") || text.equals("sim")) return true;
            if (text.equals("n") || text.equals("nao") || text.equals("não")) return false;
            error("Responda com 's' ou 'n'.");
        }
    }

    private int readMenuOption(String title, String... options) {
        title(title);
        for (int i = 0; i < options.length; i++) {
            println("  " + (i + 1) + ") " + options[i]);
        }
        println("  0) Voltar/Sair");
        while (true) {
            String text = readLine("Escolha: ");
            try {
                int value = Integer.parseInt(text);
                if (value >= 0 && value <= options.length) return value;
            } catch (NumberFormatException ignored) {
                // cai na mensagem abaixo
            }
            error("Informe um número entre 0 e " + options.length + ".");
        }
    }
}
