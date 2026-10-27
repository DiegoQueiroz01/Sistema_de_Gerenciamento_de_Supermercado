package br.com.supermercado.application.inventory;

import br.com.supermercado.application.product.ProductService;
import br.com.supermercado.domain.inventory.MovementType;
import br.com.supermercado.domain.inventory.StockMovement;
import br.com.supermercado.domain.product.Product;
import br.com.supermercado.repositories.ProductRepository;
import br.com.supermercado.repositories.StockMovementRepository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Operações de estoque (E.1): entradas, saídas, histórico, alertas,
// produtos parados e mais vendidos.
// Ligação com as vendas (E.2): registerSaleExit e hasStock são chamados pelas vendas,
// e as vendas são lidas pela interface SalesSource.
public class InventoryService {

    // Uma linha do ranking de mais vendidos
    public static class TopSellingProduct {
        private final Product product;
        private final int unitsSold;

        public TopSellingProduct(Product product, int unitsSold) {
            this.product = product;
            this.unitsSold = unitsSold;
        }

        public Product product() { return product; }
        public int unitsSold() { return unitsSold; }
    }

    // Item vendido: só o código do produto e a quantidade
    public static class SoldItem {
        private final String productCode;
        private final int quantity;

        public SoldItem(String productCode, int quantity) {
            if (productCode == null || productCode.isBlank()) {
                throw new IllegalArgumentException("O código do produto do item é obrigatório.");
            }
            if (quantity <= 0) {
                throw new IllegalArgumentException("A quantidade do item deve ser maior que zero.");
            }
            this.productCode = productCode;
            this.quantity = quantity;
        }

        public String productCode() { return productCode; }
        public int quantity() { return quantity; }
    }

    // De onde vêm as vendas para montar o ranking. Quem implementa é o módulo de vendas.
    public interface SalesSource {
        List<SoldItem> findAllSoldItems();
    }

    private final ProductRepository products;
    private final StockMovementRepository movements;
    private final SalesSource salesSource;

    public InventoryService(ProductRepository products, StockMovementRepository movements,
                            SalesSource salesSource) {
        this.products = products;
        this.movements = movements;
        this.salesSource = salesSource;
    }

    // ---------- entradas e saídas ----------

    public StockMovement registerEntry(String productCode, int quantity, String responsible) {
        return registerEntry(productCode, quantity, responsible, LocalDateTime.now());
    }

    public StockMovement registerEntry(String productCode, int quantity, String responsible, LocalDateTime when) {
        return register(getProduct(productCode), quantity, MovementType.ENTRY, responsible, when);
    }

    public StockMovement registerExit(String productCode, int quantity, String responsible) {
        return register(getProduct(productCode), quantity, MovementType.EXIT, responsible, LocalDateTime.now());
    }

    // Primeiro atualiza o estoque (que confere as regras). Se der erro, nada vai para o histórico.
    private StockMovement register(Product product, int quantity, MovementType type,
                                   String responsible, LocalDateTime when) {
        product.updateStock(quantity, type);
        if (responsible == null || responsible.isBlank()) {
            responsible = "-";
        }
        StockMovement movement = new StockMovement(movements.nextId(), product.code(), quantity,
                type, when, responsible);
        movements.save(movement);
        return movement;
    }

    // ---------- usado pelas vendas ----------

    public boolean hasStock(String productCode, int quantity) {
        if (quantity <= 0) {
            return false;
        }
        Product p = products.findByCode(productCode);
        return p != null && p.quantity() >= quantity;
    }

