package br.com.supermercado.presentation.cli;

import br.com.supermercado.application.checkout.QueueService;
import br.com.supermercado.application.checkout.QueueService.CustomerAssignment;
import br.com.supermercado.application.reporting.DashboardService;
import br.com.supermercado.application.reporting.DashboardService.CheckoutSummary;
import br.com.supermercado.application.reporting.DashboardService.QueueOverview;
import br.com.supermercado.domain.checkout.Checkout;
import br.com.supermercado.domain.checkout.Customer;
import br.com.supermercado.domain.checkout.ServiceRecord;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

// Menu de CAIXAS E FILAS: abrir/fechar caixas, chegada de clientes,
// atendimento, transferência e histórico.
public class QueueMenu {
    private final InputReader in;
    private final TerminalOutput out;
    private final QueueService queues;
    private final DashboardService dashboard;

    public QueueMenu(InputReader in, TerminalOutput out, QueueService queues, DashboardService dashboard) {
        this.in = in;
        this.out = out;
        this.queues = queues;
        this.dashboard = dashboard;
    }

    public void run() {
        int option;
        do {
            option = in.readMenuOption("CAIXAS E FILAS", "Visão geral", "Adicionar caixa",
                    "Abrir caixa", "Fechar caixa (redistribui clientes)",
                    "Novo cliente (menor fila)", "Atender próximo cliente",
                    "Concluir atendimento atual", "Consultar fila de um caixa",
                    "Transferir cliente entre filas", "Histórico de atendimentos",
                    "Qual caixa tem a menor fila?", "Remover caixa fechado");
            try {
                switch (option) {
                    case 1: overview(); break;
                    case 2: addCheckout(); break;
                    case 3: openCheckout(); break;
                    case 4: closeCheckout(); break;
                    case 5: newCustomer(); break;
                    case 6: attendNext(); break;
                    case 7: finishAttendance(); break;
                    case 8: showQueue(); break;
                    case 9: transfer(); break;
                    case 10: history(); break;
                    case 11: shortestQueue(); break;
                    case 12: removeCheckout(); break;
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                out.error(e.getMessage());
            }
        } while (option != 0);
    }

    private void overview() {
        QueueOverview o = dashboard.queueOverview();
        out.title("Visão Geral");
        out.println("Caixas abertos ....... " + o.openCheckouts() + " de " + o.totalCheckouts());
        out.println("Clientes aguardando .. " + o.waitingCustomers());
        out.println("Total atendidos ...... " + o.totalAttendances());
        out.println("Espera média ......... " + TerminalOutput.duration(o.averageWait()));
        out.println("Mais atendimentos .... " + checkoutName(o.busiestCheckoutId()));
        out.println("Menor fila ........... " + checkoutName(o.shortestQueueCheckoutId()));
        out.println("---------------------------------");
        for (CheckoutSummary c : o.checkouts()) {
            String line = "CAIXA " + c.checkoutId() + " [" + (c.open() ? "ABERTO" : "FECHADO") + "]";
            if (c.open()) {
                line += " aguardando: " + c.waiting()
                        + " | atendidos: " + c.attendances()
                        + " | espera média: " + TerminalOutput.duration(c.averageWait());
                if (c.customerInService() != null) {
                    line += " | em atendimento: " + c.customerInService().ticket();
                }
            }
            out.println(line);
        }
    }

    private String checkoutName(String id) {
        if (id == null) {
            return "-";
        }
        return "CAIXA " + id;
    }

    private void addCheckout() {
        Checkout c = queues.createCheckout();
        out.success("Caixa " + c.id() + " criado (fechado).");
    }

    private void openCheckout() {
        Checkout c = queues.openCheckout(in.readRequired("Número do caixa: "));
        out.success("Caixa " + c.id() + " aberto.");
    }

    private void closeCheckout() {
        String id = in.readRequired("Número do caixa: ");
        queues.closeCheckout(id);
        out.success("Caixa " + id + " fechado; clientes pendentes foram redistribuídos.");
    }

    private void newCustomer() {
        String name = in.readLine("Nome do cliente: ");
        boolean priority = in.confirm("Prioritário?");
        CustomerAssignment a = queues.registerArrival(name, priority, LocalDateTime.now());
        out.success("Senha " + a.customer().ticket() + " -> CAIXA " + a.checkoutId()
                + " (entrada " + TerminalOutput.hour(a.customer().arrivalTime()) + ")");
    }

    private void attendNext() {
        String id = in.readRequired("Número do caixa: ");
        ServiceRecord record = queues.startNextAttendance(id, LocalDateTime.now());
        if (record == null) {
            out.warning("Fila vazia. O atendimento anterior (se houver) foi concluído.");
            return;
        }
        out.success("Chamando " + record.customer() + " | espera "
                + TerminalOutput.duration(record.calculateWaitTime()));
    }

    private void finishAttendance() {
        String id = in.readRequired("Número do caixa: ");
        Customer customer = queues.finishAttendance(id);
        if (customer == null) {
            out.warning("Não há atendimento em andamento neste caixa.");
        } else {
            out.success("Atendimento de " + customer + " concluído.");
        }
    }

    private void showQueue() {
        String id = in.readRequired("Número do caixa: ");
        Checkout c = queues.checkout(id);
        if (c == null) {
            out.error("Caixa não encontrado: " + id);
            return;
        }
        out.title("CAIXA " + c.id() + " [" + (c.isOpen() ? "ABERTO" : "FECHADO") + "] - aguardando: "
                + c.waitingCount() + " de " + c.queueCapacity());
        if (c.customerInService() != null) {
            out.println("  Em atendimento: " + c.customerInService());
        }
        List<Customer> waiting = c.waitingCustomers();
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < waiting.size(); i++) {
            Customer customer = waiting.get(i);
            Duration waited = Duration.between(customer.arrivalTime(), now);
            out.println("  #" + (i + 1) + " " + customer + " | entrada " + TerminalOutput.hour(customer.arrivalTime())
                    + " | esperando " + TerminalOutput.duration(waited));
        }
    }

