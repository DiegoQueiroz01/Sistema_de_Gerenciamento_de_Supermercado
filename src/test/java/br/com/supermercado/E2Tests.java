package br.com.supermercado;

import br.com.supermercado.application.checkout.QueueService;
import br.com.supermercado.application.reporting.DashboardService;
import br.com.supermercado.application.reporting.DashboardService.QueueOverview;
import br.com.supermercado.domain.checkout.Checkout;
import br.com.supermercado.domain.checkout.Customer;
import br.com.supermercado.domain.checkout.ServiceRecord;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Testes de caixas e filas sem dependências externas.
 * Execução: mvn compile exec:java -Dexec.mainClass=br.com.supermercado.E2Tests -Dexec.classpathScope=test
 */
public class E2Tests {
    static int passed = 0, failed = 0;

    interface Action { void run() throws Exception; }

    static void check(String name, boolean condition) {
        if (condition) { passed++; System.out.println("PASSOU  " + name); }
        else { failed++; System.out.println("FALHOU  " + name); }
    }

    static boolean throwsType(Class<? extends Exception> type, Action action) {
        try { action.run(); return false; } catch (Exception e) { return type.isInstance(e); }
    }

    static List<String> tickets(Checkout checkout) {
        return checkout.waitingCustomers().stream().map(Customer::ticket).toList();
    }

