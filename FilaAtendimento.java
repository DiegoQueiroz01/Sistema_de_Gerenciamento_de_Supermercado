
public class FilaAtendimento implements Enfileiravel<Cliente> {

    private final int id;
    private final String tipo;
    private final int capacidadeMaxima;
    private final Cliente[] dados;
    private int inicio;
    private int quantidadeClientes;
    private int clientesPrioritarios;

    public FilaAtendimento(int id, String tipo, int capacidadeMaxima) {
        this.id = id;
        this.tipo = tipo;
        this.capacidadeMaxima = capacidadeMaxima < 1 ? 1 : capacidadeMaxima;
        this.dados = new Cliente[this.capacidadeMaxima];
        this.inicio = 0;
        this.quantidadeClientes = 0;
        this.clientesPrioritarios = 0;
    }


    public boolean adicionarCliente(Cliente cliente) {
        if (cliente == null || estaCheia() || contem(cliente)) {
            return false;
        }
        if (cliente.isPrioritario()) {
            inserirNaPosicao(clientesPrioritarios, cliente);
            clientesPrioritarios++;
        } else {
            inserirNaPosicao(quantidadeClientes, cliente);
        }
        return true;
    }

    public Cliente removerProximoCliente() {
        if (estaVazia()) {
            return null;
        }
        Cliente removido = dados[inicio];
        dados[inicio] = null;
        inicio = (inicio + 1) % capacidadeMaxima;
        quantidadeClientes--;
        if (removido.isPrioritario()) {
            clientesPrioritarios--;
        }
        return removido;
    }

    public Cliente obterProximoCliente() {
        if (estaVazia()) {
            return null;
        }
        return dados[inicio];
    }

    
    public boolean transferirCliente(Cliente cliente, FilaAtendimento filaDestino) {
        if (cliente == null || filaDestino == null || filaDestino == this) {
            return false;
        }
        if (!contem(cliente) || filaDestino.estaCheia() || filaDestino.contem(cliente)) {
            return false;
        }
        removerCliente(cliente);
        filaDestino.adicionarCliente(cliente);
        filaDestino.ordenarPorFIFO();
        return true;
    }

    
    public void ordenarPorFIFO() {
        ordenarBloco(0, clientesPrioritarios);
        ordenarBloco(clientesPrioritarios, quantidadeClientes);
    }

    public int contarClientesEspera() {
        return quantidadeClientes;
    }

    public boolean ehPrioritaria() {
        return clientesPrioritarios > 0;
    }

    public Lista<Cliente> listarClientes() {
        Lista<Cliente> copia = new Lista<>();
        for (int i = 0; i < quantidadeClientes; i++) {
            copia.adicionar(dados[posicao(i)]);
        }
        return copia;
    }


    public boolean contem(Cliente cliente) {
        return indiceDe(cliente) >= 0;
    }

    public boolean removerCliente(Cliente cliente) {
        int indice = indiceDe(cliente);
        if (indice < 0) {
            return false;
        }
        boolean eraPrioritario = dados[posicao(indice)].isPrioritario();
        for (int i = indice; i < quantidadeClientes - 1; i++) {
            dados[posicao(i)] = dados[posicao(i + 1)];
        }
        dados[posicao(quantidadeClientes - 1)] = null;
        quantidadeClientes--;
        if (eraPrioritario) {
            clientesPrioritarios--;
        }
        return true;
    }

    private int posicao(int indiceLogico) {
        return (inicio + indiceLogico) % capacidadeMaxima;
    }

    private int indiceDe(Cliente cliente) {
        for (int i = 0; i < quantidadeClientes; i++) {
            if (dados[posicao(i)] == cliente) {
                return i;
            }
        }
        return -1;
    }

    private void inserirNaPosicao(int indiceLogico, Cliente cliente) {
        for (int i = quantidadeClientes; i > indiceLogico; i--) {
            dados[posicao(i)] = dados[posicao(i - 1)];
        }
        dados[posicao(indiceLogico)] = cliente;
        quantidadeClientes++;
    }

    private void ordenarBloco(int de, int ate) {
        for (int i = de + 1; i < ate; i++) {
            Cliente atual = dados[posicao(i)];
            int j = i - 1;
            while (j >= de && dados[posicao(j)].getDataHoraEntrada()
                    .isAfter(atual.getDataHoraEntrada())) {
                dados[posicao(j + 1)] = dados[posicao(j)];
                j--;
            }
            dados[posicao(j + 1)] = atual;
        }
    }

    

    @Override
    public boolean enfileirar(Cliente cliente) {
        return adicionarCliente(cliente);
    }

    @Override
    public Cliente desenfileirar() {
        return removerProximoCliente();
    }

    @Override
    public Cliente primeiro() {
        return obterProximoCliente();
    }

    @Override
    public int tamanho() {
        return quantidadeClientes;
    }

    @Override
    public boolean estaVazia() {
        return quantidadeClientes == 0;
    }

    @Override
    public boolean estaCheia() {
        return quantidadeClientes == capacidadeMaxima;
    }


    public int getId() {
        return id;
    }

    public String getTipo() {
        return tipo;
    }

    public int getCapacidadeMaxima() {
        return capacidadeMaxima;
    }

    public int getQuantidadeClientes() {
        return quantidadeClientes;
    }

    public int getClientesPrioritarios() {
        return clientesPrioritarios;
    }
}
