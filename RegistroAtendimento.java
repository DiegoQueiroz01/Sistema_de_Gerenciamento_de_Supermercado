import java.time.LocalDateTime;

/**
 * Registro historico de um atendimento: consolida dados do cliente e do caixa.
 */
public class RegistroAtendimento {

    private final int id;
    private String senhaCliente;
    private String nomeCliente;
    private boolean prioritario;
    private LocalDateTime dataHoraEntrada;
    private LocalDateTime dataHoraAtendimento;
    private float tempoEspera; 
    private int caixaId;
    private int clienteId;

    public RegistroAtendimento(int id) {
        this.id = id;
    }

    public void criarRegistro(Cliente cliente, Caixa caixa) {
        atualizarCliente(cliente);
        atualizarCaixa(caixa);
        calcularTempoEspera();
    }

    public float calcularTempoEspera() {
        if (dataHoraEntrada != null && dataHoraAtendimento != null) {
            tempoEspera = java.time.Duration.between(dataHoraEntrada, dataHoraAtendimento).toMillis() / 1000f;
        }
        return tempoEspera;
    }

    public float obterDuracaoEspera() {
        return tempoEspera;
    }

    public String resumirAtendimento() {
        return senhaCliente + " " + nomeCliente + (prioritario ? " [PRIO]" : "")
                + " | CX " + (caixaId < 10 ? "0" : "") + caixaId
                + " | entrada " + Tempo.hora(dataHoraEntrada)
                + " | atendimento " + Tempo.hora(dataHoraAtendimento)
                + " | espera " + Tempo.formatar(tempoEspera);
    }

    public void atualizarCaixa(Caixa caixa) {
        this.caixaId = caixa.getId();
    }

    public void atualizarCliente(Cliente cliente) {
        this.clienteId = cliente.getIdSequencial();
        this.senhaCliente = cliente.getSenha();
        this.nomeCliente = cliente.getNome();
        this.prioritario = cliente.isPrioritario();
        this.dataHoraEntrada = cliente.getDataHoraEntrada();
        this.dataHoraAtendimento = cliente.getDataHoraAtendimento();
    }

    public int getId() {
        return id;
    }

    public String getSenhaCliente() {
        return senhaCliente;
    }

    public String getNomeCliente() {
        return nomeCliente;
    }

    public boolean isPrioritario() {
        return prioritario;
    }

    public LocalDateTime getDataHoraEntrada() {
        return dataHoraEntrada;
    }

    public LocalDateTime getDataHoraAtendimento() {
        return dataHoraAtendimento;
    }

    public float getTempoEspera() {
        return tempoEspera;
    }

    public int getCaixaId() {
        return caixaId;
    }

    public int getClienteId() {
        return clienteId;
    }
}