    public static void main(String[] args) {
        LocalDateTime t0 = LocalDateTime.of(2026, 1, 1, 10, 0);

        // Caixas
        QueueService q = new QueueService();
        Checkout c1 = q.createCheckout();
        Checkout c2 = q.createCheckout();
        check("CF01 caixas criados fechados e numerados", c1.id().equals("1") && c2.id().equals("2")
                && !c1.isOpen() && !c2.isOpen());
        check("CF02 chegada sem caixa aberto recusada", throwsType(IllegalStateException.class,
                () -> q.registerArrival("Ana", false, t0)));
        q.openCheckout("1");
        check("CF03 abrir caixa já aberto recusado", throwsType(IllegalStateException.class,
                () -> q.openCheckout("1")));

        // Chegada, senha e prioridade
        Customer ana = q.registerArrival("Ana", false, t0).customer();
        Customer bruno = q.registerArrival("Bruno", false, t0.plusMinutes(1)).customer();
        Customer carla = q.registerArrival("Carla", true, t0.plusMinutes(2)).customer();
        check("CF04 senha sequencial formatada", ana.ticket().equals("S001") && carla.ticket().equals("S003"));
        check("CF05 nome vazio vira 'Cliente'", q.registerArrival(" ", false, t0.plusMinutes(3))
                .customer().name().equals("Cliente"));
        check("CF06 prioritário passa à frente dos comuns", tickets(c1).equals(List.of("S003", "S001", "S002", "S004")));

        // Menor fila
        q.openCheckout("2");
        check("CF07 novo cliente vai para a menor fila", q.registerArrival("Daniel", false, t0.plusMinutes(4))
                .checkoutId().equals("2"));
        check("CF08 menor fila consultável", q.shortestOpenCheckout().orElseThrow().id().equals("2"));

        // Transferência pela senha
        q.transferCustomer("s002", "1", "2");
        check("CF09 transferência pela senha respeita a ordem de chegada", tickets(c2).equals(List.of("S002", "S005"))
                && !c1.hasCustomer(bruno.id()));
        check("CF10 transferência com senha inexistente recusada", throwsType(IllegalArgumentException.class,
                () -> q.transferCustomer("S999", "1", "2")));

        // Atendimento
        ServiceRecord r1 = q.startNextAttendance("1", t0.plusMinutes(10)).orElseThrow();
        check("CF11 atende primeiro o prioritário", r1.customer().ticket().equals("S003")
                && c1.customerInService().orElseThrow().equals(carla));
        check("CF12 espera calculada (8 min)", r1.calculateWaitTime().equals(Duration.ofMinutes(8)));
        q.startNextAttendance("1", t0.plusMinutes(12));
        check("CF13 chamar o próximo conclui o atendimento anterior",
                c1.customerInService().orElseThrow().equals(ana));
        check("CF14 concluir atendimento libera o caixa", q.finishAttendance("1").isPresent()
                && c1.customerInService().isEmpty());
        check("CF15 histórico por caixa", q.serviceRecordsOf("1").size() == 2 && q.serviceRecordsOf("2").isEmpty());

        // Painel
        QueueOverview o = new DashboardService(q).queueOverview();
        check("CF16 painel: totais", o.totalCheckouts() == 2 && o.openCheckouts() == 2
                && o.waitingCustomers() == 3 && o.totalAttendances() == 2);
        check("CF17 painel: espera média e caixa mais movimentado",
                o.averageWait().equals(Duration.ofMinutes(10)) && o.busiestCheckoutId().orElseThrow().equals("1"));

        // Fechamento
        q.startNextAttendance("1", t0.plusMinutes(13));
        check("CF18 chegada vai para o caixa 1 (fila vazia)", q.registerArrival("Elisa", false, t0.plusMinutes(14))
                .checkoutId().equals("1"));
        q.closeCheckout("1");
        check("CF19 fechar caixa conclui atendimento e redistribui", !c1.isOpen()
                && c1.customerInService().isEmpty() && tickets(c2).equals(List.of("S002", "S005", "S006")));
        check("CF20 fechar o último caixa com fila recusado", throwsType(IllegalStateException.class,
                () -> q.closeCheckout("2")));
        check("CF21 remover caixa aberto recusado", throwsType(IllegalStateException.class,
                () -> q.removeCheckout("2")));
        q.removeCheckout("1");
        check("CF22 remover caixa fechado", q.checkout("1").isEmpty());

        // Capacidade
        QueueService small = new QueueService(1);
        small.createCheckout();
        small.createCheckout();
        small.openCheckout("1");
        small.openCheckout("2");
        small.registerArrival("A", false, t0);
        small.registerArrival("B", false, t0);
        check("CF23 sem vaga em nenhuma fila recusa chegada", throwsType(IllegalStateException.class,
                () -> small.registerArrival("C", false, t0)));
        check("CF24 fechar sem espaço nos outros caixas recusado", throwsType(IllegalStateException.class,
                () -> small.closeCheckout("1")) && small.checkout("1").orElseThrow().waitingCount() == 1);
        check("CF25 transferência para fila cheia não perde o cliente", throwsType(IllegalStateException.class,
                () -> small.transferCustomer("S001", "1", "2"))
                && small.checkout("1").orElseThrow().waitingCount() == 1);

        // Prioritário na transferência e no fechamento
        QueueService p = new QueueService();
        Checkout p1 = p.createCheckout();
        Checkout p2 = p.createCheckout();
        p.openCheckout("1");
        p.registerArrival("Comum A", false, t0);                       // S001 -> 1
        p.openCheckout("2");
        p.registerArrival("Comum B", false, t0.plusMinutes(1));        // S002 -> 2
        p.registerArrival("Prio C", true, t0.plusMinutes(2));          // S003 -> 1
        p.registerArrival("Prio D", true, t0.plusMinutes(3));          // S004 -> 2
        p.transferCustomer("S003", "1", "2");
        check("CF26 prioritário transferido fica à frente dos comuns",
                tickets(p2).equals(List.of("S003", "S004", "S002")));
        p.closeCheckout("1");
        check("CF27 fechamento mantém prioridade e ordem de chegada",
                !p1.isOpen() && tickets(p2).equals(List.of("S003", "S004", "S001", "S002")));

        // Demonstração
        QueueService demo = E2Demo.createSeededQueues();
        check("CF28 demonstração: 3 abertos, 5 aguardando, 2 atendidos", demo.openCheckouts().size() == 3
                && demo.totalWaiting() == 5 && demo.serviceRecords().size() == 2);

        System.out.println("\nResultado: " + passed + " passaram, " + failed + " falharam.");
        System.exit(failed == 0 ? 0 : 1);
    }
}
