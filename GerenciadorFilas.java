import java.time.LocalDateTime;


public class GerenciadorFilas {

    private final Lista<Caixa> caixas;
    private final Lista<RegistroAtendimento> historicoGeral;
    private final int capacidadePadraoFila;
    private int proximaSenhaSequencial;
    private int proximoIdCaixa;
    private int proximoIdRegistro;
    private int totalClientesDirecionados;
    private int totalTransferencias;

    public GerenciadorFilas(int capacidadePadraoFila) {
        this.caixas = new Lista<>();
        this.historicoGeral = new Lista<>();
        this.capacidadePadraoFila = capacidadePadraoFila;
        this.proximaSenhaSequencial = 1;
        this.proximoIdCaixa = 1;
        this.proximoIdRegistro = 1;
    }

    
    public Caixa criarCaixa() {
        Caixa caixa = new Caixa(proximoIdCaixa++, capacidadePadraoFila);
        adicionarCaixa(caixa);
        return caixa;
    }

    public void adicionarCaixa(Caixa caixa) {
        if (caixa != null && obterCaixa(caixa.getId()) == null) {
            caixas.adicionar(caixa);
        }
    }

    public boolean removerCaixa(int caixaId) {
        Caixa caixa = obterCaixa(caixaId);
        if (caixa == null || !caixa.obterFila().estaVazia() || caixa.getClienteEmAtendimento() != null) {
            return false;
        }
        return caixas.remover(caixa);
    }

    public boolean abrirCaixa(int caixaId) {
        Caixa caixa = obterCaixa(caixaId);
        if (caixa == null || caixa.estaAberto()) {
            return false;
        }
        caixa.abrir();
        return true;
    }

   
    public boolean fecharCaixa(int caixaId) {
        Caixa caixa = obterCaixa(caixaId);
        if (caixa == null || !caixa.estaAberto()) {
            return false;
        }
        FilaAtendimento origem = caixa.obterFila();
        int pendentes = origem.contarClientesEspera();
        if (pendentes > 0 && vagasDisponiveisExcluindo(caixa) < pendentes) {
            return false; // nao ha outro caixa aberto com espaco
        }
        if (caixa.getClienteEmAtendimento() != null) {
            caixa.concluirAtendimento();
        }
        while (!origem.estaVazia()) {
            Cliente cliente = origem.obterProximoCliente();
            Caixa destino = encontrarCaixaMenorFila(caixa, true);
            transferirCliente(cliente, origem, destino.obterFila());
        }
        caixa.fechar();
        return true;
    }

    public Caixa obterCaixa(int caixaId) {
        for (int i = 0; i < caixas.tamanho(); i++) {
            if (caixas.obter(i).getId() == caixaId) {
                return caixas.obter(i);
            }
        }
        return null;
    }

    public Lista<Caixa> obterCaixasAtivas() {
        Lista<Caixa> ativas = new Lista<>();
        for (int i = 0; i < caixas.tamanho(); i++) {
            if (caixas.obter(i).estaAberto()) {
                ativas.adicionar(caixas.obter(i));
            }
        }
        return ativas;
    }

    
    public Caixa encontrarCaixaMenorFila() {
        return encontrarCaixaMenorFila(null, false);
    }

    public FilaAtendimento encontrarMenorFila() {
        Caixa caixa = encontrarCaixaMenorFila();
        return caixa == null ? null : caixa.obterFila();
    }

    private Caixa encontrarCaixaMenorFila(Caixa excluir, boolean ignorarCheias) {
        Caixa menor = null;
        for (int i = 0; i < caixas.tamanho(); i++) {
            Caixa atual = caixas.obter(i);
            if (!atual.estaAberto() || atual == excluir) {
                continue;
            }
            if (ignorarCheias && atual.obterFila().estaCheia()) {
                continue;
            }
            if (menor == null || atual.obterFila().contarClientesEspera() < menor.obterFila().contarClientesEspera()) {
                menor = atual;
            }
        }
        return menor;
    }

