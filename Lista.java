
public class Lista<T> implements Listavel<T> {

    private Object[] dados;
    private int tamanho;

    public Lista() {
        this(10);
    }

    public Lista(int capacidadeInicial) {
        dados = new Object[capacidadeInicial < 1 ? 1 : capacidadeInicial];
        tamanho = 0;
    }

    @Override
    public void adicionar(T elemento) {
        if (tamanho == dados.length) {
            aumentarCapacidade();
        }
        dados[tamanho] = elemento;
        tamanho++;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T obter(int indice) {
        validarIndice(indice);
        return (T) dados[indice];
    }

    @Override
    @SuppressWarnings("unchecked")
    public T remover(int indice) {
        validarIndice(indice);
        T removido = (T) dados[indice];
        for (int i = indice; i < tamanho - 1; i++) {
            dados[i] = dados[i + 1];
        }
        dados[tamanho - 1] = null;
        tamanho--;
        return removido;
    }

    @Override
    public boolean remover(T elemento) {
        int indice = indiceDe(elemento);
        if (indice < 0) {
            return false;
        }
        remover(indice);
        return true;
    }

    @Override
    public int indiceDe(T elemento) {
        for (int i = 0; i < tamanho; i++) {
            if (dados[i] == elemento || (elemento != null && elemento.equals(dados[i]))) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public boolean contem(T elemento) {
        return indiceDe(elemento) >= 0;
    }

    @Override
    public int tamanho() {
        return tamanho;
    }

    @Override
    public boolean estaVazia() {
        return tamanho == 0;
    }

    private void aumentarCapacidade() {
        Object[] novo = new Object[dados.length * 2];
        for (int i = 0; i < tamanho; i++) {
            novo[i] = dados[i];
        }
        dados = novo;
    }

    private void validarIndice(int indice) {
        if (indice < 0 || indice >= tamanho) {
            throw new IndexOutOfBoundsException("Indice invalido: " + indice + " (tamanho " + tamanho + ")");
        }
    }
}
