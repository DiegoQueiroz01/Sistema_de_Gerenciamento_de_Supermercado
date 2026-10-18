import java.time.LocalDateTime;

/**
 * Caixa de atendimento. Cada caixa possui uma fila independente,
 * um cliente em atendimento e seu proprio historico.
 *
 * Para fechar um caixa com clientes aguardando, use
 * GerenciadorFilas.fecharCaixa(), que transfere os clientes antes.
 */
public class Caixa {

    public static final String ABERTO = "ABERTO";
    public static final String FECHADO = "FECHADO";

    private final int id;
    private String status;
    private int totalAtendimentos;
    private float tempoMedioEspera; // em segundos
    private final FilaAtendimento fila;
    private final Lista<RegistroAtendimento> historico;
    private Cliente clienteEmAtendimento;

    public Caixa(int id, int capacidadeFila) {
        this.id = id;
        this.status = FECHADO;
        this.totalAtendimentos = 0;
        this.tempoMedioEspera = 0f;
        this.fila = new FilaAtendimento(id, "NORMAL", capacidadeFila);
        this.historico = new Lista<>();
        this.clienteEmAtendimento = null;
    }

    public void abrir() {
        status = ABERTO;
    }

    public void fechar() {
        status = FECHADO;
    }

    public boolean estaAberto() {
        return ABERTO.equals(status);
    }

    public boolean adicionarCliente(Cliente cliente) {
        if (!estaAberto()) {
            return false;
        }
        if (fila.adicionarCliente(cliente)) {
            cliente.definirCaixa(id);
            return true;
        }
        return false;
    }

    public Cliente chamarProximo() {
        if (!estaAberto() || fila.estaVazia()) {
            return null;
        }
        Cliente proximo = fila.removerProximoCliente();
        proximo.registrarAtendimento(LocalDateTime.now());
        clienteEmAtendimento = proximo;
        return proximo;
    }

    public boolean removerClienteAtendido(Cliente cliente) {
        if (cliente != null && clienteEmAtendimento == cliente) {
            clienteEmAtendimento = null;
            cliente.definirCaixa(0);
            return true;
        }
        return false;
    }

    public void concluirAtendimento() {
        removerClienteAtendido(clienteEmAtendimento);
    }

    public void atualizarEstatisticas(float tempoEspera) {
        tempoMedioEspera = (tempoMedioEspera * totalAtendimentos + tempoEspera) / (totalAtendimentos + 1);
        totalAtendimentos++;
    }

    public FilaAtendimento obterFila() {
        return fila;
    }

    public void registrarAtendimento(RegistroAtendimento registro) {
        historico.adicionar(registro);
        atualizarEstatisticas(registro.obterDuracaoEspera());
    }

    public int getId() {
        return id;
    }

    public String getStatus() {
        return status;
    }

    public int getTotalAtendimentos() {
        return totalAtendimentos;
    }

    public float getTempoMedioEspera() {
        return tempoMedioEspera;
    }

    public Lista<RegistroAtendimento> getHistorico() {
        return historico;
    }

    public Cliente getClienteEmAtendimento() {
        return clienteEmAtendimento;
    }
}
