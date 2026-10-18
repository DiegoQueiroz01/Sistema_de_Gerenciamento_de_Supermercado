import java.time.Duration;
import java.time.LocalDateTime;


public class Cliente {

    private final int idSequencial;
    private final String senha;
    private final String nome;
    private LocalDateTime dataHoraEntrada;
    private LocalDateTime dataHoraAtendimento;
    private float tempoEspera; 
    private boolean prioritario;
    private int caixaId; 

    public Cliente(int idSequencial, String senha, String nome, boolean prioritario) {
        this.idSequencial = idSequencial;
        this.senha = senha;
        this.nome = nome;
        this.prioritario = prioritario;
        this.caixaId = 0;
    }

    public void registrarEntrada(LocalDateTime dataHoraEntrada) {
        this.dataHoraEntrada = dataHoraEntrada;
    }

    public void registrarAtendimento(LocalDateTime dataHoraAtendimento) {
        this.dataHoraAtendimento = dataHoraAtendimento;
        this.tempoEspera = calcularTempoEspera();
    }

   
    public float calcularTempoEspera() {
        if (dataHoraEntrada == null) {
            return 0f;
        }
        LocalDateTime fim = (dataHoraAtendimento != null) ? dataHoraAtendimento : LocalDateTime.now();
        return Duration.between(dataHoraEntrada, fim).toMillis() / 1000f;
    }

    public void definirPrioridade(boolean prioritario) {
        this.prioritario = prioritario;
    }

    public String obterSenhaFormatada() {
        return senha;
    }

    public boolean estaAguardando() {
        return dataHoraAtendimento == null;
    }

    public void definirCaixa(int caixaId) {
        this.caixaId = caixaId;
    }

    public int getIdSequencial() {
        return idSequencial;
    }

    public String getSenha() {
        return senha;
    }

    public String getNome() {
        return nome;
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

    public boolean isPrioritario() {
        return prioritario;
    }

    public int getCaixaId() {
        return caixaId;
    }

    @Override
    public String toString() {
        return senha + " " + nome + (prioritario ? " [PRIO]" : "");
    }
}