    private void transfer() {
        String source = in.readRequired("Caixa de origem: ");
        String ticket = in.readRequired("Senha do cliente (ex.: S001): ");
        String destination = in.readRequired("Caixa de destino: ");
        queues.transferCustomer(ticket, source, destination);
        out.success("Cliente " + ticket.toUpperCase() + " transferido para a fila do caixa " + destination + ".");
    }

    private void history() {
        String id = in.readLine("Caixa (vazio = todos): ");
        List<ServiceRecord> records;
        if (id.isEmpty()) {
            records = queues.serviceRecords();
        } else {
            records = queues.serviceRecordsOf(id);
        }
        out.title("Histórico de Atendimentos");
        if (records.isEmpty()) {
            out.println("Nenhum atendimento registrado.");
            return;
        }
        // percorre de trás para frente para mostrar os mais recentes primeiro
        for (int i = records.size() - 1; i >= 0; i--) {
            ServiceRecord r = records.get(i);
            out.println(r.customer() + " | CX " + r.checkoutId()
                    + " | entrada " + TerminalOutput.hour(r.arrivalTime())
                    + " | atendimento " + TerminalOutput.hour(r.serviceStartedAt())
                    + " | espera " + TerminalOutput.duration(r.calculateWaitTime()));
        }
    }

    private void shortestQueue() {
        Checkout c = queues.shortestOpenCheckout();
        if (c == null) {
            out.warning("Nenhum caixa aberto com espaço.");
        } else {
            out.println("Menor fila: CAIXA " + c.id() + " com " + c.waitingCount() + " aguardando.");
        }
    }

    private void removeCheckout() {
        String id = in.readRequired("Número do caixa: ");
        queues.removeCheckout(id);
        out.success("Caixa " + id + " removido.");
    }
}