    private int vagasDisponiveisExcluindo(Caixa excluir) {
        int vagas = 0;
        for (int i = 0; i < caixas.tamanho(); i++) {
            Caixa atual = caixas.obter(i);
            if (atual.estaAberto() && atual != excluir) {
                vagas += atual.obterFila().getCapacidadeMaxima() - atual.obterFila().contarClientesEspera();
            }
        }
        return vagas;
    }

    
    public Cliente registrarNovoCliente(String nome, boolean prioritario) {
        if (encontrarCaixaMenorFila(null, true) == null) {
            return null;
        }
        int numero = proximaSenhaSequencial;
        Cliente cliente = new Cliente(numero, gerarSenhaSequencial(), nome, prioritario);
        cliente.registrarEntrada(LocalDateTime.now());
        return direcionarNovoCliente(cliente) == null ? null : cliente;
    }

  
    public Caixa direcionarNovoCliente(Cliente cliente) {
        Caixa destino = encontrarCaixaMenorFila(null, true);
        if (destino == null || !destino.adicionarCliente(cliente)) {
            return null;
        }
        totalClientesDirecionados++;
        return destino;
    }

    public String gerarSenhaSequencial() {
        String numero = String.valueOf(proximaSenhaSequencial++);
        while (numero.length() < 3) {
            numero = "0" + numero;
        }
        return "S" + numero;
    }


    public boolean transferirCliente(Cliente cliente, FilaAtendimento origem, FilaAtendimento destino) {
        Caixa caixaDestino = caixaDaFila(destino);
        if (caixaDestino == null || !caixaDestino.estaAberto()) {
            return false;
        }
        if (!origem.transferirCliente(cliente, destino)) {
            return false;
        }
        cliente.definirCaixa(caixaDestino.getId());
        totalTransferencias++;
        return true;
    }

    public boolean transferirCliente(int caixaOrigemId, String senha, int caixaDestinoId) {
        Caixa origem = obterCaixa(caixaOrigemId);
        Caixa destino = obterCaixa(caixaDestinoId);
        if (origem == null || destino == null) {
            return false;
        }
        Cliente cliente = buscarClienteNaFila(origem, senha);
        if (cliente == null) {
            return false;
        }
        return transferirCliente(cliente, origem.obterFila(), destino.obterFila());
    }

    public Cliente buscarClienteNaFila(Caixa caixa, String senha) {
        Lista<Cliente> clientes = caixa.obterFila().listarClientes();
        for (int i = 0; i < clientes.tamanho(); i++) {
            if (clientes.obter(i).getSenha().equalsIgnoreCase(senha)) {
                return clientes.obter(i);
            }
        }
        return null;
    }

    private Caixa caixaDaFila(FilaAtendimento fila) {
        for (int i = 0; i < caixas.tamanho(); i++) {
            if (caixas.obter(i).obterFila() == fila) {
                return caixas.obter(i);
            }
        }
        return null;
    }

   
    public Cliente atenderProximo(int caixaId) {
        Caixa caixa = obterCaixa(caixaId);
        if (caixa == null || !caixa.estaAberto()) {
            return null;
        }
        if (caixa.getClienteEmAtendimento() != null) {
            caixa.concluirAtendimento();
        }
        Cliente proximo = caixa.chamarProximo();
        if (proximo == null) {
            return null;
        }
        RegistroAtendimento registro = new RegistroAtendimento(proximoIdRegistro++);
        registro.criarRegistro(proximo, caixa);
        caixa.registrarAtendimento(registro);
        historicoGeral.adicionar(registro);
        return proximo;
    }


    public int contarClientesAguardando() {
        int total = 0;
        for (int i = 0; i < caixas.tamanho(); i++) {
            total += caixas.obter(i).obterFila().contarClientesEspera();
        }
        return total;
    }

    public float calcularCargaMedia() {
        int abertos = obterCaixasAtivas().tamanho();
        if (abertos == 0) {
            return 0f;
        }
        return (float) contarClientesAguardando() / abertos;
    }

    public Lista<Caixa> getCaixas() {
        return caixas;
    }

    public Lista<RegistroAtendimento> getHistoricoGeral() {
        return historicoGeral;
    }

    public int getProximaSenhaSequencial() {
        return proximaSenhaSequencial;
    }

    public int getTotalClientesDirecionados() {
        return totalClientesDirecionados;
    }

    public int getTotalTransferencias() {
        return totalTransferencias;
    }
}