    // Dá baixa no estoque de todos os itens de uma venda.
    // É tudo ou nada: se faltar estoque de um item, nenhum estoque é alterado.
    public void registerSaleExit(LocalDateTime saleDate, List<SoldItem> items, String responsible) {
        if (saleDate == null) {
            throw new IllegalArgumentException("A data da venda é obrigatória.");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Uma venda precisa ter ao menos um item.");
        }

        // soma as quantidades do mesmo produto
        Map<String, Integer> needed = new LinkedHashMap<>();
        for (SoldItem item : items) {
            Integer current = needed.get(item.productCode());
            if (current == null) {
                current = 0;
            }
            needed.put(item.productCode(), current + item.quantity());
        }

        // primeiro confere tudo...
        for (String code : needed.keySet()) {
            Product p = getProduct(code);
            int quantity = needed.get(code);
            if (quantity > p.quantity()) {
                throw new IllegalStateException("Quantidade indisponível em estoque para '" + p.name()
                        + "' (solicitado: " + quantity + ", máx: " + p.quantity() + ").");
            }
        }
        // ...depois dá baixa
        for (String code : needed.keySet()) {
            register(getProduct(code), needed.get(code), MovementType.EXIT, responsible, saleDate);
        }
    }

    // ---------- consultas ----------

    // Histórico de um produto, do mais recente para o mais antigo
    public List<StockMovement> historyOf(String productCode) {
        List<StockMovement> list = movements.findByProductCode(productCode);
        Collections.reverse(list);
        return list;
    }

    public List<StockMovement> fullHistory() {
        List<StockMovement> list = movements.findAll();
        Collections.reverse(list);
        return list;
    }

    // Produtos abaixo do mínimo, do que falta mais para o que falta menos
    public List<Product> listBelowMinimum() {
        List<Product> result = new ArrayList<>();
        for (Product p : products.findAll()) {
            if (p.isBelowMinimum()) {
                result.add(p);
            }
        }
        result.sort((a, b) -> (a.quantity() - a.minimumStock()) - (b.quantity() - b.minimumStock()));
        return result;
    }

    // Produtos sem entrada nem saída nos últimos "days" dias.
    // Os que estão parados há mais tempo aparecem primeiro.
    public List<Product> listWithoutMovement(int days) {
        if (days <= 0) {
            throw new IllegalArgumentException("O período deve ser de pelo menos 1 dia.");
        }
        LocalDateTime limit = LocalDateTime.now().minusDays(days);
        List<Product> result = new ArrayList<>();
        for (Product p : products.findAll()) {
            if (lastActivity(p).isBefore(limit)) {
                result.add(p);
            }
        }
        result.sort((a, b) -> lastActivity(a).compareTo(lastActivity(b)));
        return result;
    }

    // Data da última movimentação (ou a data de cadastro, se nunca houve)
    public LocalDateTime lastActivity(Product product) {
        LocalDateTime last = product.registeredAt();
        for (StockMovement m : movements.findByProductCode(product.code())) {
            if (m.timestamp().isAfter(last)) {
                last = m.timestamp();
            }
        }
        return last;
    }

    public long daysWithoutMovement(Product product) {
        return ChronoUnit.DAYS.between(lastActivity(product), LocalDateTime.now());
    }

    // Soma o que foi vendido de cada produto e devolve os "limit" primeiros
    public List<TopSellingProduct> topSelling(int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("O limite do ranking deve ser maior que zero.");
        }
        Map<String, Integer> totals = new LinkedHashMap<>();
        for (SoldItem item : salesSource.findAllSoldItems()) {
            Integer current = totals.get(item.productCode());
            if (current == null) {
                current = 0;
            }
            totals.put(item.productCode(), current + item.quantity());
        }

        List<TopSellingProduct> ranking = new ArrayList<>();
        for (String code : totals.keySet()) {
            Product p = products.findByCode(code);
            if (p != null) { // produtos desativados ficam fora do ranking
                ranking.add(new TopSellingProduct(p, totals.get(code)));
            }
        }
        // mais vendido primeiro; empate fica em ordem alfabética
        ranking.sort((a, b) -> {
            if (a.unitsSold() != b.unitsSold()) {
                return b.unitsSold() - a.unitsSold();
            }
            return ProductService.normalize(a.product().name())
                    .compareTo(ProductService.normalize(b.product().name()));
        });

        List<TopSellingProduct> result = new ArrayList<>();
        for (int i = 0; i < ranking.size() && i < limit; i++) {
            result.add(ranking.get(i));
        }
        return result;
    }

    private Product getProduct(String code) {
        Product p = products.findByCode(code);
        if (p == null) {
            throw new IllegalArgumentException("Produto não encontrado: " + code);
        }
        return p;
    }
}
