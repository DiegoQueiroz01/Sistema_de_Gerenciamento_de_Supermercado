
public class Dashboard {

    private int totalCaixas;
    private int totalCaixasAtivas;
    private int totalClientesEmEspera;
    private int totalAtendidos;
    private float tempoMedioEsperaGeral; 
    private int caixaMaisMovimentada;    
    private int caixaMenorFila;          
    private int filasAtivas;
    private GerenciadorFilas gerenciador;

    public void atualizarIndicadores(GerenciadorFilas gerenciador, Lista<RegistroAtendimento> registros) {
        this.gerenciador = gerenciador;
        totalCaixas = gerenciador.getCaixas().tamanho();
        totalCaixasAtivas = gerenciador.obterCaixasAtivas().tamanho();
        totalClientesEmEspera = contarClientesEmEspera(gerenciador);
        totalAtendidos = registros.tamanho();
        tempoMedioEsperaGeral = calcularTempoMedioEspera(registros);
        Caixa mais = identificarCaixaMaisMovimentada(gerenciador.getCaixas());
        caixaMaisMovimentada = (mais == null) ? 0 : mais.getId();
        Caixa menor = gerenciador.encontrarCaixaMenorFila();
        caixaMenorFila = (menor == null) ? 0 : menor.getId();
        filasAtivas = listarFilasAtivas(gerenciador).tamanho();
    }

    public float calcularTempoMedioEspera(Lista<RegistroAtendimento> registros) {
        if (registros.estaVazia()) {
            return 0f;
        }
        float soma = 0f;
        for (int i = 0; i < registros.tamanho(); i++) {
            soma += registros.obter(i).obterDuracaoEspera();
        }
        return soma / registros.tamanho();
    }

    public Caixa identificarCaixaMaisMovimentada(Lista<Caixa> caixas) {
        Caixa melhor = null;
        for (int i = 0; i < caixas.tamanho(); i++) {
            Caixa atual = caixas.obter(i);
            if (atual.getTotalAtendimentos() > 0
                    && (melhor == null || atual.getTotalAtendimentos() > melhor.getTotalAtendimentos())) {
                melhor = atual;
            }
        }
        return melhor;
    }

    public Lista<FilaAtendimento> listarFilasAtivas(GerenciadorFilas gerenciador) {
        Lista<FilaAtendimento> filas = new Lista<>();
        Lista<Caixa> ativas = gerenciador.obterCaixasAtivas();
        for (int i = 0; i < ativas.tamanho(); i++) {
            filas.adicionar(ativas.obter(i).obterFila());
        }
        return filas;
    }

    public int contarClientesEmEspera(GerenciadorFilas gerenciador) {
        return gerenciador.contarClientesAguardando();
    }

    public String exibirResumo() {
        StringBuilder sb = new StringBuilder();
        sb.append("========== VISAO GERAL ==========\n");
        sb.append("Caixas abertos ....... ").append(totalCaixasAtivas).append(" de ").append(totalCaixas).append("\n");
        sb.append("Clientes aguardando .. ").append(totalClientesEmEspera).append("\n");
        sb.append("Total atendidos ...... ").append(totalAtendidos).append("\n");
        sb.append("Espera media ......... ").append(Tempo.formatar(tempoMedioEsperaGeral)).append("\n");
        sb.append("Mais atendimentos .... ").append(caixaMaisMovimentada == 0 ? "-" : "CAIXA " + caixaMaisMovimentada).append("\n");
        sb.append("Menor fila ........... ").append(caixaMenorFila == 0 ? "-" : "CAIXA " + caixaMenorFila).append("\n");
        sb.append("---------------------------------\n");
        if (gerenciador != null) {
            Lista<Caixa> caixas = gerenciador.getCaixas();
            for (int i = 0; i < caixas.tamanho(); i++) {
                Caixa c = caixas.obter(i);
                sb.append("CAIXA ").append(c.getId() < 10 ? "0" : "").append(c.getId())
                        .append(" [").append(c.getStatus()).append("]");
                if (c.estaAberto()) {
                    sb.append(" aguardando: ").append(c.obterFila().contarClientesEspera())
                            .append(" | atendidos: ").append(c.getTotalAtendimentos())
                            .append(" | espera media: ").append(Tempo.formatar(c.getTempoMedioEspera()));
                    if (c.getClienteEmAtendimento() != null) {
                        sb.append(" | em atendimento: ").append(c.getClienteEmAtendimento().getSenha());
                    }
                }
                sb.append("\n");
            }
        }
        sb.append("=================================");
        return sb.toString();
    }

    public int getTotalCaixasAtivas() {
        return totalCaixasAtivas;
    }

    public int getTotalClientesEmEspera() {
        return totalClientesEmEspera;
    }

    public float getTempoMedioEsperaGeral() {
        return tempoMedioEsperaGeral;
    }

    public int getCaixaMaisMovimentada() {
        return caixaMaisMovimentada;
    }

    public int getFilasAtivas() {
        return filasAtivas;
    }
}
