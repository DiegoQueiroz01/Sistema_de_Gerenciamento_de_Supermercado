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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Casos de uso de estoque (E.1): entradas, saídas, histórico, alertas, itens parados e mais vendidos.
 *
 * PONTOS DE INTEGRAÇÃO COM O E.2: {@link #registerSaleExit}, {@link #hasStock} (E.2 chama o E.1)
 * e {@link SalesSource} (E.1 lê as vendas por esta interface). Este serviço não usa classes do E.2.
 */
public final class InventoryService {

    /** Resultado do ranking de mais vendidos. */
    public record TopSellingProduct(Product product, int unitsSold) { }

    /** Linha de venda como o E.1 enxerga: produto e quantidade (independe das classes de venda do E.2). */
    public record SoldItem(String productCode, int quantity) {
        public SoldItem {
            if (productCode == null || productCode.isBlank()) {
                throw new IllegalArgumentException("O código do produto do item é obrigatório.");
            }
            if (quantity <= 0) {
                throw new IllegalArgumentException("A quantidade do item deve ser maior que zero.");
            }
        }
    }

    /**
     * PONTO DE INTEGRAÇÃO (o E.1 lê, o E.2 implementa): fonte dos itens já vendidos, usada em "mais vendidos".
     * O E.2 cria uma implementação a partir do seu repositório de vendas e a passa ao construtor.
     */
    public interface SalesSource {
        List<SoldItem> findAllSoldItems();
    }

    private final ProductRepository products;
    private final StockMovementRepository movements;
    private final SalesSource soldItems;

    public InventoryService(ProductRepository products, StockMovementRepository movements,
                            SalesSource soldItems) {
        this.products = Objects.requireNonNull(products);
        this.movements = Objects.requireNonNull(movements);
        this.soldItems = Objects.requireNonNull(soldItems);
    }

    // ----------------------------------------------------------- movimentações
    public StockMovement registerEntry(String productCode, int quantity, String responsible) {
        return registerEntry(productCode, quantity, responsible, LocalDateTime.now());
    }

    public StockMovement registerEntry(String productCode, int quantity, String responsible,
                                       LocalDateTime when) {
        return register(requireProduct(productCode), quantity, MovementType.ENTRY, responsible, when);
    }

    public StockMovement registerExit(String productCode, int quantity, String responsible) {
        return register(requireProduct(productCode), quantity, MovementType.EXIT, responsible,
                LocalDateTime.now());
    }

    /** O estoque é atualizado primeiro (valida a regra); se falhar, nada é gravado no histórico. */
    private StockMovement register(Product product, int quantity, MovementType type,
                                   String responsible, LocalDateTime when) {
        product.updateStock(quantity, type);
        StockMovement movement = new StockMovement(movements.nextId(), product.code(), quantity,
                type, when, responsible == null || responsible.isBlank() ? "-" : responsible);
        movements.save(movement);
        return movement;
    }

    // ----------------------------------------------------------- integração com vendas (E.2)
    public boolean hasStock(String productCode, int quantity) {
        return quantity > 0 && products.findByCode(productCode)
                .map(p -> p.quantity() >= quantity).orElse(false);
    }

    /**
     * Dá baixa no estoque de todos os itens de uma venda e registra uma saída por produto.
     * Tudo ou nada: se qualquer item não tiver estoque, nada é alterado.
     * Não grava a venda em si (responsabilidade do E.2).
     */
    public void registerSaleExit(LocalDateTime saleDate, List<SoldItem> items, String responsible) {
        Objects.requireNonNull(saleDate, "A data da venda é obrigatória.");
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Uma venda precisa ter ao menos um item.");
        }
        Map<String, Integer> needed = new LinkedHashMap<>();
        for (SoldItem item : items) {
            needed.merge(item.productCode(), item.quantity(), Integer::sum);
        }
        for (Map.Entry<String, Integer> e : needed.entrySet()) {
            Product p = requireProduct(e.getKey());
            if (e.getValue() > p.quantity()) {
                throw new IllegalStateException("Quantidade indisponível em estoque para '" + p.name()
                        + "' (solicitado: " + e.getValue() + ", máx: " + p.quantity() + ").");
            }
        }
        for (Map.Entry<String, Integer> e : needed.entrySet()) {
            register(requireProduct(e.getKey()), e.getValue(), MovementType.EXIT, responsible, saleDate);
        }
    }

    // ----------------------------------------------------------- consultas
    /** Histórico de um produto, da mais recente para a mais antiga. */
    public List<StockMovement> historyOf(String productCode) {
        List<StockMovement> list = new ArrayList<>(movements.findByProductCode(productCode));
        Collections.reverse(list);
        return list;
    }

    public List<StockMovement> fullHistory() {
        List<StockMovement> list = new ArrayList<>(movements.findAll());
        Collections.reverse(list);
        return list;
    }

    /** Produtos abaixo do mínimo, do maior déficit para o menor. */
    public List<Product> listBelowMinimum() {
        List<Product> result = new ArrayList<>();
        for (Product p : products.findAll()) {
            if (p.isBelowMinimum()) {
                result.add(p);
            }
        }
        result.sort(Comparator.comparingInt((Product p) -> p.quantity() - p.minimumStock()));
        return result;
    }

    /**
     * Produtos sem entrada/saída nos últimos {@code days} dias. Referência: última movimentação
     * ou, se nunca houve, a data de cadastro. Os parados há mais tempo vêm primeiro.
     */
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
        result.sort(Comparator.comparing(this::lastActivity));
        return result;
    }

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

    /** Soma as quantidades vendidas (via SalesSource) e devolve o ranking. */
    public List<TopSellingProduct> topSelling(int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("O limite do ranking deve ser maior que zero.");
        }
        Map<String, Integer> totals = new LinkedHashMap<>();
        for (SoldItem item : soldItems.findAllSoldItems()) {
            totals.merge(item.productCode(), item.quantity(), Integer::sum);
        }
        List<TopSellingProduct> ranking = new ArrayList<>();
        totals.forEach((code, units) -> products.findByCode(code)
                .ifPresent(p -> ranking.add(new TopSellingProduct(p, units))));
        ranking.sort(Comparator.comparingInt(TopSellingProduct::unitsSold).reversed()
                .thenComparing(t -> ProductService.normalize(t.product().name())));
        return ranking.size() > limit ? new ArrayList<>(ranking.subList(0, limit)) : ranking;
    }

    private Product requireProduct(String code) {
        return products.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado: " + code));
    }
}
