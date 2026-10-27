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

// Vendas: montar o carrinho, finalizar (baixa no estoque + registro) e consultar.
public class SaleService {

    // Totais de um grupo de vendas (por exemplo, as de hoje)
    public static class SalesSummary {
        private final int sales;
        private final int units;
        private final double revenue;

        public SalesSummary(int sales, int units, double revenue) {
            this.sales = sales;
            this.units = units;
            this.revenue = revenue;
        }

        public int sales() { return sales; }
        public int units() { return units; }
        public double revenue() { return revenue; }

        public double averageTicket() {
            if (sales == 0) {
                return 0;
            }
            return revenue / sales;
        }
    }

    // Entrega ao estoque a lista de tudo que já foi vendido (para o ranking de mais vendidos)
    public static class SalesHistory implements SalesSource {
        private final SaleRepository sales;

        public SalesHistory(SaleRepository sales) {
            this.sales = sales;
        }

        @Override
        public List<SoldItem> findAllSoldItems() {
            List<SoldItem> items = new ArrayList<>();
            for (Sale sale : sales.findAll()) {
                for (SaleItem item : sale.items()) {
                    items.add(new SoldItem(item.productCode(), item.quantity()));
                }
            }
            return items;
        }
    }

    private final SaleRepository sales;
    private final ProductService products;
    private final InventoryService inventory;

    public SaleService(SaleRepository sales, ProductService products, InventoryService inventory) {
        this.sales = sales;
        this.products = products;
        this.inventory = inventory;
    }

    // O InventoryService precisa disso antes do SaleService existir, por isso é static
    public static SalesSource salesSource(SaleRepository sales) {
        return new SalesHistory(sales);
    }

    public Cart newCart() {
        return new Cart(products, inventory);
    }

    // Finaliza a venda: baixa o estoque e grava a venda.
    // Se faltar estoque de algum item, nada é alterado.
    public Sale finalizeSale(Cart cart, String cashier, String checkoutId, String customerTicket,
                             LocalDateTime when) {
        if (cart == null) {
            throw new IllegalArgumentException("O carrinho é obrigatório.");
        }
        if (when == null) {
            throw new IllegalArgumentException("A data da venda é obrigatória.");
        }
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

    // Devolve null se não encontrar
    public Sale findById(long id) {
        return sales.findById(id);
    }

    // Da venda mais recente para a mais antiga
    public List<Sale> listAll() {
        List<Sale> list = sales.findAll();
        Collections.reverse(list);
        return list;
    }

    // Vendas de um dia, da mais recente para a mais antiga
    public List<Sale> listByDate(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("A data é obrigatória.");
        }
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
