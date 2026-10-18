import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;


public class Main {

    private static final int NUMERO_CAIXAS_INICIAL = 5;
    private static final int CAPACIDADE_FILA = 100;

    private static BufferedReader entrada;
    private static GerenciadorFilas gerenciador;
    private static Dashboard dashboard;

    public static void main(String[] args) throws IOException {
        entrada = new BufferedReader(new InputStreamReader(System.in));
        gerenciador = new GerenciadorFilas(CAPACIDADE_FILA);
        dashboard = new Dashboard();
        for (int i = 0; i < NUMERO_CAIXAS_INICIAL; i++) {
            gerenciador.criarCaixa();
        }

        int opcao;
        do {
            exibirMenu();
            opcao = lerInt("Opcao: ");
            System.out.println();
            executar(opcao);
        } while (opcao != 0);
        System.out.println("Encerrando.");
    }

    private static void exibirMenu() {
        System.out.println("\n===== SUPERMERCADO - FILAS DE CAIXA =====");
        System.out.println(" 1 - Visao geral");
        System.out.println(" 2 - Adicionar caixa");
        System.out.println(" 3 - Abrir caixa");
        System.out.println(" 4 - Fechar caixa (transfere clientes)");
        System.out.println(" 5 - Novo cliente (menor fila)");
        System.out.println(" 6 - Atender proximo / concluir atendimento");
        System.out.println(" 7 - Consultar fila de um caixa");
        System.out.println(" 8 - Transferir cliente entre filas");
        System.out.println(" 9 - Historico de atendimentos");
        System.out.println("10 - Qual caixa tem a menor fila?");
        System.out.println("11 - Carregar demonstracao");
        System.out.println(" 0 - Sair");
    }

    private static void executar(int opcao) throws IOException {
        switch (opcao) {
            case 1:
                visaoGeral();
                break;
            case 2:
                Caixa novo = gerenciador.criarCaixa();
                System.out.println("Caixa " + novo.getId() + " criado (fechado).");
                break;
            case 3:
                int abrir = lerInt("Numero do caixa: ");
                System.out.println(gerenciador.abrirCaixa(abrir) ? "Caixa aberto." : "Nao foi possivel abrir (inexistente ou ja aberto).");
                break;
            case 4:
                int fechar = lerInt("Numero do caixa: ");
                System.out.println(gerenciador.fecharCaixa(fechar)
                        ? "Caixa fechado; clientes pendentes foram redistribuidos."
                        : "Nao foi possivel fechar (inexistente, ja fechado ou sem outro caixa aberto com espaco).");
                break;
            case 5:
                novoCliente();
                break;
            case 6:
                atender();
                break;
            case 7:
                consultarFila();
                break;
            case 8:
                transferir();
                break;
            case 9:
                historico();
                break;
            case 10:
                Caixa menor = gerenciador.encontrarCaixaMenorFila();
                System.out.println(menor == null ? "Nenhum caixa aberto."
                        : "Menor fila: CAIXA " + menor.getId() + " com "
                        + menor.obterFila().contarClientesEspera() + " aguardando.");
                break;
            case 11:
                demonstracao();
                break;
            case 0:
                break;
            default:
                System.out.println("Opcao invalida.");
        }
    }

    private static void visaoGeral() {
        dashboard.atualizarIndicadores(gerenciador, gerenciador.getHistoricoGeral());
        System.out.println(dashboard.exibirResumo());
    }

    private static void novoCliente() throws IOException {
        System.out.print("Nome do cliente: ");
        String nome = entrada.readLine();
        if (nome == null || nome.trim().isEmpty()) {
            nome = "Cliente";
        }
        System.out.print("Prioritario? (s/n): ");
        String resp = entrada.readLine();
        boolean prioritario = resp != null && resp.trim().equalsIgnoreCase("s");
        Cliente cliente = gerenciador.registrarNovoCliente(nome.trim(), prioritario);
        if (cliente == null) {
            System.out.println("Nenhum caixa aberto com espaco. Abra um caixa primeiro.");
        } else {
            System.out.println("Senha " + cliente.getSenha() + " -> CAIXA " + cliente.getCaixaId()
                    + " (entrada " + Tempo.hora(cliente.getDataHoraEntrada()) + ")");
        }
    }

