package br.com.supermercado.presentation.cli;

import br.com.supermercado.application.checkout.QueueService;
import br.com.supermercado.application.product.ProductService;
import br.com.supermercado.application.sales.Cart;
import br.com.supermercado.application.sales.SaleService;
import br.com.supermercado.domain.checkout.Checkout;
import br.com.supermercado.domain.checkout.Customer;
import br.com.supermercado.domain.product.Product;
import br.com.supermercado.domain.sales.Sale;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Menu do OPERADOR DE CAIXA: registrar vendas (carrinho, cupom) e consultar as vendas.
 * A venda pode ser ligada a um caixa; se houver cliente em atendimento nele, a senha
 * do cliente fica registrada no cupom.
 */
public final class CashierMenu {
    private final InputReader in;
    private final TerminalOutput out;
    private final SaleService sales;
    private final QueueService queues;
    private final ProductSelector selector;
    private final String cashier;

    public CashierMenu(InputReader in, TerminalOutput out, SaleService sales, ProductService products,
                       QueueService queues, String cashier) {
        this.in = in;
        this.out = out;
        this.sales = sales;
        this.queues = queues;
        this.selector = new ProductSelector(in, out, products);
        this.cashier = cashier;
    }

    public void run() {
        int option;
        do {
            option = in.readMenuOption("VENDAS (OPERADOR DE CAIXA)", "Registrar nova venda",
                    "Vendas de hoje", "Consultar venda pelo número");
            try {
                switch (option) {
                    case 1 -> newSale();
                    case 2 -> todaySales();
                    case 3 -> showSale();
                    default -> { }
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                out.error(e.getMessage());
            }
        } while (option != 0);
    }

    private void newSale() {
        out.title("Nova Venda");
        String checkoutId = null;
        String ticket = null;
        String typed = in.readLine("Número do caixa (vazio = sem caixa): ");
        if (!typed.isEmpty()) {
            Checkout checkout = queues.checkout(typed)
                    .orElseThrow(() -> new IllegalArgumentException("Caixa não encontrado: " + typed));
            checkoutId = checkout.id();
            Optional<Customer> inService = checkout.customerInService();
            if (inService.isPresent()) {
                ticket = inService.get().ticket();
                out.println("  Cliente em atendimento: " + inService.get());
            } else {
                out.warning("Nenhum cliente em atendimento no caixa " + checkoutId + ".");
            }
        }

        Cart cart = sales.newCart();
        while (true) {
            int option = in.readMenuWithZeroLabel("CARRINHO - " + TerminalOutput.money(cart.total()),
                    "Cancelar venda", "Adicionar item", "Desfazer último item",
                    "Remover produto do carrinho", "Ver carrinho", "Finalizar venda");
            try {
                switch (option) {
                    case 1 -> addItem(cart);
                    case 2 -> undoLast(cart);
                    case 3 -> removeProduct(cart);
                    case 4 -> showCart(cart);
                    case 5 -> {
                        if (finalizeSale(cart, checkoutId, ticket)) return;
                    }
                    default -> {
                        if (cart.isEmpty() || in.confirm("Descartar o carrinho e cancelar a venda?")) {
                            out.warning("Venda cancelada. O estoque não foi alterado.");
                            return;
                        }
                    }
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                out.error(e.getMessage());
            }
        }
    }

    private void addItem(Cart cart) {
        Product p = selector.select("Código ou nome do produto: ");
        if (p == null) return;
        int available = p.quantity() - cart.quantityOf(p.code());
        if (available <= 0) {
            out.error("Sem estoque disponível para '" + p.name() + "'.");
            return;
        }
        int quantity = in.readInt("Quantidade (1 a " + available + "): ", 1, available);
        Cart.Line line = cart.add(p.code(), quantity);
        out.success(quantity + " x " + line.productName() + " (" + TerminalOutput.money(line.unitPrice())
                + ") | total do carrinho: " + TerminalOutput.money(cart.total()));
    }

    private void undoLast(Cart cart) {
        Cart.Line undone = cart.undoLast();
        if (undone == null) {
            out.warning("O carrinho está vazio.");
        } else {
            out.success("Removido: " + undone.quantity() + " x " + undone.productName());
        }
    }

    private void removeProduct(Cart cart) {
        List<Cart.Line> lines = cart.lines();
        if (lines.isEmpty()) {
            out.warning("O carrinho está vazio.");
            return;
        }
        out.println(SaleViews.cart(cart));
        int n = in.readInt("Número do item a remover (0 = cancelar): ", 0, lines.size());
        if (n == 0) return;
        Cart.Line line = lines.get(n - 1);
        cart.remove(line.productCode());
        out.success("Removido do carrinho: " + line.productName());
    }

    private void showCart(Cart cart) {
        out.title("Carrinho");
        out.println(SaleViews.cart(cart));
    }

    private boolean finalizeSale(Cart cart, String checkoutId, String ticket) {
        if (cart.isEmpty()) {
            out.error("Adicione ao menos um item antes de finalizar.");
            return false;
        }
        showCart(cart);
        if (!in.confirm("Confirmar a venda de " + TerminalOutput.money(cart.total()) + "?")) {
            return false;
        }
        Sale sale = sales.finalizeSale(cart, cashier, checkoutId, ticket, LocalDateTime.now());
        out.success("Venda " + String.format("%06d", sale.id()) + " registrada. Estoque atualizado.");
        out.blank();
        out.println(SaleViews.receipt(sale, null));
        return true;
    }

    private void todaySales() {
        out.title("Vendas de Hoje");
        List<Sale> list = sales.listByDate(LocalDate.now());
        out.println(SaleViews.table(list, sales.summarize(list)));
    }

    private void showSale() {
        long id = in.readInt("Número da venda: ", 1, Integer.MAX_VALUE);
        Sale sale = sales.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Venda não encontrada: " + id));
        out.blank();
        out.println(SaleViews.receipt(sale, null));
    }
}
