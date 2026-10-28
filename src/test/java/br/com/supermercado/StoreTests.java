package br.com.supermercado;

import br.com.supermercado.application.store.SupermarketService;
import br.com.supermercado.domain.store.Supermarket;
import br.com.supermercado.infrastructure.memory.InMemorySupermarketRepository;

/**
 * Testes dos dados do supermercado sem dependências externas.
 * Execução: mvn test-compile exec:java -Dexec.mainClass=br.com.supermercado.StoreTests -Dexec.classpathScope=test
 */
public class StoreTests {
    static int passed = 0, failed = 0;

    interface Action { void run() throws Exception; }

    static void check(String name, boolean condition) {
        if (condition) { passed++; System.out.println("PASSOU  " + name); }
        else { failed++; System.out.println("FALHOU  " + name); }
    }

    static boolean throwsType(Class<? extends Exception> type, Action action) {
        try { action.run(); return false; } catch (Exception e) { return type.isInstance(e); }
    }

    static int run() {
        passed = 0;
        failed = 0;
        SupermarketService service = new SupermarketService(new InMemorySupermarketRepository());

        check("CS01 sem cadastro usa o nome padrão",
                service.current().name().equals(SupermarketService.DEFAULT_NAME));
        check("CS02 CNPJ válido aceito", Supermarket.isValidCnpj("11222333000181"));
        check("CS03 dígito verificador errado recusado", !Supermarket.isValidCnpj("11222333000182"));
        check("CS04 dígitos repetidos recusados", !Supermarket.isValidCnpj("11111111111111"));
        check("CS05 tamanho errado recusado", !Supermarket.isValidCnpj("1122233300018"));

        Supermarket s = service.update(" Super Bom ", "11.222.333/0001-81", "Rua A, 10", "(11) 9999-0000");
        check("CS06 CNPJ guardado só com números e formatado na exibição",
                s.cnpj().equals("11222333000181") && s.formattedCnpj().equals("11.222.333/0001-81"));
        check("CS07 atualização substitui os dados", service.current().name().equals("Super Bom"));
        check("CS08 CNPJ inválido recusado sem alterar os dados", throwsType(IllegalArgumentException.class,
                () -> service.update("Outro", "12.345.678/0001-00", "", ""))
                && service.current().name().equals("Super Bom"));
        check("CS09 nome vazio recusado", throwsType(IllegalArgumentException.class,
                () -> service.update(" ", "", "", "")));
        check("CS10 CNPJ é opcional", service.update("Sem CNPJ", "", "", "").formattedCnpj().equals("-"));
        service.update("Super Bom", "11222333000181", "Rua A, 10", "");
        String header = service.receiptHeader();
        check("CS11 cabeçalho do cupom", header.startsWith("SUPER BOM") && header.contains("CNPJ: 11.222.333/0001-81")
                && header.contains("Rua A, 10") && !header.contains("Tel."));

        System.out.println("\nResultado: " + passed + " passaram, " + failed + " falharam.");
        return failed;
    }

    public static void main(String[] args) {
        System.exit(run() == 0 ? 0 : 1);
    }
}
