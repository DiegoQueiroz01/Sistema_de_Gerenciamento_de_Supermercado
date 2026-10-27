package br.com.supermercado;

import br.com.supermercado.application.inventory.InventoryService;
import br.com.supermercado.application.product.ProductService;
import br.com.supermercado.domain.product.Category;
import br.com.supermercado.domain.user.UserRole;

import java.time.LocalDateTime;

// Dados de exemplo carregados quando o programa abre
public class DemoData {
    private static final int INITIAL_CHECKOUTS = 5;
    private static final int OPEN_CHECKOUTS = 3;

    private DemoData() {}

    public static void loadStore(AppServices app) {
        app.store().update("Mercado Fácil", "11.222.333/0001-81",
                "Av. Brasil, 1500 - Centro", "(11) 3333-4444");
    }

    // Um usuário de cada perfil (o admin é criado pelo AuthService)
    public static void loadUsers(AppServices app) {
        app.auth().ensureDefaultAdmin();
        app.auth().register("estoquista", "Carlos Oliveira", "estoque123", UserRole.STOCKIST);
        app.auth().register("caixa", "Marta Rocha", "caixa123", UserRole.CASHIER);
    }

    // Caixas de 1 a 5, com os três primeiros abertos
    public static void loadCheckouts(AppServices app) {
        for (int i = 0; i < INITIAL_CHECKOUTS; i++) {
            app.queues().createCheckout();
        }
        for (int i = 1; i <= OPEN_CHECKOUTS; i++) {
            app.queues().openCheckout(String.valueOf(i));
        }
    }

    // Categorias e produtos com estoque inicial
    public static void loadProducts(AppServices app) {
        ProductService products = app.products();
        InventoryService inventory = app.inventory();
        Category beverages = products.registerCategory("Bebidas");
        Category dairy = products.registerCategory("Laticínios");
        Category produce = products.registerCategory("Hortifruti");
        Category cleaning = products.registerCategory("Limpeza");
        Category grocery = products.registerCategory("Mercearia");

        LocalDateTime recent = LocalDateTime.now().minusDays(2);
        LocalDateTime old = LocalDateTime.now().minusDays(40);
        String who = "Carlos Oliveira";
        add(products, inventory, "78910001", "Refrigerante Coca-Cola 2L", beverages, 8.99, 45, 20, recent, who);
        add(products, inventory, "78910002", "Leite Integral Aurora 1L", dairy, 4.20, 30, 10, recent, who);
        add(products, inventory, "78910003", "Manteiga Aviação com Sal 200g", dairy, 9.50, 11, 12, recent, who);
        add(products, inventory, "78910004", "Detergente Ypê Coco 500ml", cleaning, 2.20, 18, 10, recent, who);
        add(products, inventory, "78910005", "Arroz Agulhinha Tipo 1 Tio João 5kg", grocery, 24.90, 25, 15, recent, who);
        add(products, inventory, "78910006", "Detergente Líquido Limpol 500ml", cleaning, 3.80, 8, 20, recent, who);
        add(products, inventory, "78910007", "Azeite de Oliva Extra Virgem 500ml", grocery, 39.90, 42, 10, old, who);
        add(products, inventory, "78910008", "Sabão em Pó Omo Multiação 1kg", cleaning, 14.50, 60, 10, old, who);
        add(products, inventory, "78910009", "Cerveja Heineken Lata 350ml", beverages, 5.49, 110, 24, old, who);
        add(products, inventory, "78910010", "Saco de Lixo Embalixo 50L", cleaning, 12.00, 25, 10, old, who);
        add(products, inventory, "78910011", "Feijão Carioca 1kg", grocery, 7.50, 100, 20, recent, who);
        add(products, inventory, "78910012", "Banana Prata (kg)", produce, 5.99, 40, 10, recent, who);
    }

    private static void add(ProductService products, InventoryService inventory, String code, String name,
                            Category category, double price, int quantity, int minimum,
                            LocalDateTime date, String who) {
        products.registerProduct(code, name, category, price, minimum, date);
        inventory.registerEntry(code, quantity, who, date);
    }
}