    private static void atender() {
        int id = lerInt("Numero do caixa: ");
        Caixa caixa = gerenciador.obterCaixa(id);
        if (caixa == null || !caixa.estaAberto()) {
            System.out.println("Caixa inexistente ou fechado.");
            return;
        }
        Cliente atendido = gerenciador.atenderProximo(id);
        if (atendido == null) {
            System.out.println("Fila vazia. Atendimento anterior (se houver) foi concluido.");
        } else {
            System.out.println("Chamando " + atendido + " | espera " + Tempo.formatar(atendido.getTempoEspera()));
        }
    }

    private static void consultarFila() {
        int id = lerInt("Numero do caixa: ");
        Caixa caixa = gerenciador.obterCaixa(id);
        if (caixa == null) {
            System.out.println("Caixa inexistente.");
            return;
        }
        System.out.println("CAIXA " + id + " [" + caixa.getStatus() + "] - aguardando: "
                + caixa.obterFila().contarClientesEspera());
        if (caixa.getClienteEmAtendimento() != null) {
            System.out.println("  Em atendimento: " + caixa.getClienteEmAtendimento());
        }
        Lista<Cliente> clientes = caixa.obterFila().listarClientes();
        for (int i = 0; i < clientes.tamanho(); i++) {
            Cliente c = clientes.obter(i);
            System.out.println("  #" + (i + 1) + " " + c + " | entrada " + Tempo.hora(c.getDataHoraEntrada())
                    + " | esperando " + Tempo.formatar(c.calcularTempoEspera()));
        }
    }

    private static void transferir() throws IOException {
        int origem = lerInt("Caixa de origem: ");
        System.out.print("Senha do cliente: ");
        String senha = entrada.readLine();
        int destino = lerInt("Caixa de destino: ");
        System.out.println(senha != null && gerenciador.transferirCliente(origem, senha.trim(), destino)
                ? "Cliente transferido."
                : "Transferencia nao realizada (verifique caixas, senha e se o destino esta aberto).");
    }

    private static void historico() {
        int id = lerInt("Caixa (0 = todos): ");
        Lista<RegistroAtendimento> lista;
        if (id == 0) {
            lista = gerenciador.getHistoricoGeral();
        } else {
            Caixa caixa = gerenciador.obterCaixa(id);
            if (caixa == null) {
                System.out.println("Caixa inexistente.");
                return;
            }
            lista = caixa.getHistorico();
        }
        if (lista.estaVazia()) {
            System.out.println("Nenhum atendimento registrado.");
            return;
        }
        for (int i = lista.tamanho() - 1; i >= 0; i--) { // mais recentes primeiro
            System.out.println(lista.obter(i).resumirAtendimento());
        }
    }

    private static void demonstracao() {
        gerenciador.abrirCaixa(1);
        gerenciador.abrirCaixa(2);
        gerenciador.abrirCaixa(3);
        String[] nomes = {"Ana", "Bruno", "Carla", "Daniel", "Elisa", "Fabio", "Gabriela"};
        for (int i = 0; i < nomes.length; i++) {
            boolean prioritario = (i == 2); // Carla e prioritaria
            gerenciador.registrarNovoCliente(nomes[i], prioritario);
        }
        pausar(1500);
        gerenciador.atenderProximo(1);
        gerenciador.atenderProximo(2);
        System.out.println("Demonstracao carregada (3 caixas abertos, 7 clientes, 2 atendimentos).");
        visaoGeral();
    }

    private static void pausar(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static int lerInt(String mensagem) {
        while (true) {
            System.out.print(mensagem);
            try {
                String linha = entrada.readLine();
                if (linha == null) {
                    return 0;
                }
                return Integer.parseInt(linha.trim());
            } catch (NumberFormatException e) {
                System.out.println("Digite um numero inteiro.");
            } catch (IOException e) {
                return 0;
            }
        }
    }
}
