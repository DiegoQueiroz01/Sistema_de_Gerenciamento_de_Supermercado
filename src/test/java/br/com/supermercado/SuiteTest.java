package br.com.supermercado;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Executa todas as suítes de teste pelo "mvn test". Cada suíte imprime os casos
 * (PASSOU/FALHOU) e devolve o número de falhas; qualquer falha reprova o build.
 */
class SuiteTest {

    @Test
    void produtosEEstoque() {
        assertEquals(0, E1Tests.run(), "Há casos com falha em E1Tests (veja a saída acima).");
    }

    @Test
    void caixasEFilas() {
        assertEquals(0, E2Tests.run(), "Há casos com falha em E2Tests (veja a saída acima).");
    }

    @Test
    void vendas() {
        assertEquals(0, SalesTests.run(), "Há casos com falha em SalesTests (veja a saída acima).");
    }

    @Test
    void usuarios() {
        assertEquals(0, UserTests.run(), "Há casos com falha em UserTests (veja a saída acima).");
    }

    @Test
    void supermercado() {
        assertEquals(0, StoreTests.run(), "Há casos com falha em StoreTests (veja a saída acima).");
    }

    @Test
    void administracao() {
        assertEquals(0, AdminTests.run(), "Há casos com falha em AdminTests (veja a saída acima).");
    }
}
