package br.com.supermercado.application.sales;

import br.com.supermercado.application.inventory.InventoryService;
import br.com.supermercado.application.inventory.InventoryService.SalesSource;
import br.com.supermercado.application.inventory.InventoryService.SoldItem;
import br.com.supermercado.application.product.ProductService;
import br.com.supermercado.domain.sales.Sale;
import br.com.supermercado.domain.sales.SaleItem;
import br.com.supermercado.repositories.SaleRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Casos de uso de vendas: montar o carrinho, finalizar a venda (baixa de estoque + registro)
 * e consultar o histórico. A baixa de estoque é feita pelo InventoryService, que registra
 * uma saída por produto.
 */
public final class SaleService {

    /** Resumo de um conjunto de vendas (por exemplo, as do dia). */
    public record SalesSummary(int sales, int units, double revenue) {
        public double averageTicket() {
            return sales == 0 ? 0 : revenue / sales;
        }
    }

    private final SaleRepository sales;
    private final ProductService products;
    private final InventoryService inventory;

    public SaleService(SaleRepository sales, ProductService products, InventoryService inventory) {
        this.sales = Objects.requireNonNull(sales);
        this.products = Objects.requireNonNull(products);
        this.inventory = Objects.requireNonNull(inventory);
    }

    /**
     * Fonte de itens vendidos para o ranking de mais vendidos do InventoryService.
     * É estática porque o InventoryService precisa dela antes de o SaleService existir.
     */
    public static SalesSource salesSource(SaleRepository sales) {
        Objects.requireNonNull(sales);
        return () -> {
            List<SoldItem> items = new ArrayList<>();
            for (Sale sale : sales.findAll()) {
                for (SaleItem item : sale.items()) {
                    items.add(new SoldItem(item.productCode(), item.quantity()));
                }
            }
            return items;
        };
    }

    public Cart newCart() {
        return new Cart(products, inventory);
    }

    /**
     * Finaliza a venda: dá baixa no estoque (tudo ou nada) e grava a venda.
     * Se algum item não tiver mais estoque, nada é alterado e a venda não é gravada.
     */
    public Sale finalizeSale(Cart cart, String cashier, String checkoutId, String customerTicket,
                             LocalDateTime when) {
        Objects.requireNonNull(cart, "O carrinho é obrigatório.");
        Objects.requireNonNull(when, "A data da venda é obrigatória.");
        if (cart.isEmpty()) {
            throw new IllegalArgumentException("Uma venda precisa ter ao menos um item.");
        }
        List<SaleItem> items = new ArrayList<>();
        List<SoldItem> soldItems = new ArrayList<>();
        for (Cart.Line line : cart.lines()) {
            items.add(new SaleItem(line.productCode(), line.productName(), line.quantity(), line.unitPrice()));
            soldItems.add(new SoldItem(line.productCode(), line.quantity()));
        }
        inventory.registerSaleExit(when, soldItems, cashier);
        Sale sale = new Sale(sales.nextId(), when, cashier, checkoutId, customerTicket, items);
        sales.save(sale);
        return sale;
    }

    public Optional<Sale> findById(long id) {
        return sales.findById(id);
    }

    /** Todas as vendas, da mais recente para a mais antiga. */
    public List<Sale> listAll() {
        List<Sale> list = new ArrayList<>(sales.findAll());
        Collections.reverse(list);
        return list;
    }

    /** Vendas de um dia, da mais recente para a mais antiga. */
    public List<Sale> listByDate(LocalDate date) {
        Objects.requireNonNull(date, "A data é obrigatória.");
        List<Sale> result = new ArrayList<>();
        for (Sale sale : listAll()) {
            if (sale.dateTime().toLocalDate().equals(date)) {
                result.add(sale);
            }
        }
        return result;
    }

    public SalesSummary summarize(List<Sale> list) {
        int units = 0;
        double revenue = 0;
        for (Sale sale : list) {
            units += sale.totalUnits();
            revenue += sale.total();
        }
        return new SalesSummary(list.size(), units, revenue);
    }
}
