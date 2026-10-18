public interface Listavel<T> {
    void adicionar(T elemento);

    T obter(int indice);

    T remover(int indice);

    boolean remover(T elemento);

    int indiceDe(T elemento);

    boolean contem(T elemento);

    int tamanho();

    boolean estaVazia();
}
