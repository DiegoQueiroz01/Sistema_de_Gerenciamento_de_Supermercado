package br.com.supermercado.presentation.cli;

import br.com.supermercado.application.checkout.QueueService;
import br.com.supermercado.application.product.ProductService;
import br.com.supermercado.application.sales.Cart;
import br.com.supermercado.application.sales.SaleService;
import br.com.supermercado.application.store.SupermarketService;
import br.com.supermercado.domain.checkout.Checkout;
import br.com.supermercado.domain.checkout.Customer;
import br.com.supermercado.domain.product.Product;
import br.com.supermercado.domain.sales.Sale;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// Menu de VENDAS do operador de caixa: carrinho, cupom e consulta de vendas.
// Se a venda for feita num caixa com cliente em atendimento, a senha dele vai no cupom.
public class CashierMenu {
    private final InputReader in;
    private final TerminalOutput out;
    private final SaleService sales;
    private final QueueService queues;
    private final SupermarketService store;
    private final ProductSelector selector;
    private final String cashier;

    public CashierMenu(InputReader in, TerminalOutput out, SaleService sales, ProductService products,
                       QueueService queues, SupermarketService store, String cashier) {
        this.in = in;
        this.out = out;
        this.sales = sales;
        this.queues = queues;
        this.store = store;
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
                    case 1:
                        newSale();
                        break;
                    case 2:
                        todaySales();
                        break;
                    case 3:
                        showSale();
                        break;
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
            Checkout checkout = queues.checkout(typed);
            if (checkout == null) {
                out.error("Caixa não encontrado: " + typed);
                return;
            }
            checkoutId = checkout.id();
            Customer inService = checkout.customerInService();
            if (inService != null) {
                ticket = inService.ticket();
                out.println("  Cliente em atendimento: " + inService);
            } else {
                out.warning("Nenhum cliente em atendimento no caixa " + checkoutId + ".");
            }
        }

        Cart cart = sales.newCart();
        boolean finished = false;
        while (!finished) {
            int option = in.readMenuWithZeroLabel("CARRINHO - " + TerminalOutput.money(cart.total()),
                    "Cancelar venda", "Adicionar item", "Desfazer último item",
                    "Remover produto do carrinho", "Ver carrinho", "Finalizar venda");
            try {
                switch (option) {
                    case 1:
                        addItem(cart);
                        break;
                    case 2:
                        undoLast(cart);
                        break;
                    case 3:
                        removeProduct(cart);
                        break;
                    case 4:
                        showCart(cart);
                        break;
                    case 5:
                        finished = finalizeSale(cart, checkoutId, ticket);
                        break;
                    default: // 0 = cancelar
                        if (cart.isEmpty() || in.confirm("Descartar o carrinho e cancelar a venda?")) {
                            out.warning("Venda cancelada. O estoque não foi alterado.");
                            finished = true;
                        }
                        break;
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                out.error(e.getMessage());
            }
        }
    }

    private void addItem(Cart cart) {
        Product p = selector.select("Código ou nome do produto: ");
        if (p == null) {
            return;
        }
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
        if (n == 0) {
            return;
        }
        Cart.Line line = lines.get(n - 1);
        cart.remove(line.productCode());
        out.success("Removido do carrinho: " + line.productName());
    }

    private void showCart(Cart cart) {
        out.title("Carrinho");
        out.println(SaleViews.cart(cart));
    }

    // Devolve true se a venda foi registrada
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
        out.println(SaleViews.receipt(sale, store.receiptHeader()));
        return true;
    }

    private void todaySales() {
        out.title("Vendas de Hoje");
        List<Sale> list = sales.listByDate(LocalDate.now());
        out.println(SaleViews.table(list, sales.summarize(list)));
    }

    private void showSale() {
        long id = in.readInt("Número da venda: ", 1, Integer.MAX_VALUE);
        Sale sale = sales.findById(id);
        if (sale == null) {
            out.error("Venda não encontrada: " + id);
            return;
        }
        out.blank();
        out.println(SaleViews.receipt(sale, store.receiptHeader()));
    }
}
