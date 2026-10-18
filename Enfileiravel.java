public interface Enfileiravel<T> {
    boolean enfileirar(T elemento);

    T desenfileirar();

    T primeiro();

    int tamanho();

    boolean estaVazia();

    boolean estaCheia();
}
