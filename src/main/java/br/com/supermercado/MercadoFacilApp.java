package br.com.supermercado;

import br.com.supermercado.application.checkout.QueueService;
import br.com.supermercado.application.inventory.InventoryService;
import br.com.supermercado.application.product.ProductService;
import br.com.supermercado.application.reporting.DashboardService;
import br.com.supermercado.infrastructure.mysql.ConexaoMySQL;
import br.com.supermercado.infrastructure.mysql.MySQLProductRepository;
import br.com.supermercado.infrastructure.mysql.MySQLStockMovementRepository;
import br.com.supermercado.presentation.cli.ProductAdminMenu;
import br.com.supermercado.presentation.cli.QueueMenu;
import br.com.supermercado.presentation.cli.StockistMenu;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;

/** Aplicação persistente do Mercado Fácil. */
public final class MercadoFacilApp {
  private static final int CAIXAS_INICIAIS = 5;
  private MercadoFacilApp() {}
  public static void main(String[] args) {
    try (Connection ignored = ConexaoMySQL.abrir(); Scanner scanner = new Scanner(System.in)) {
      var repositorioProdutos = new MySQLProductRepository();
      var repositorioMovimentacoes = new MySQLStockMovementRepository();
      ProductService produtos = new ProductService(repositorioProdutos);
      InventoryService estoque = new InventoryService(repositorioProdutos, repositorioMovimentacoes, List::of);
      // Caixas e filas ficam em memória durante a sessão.
      QueueService filas = new QueueService();
      for (int i = 0; i < CAIXAS_INICIAIS; i++) filas.createCheckout();
      DashboardService painel = new DashboardService(filas);
      System.out.println("MERCADO FÁCIL — conectado ao MySQL");
      int opcao;
      do {
        System.out.println("\n=== MENU PRINCIPAL ===");
        System.out.println("1) Estoquista");
        System.out.println("2) Administrador — produtos e estoque");
        System.out.println("3) Caixas e filas");
        System.out.println("0) Sair");
        System.out.print("Escolha: ");
        String entrada = scanner.nextLine().trim();
        try { opcao = Integer.parseInt(entrada); } catch (NumberFormatException e) { opcao = -1; }
        if (opcao == 1) new StockistMenu(scanner, System.out, produtos, estoque, "Estoquista").run();
        else if (opcao == 2) new ProductAdminMenu(scanner, System.out, produtos, estoque, "Administrador").run();
        else if (opcao == 3) new QueueMenu(scanner, System.out, filas, painel).run();
        else if (opcao != 0) System.out.println("Opção inválida.");
      } while (opcao != 0);
      System.out.println("Sessão encerrada. Os dados permanecem no MySQL.");
    } catch (SQLException e) {
      System.err.println("Não foi possível conectar ao MySQL: " + e.getMessage());
      System.err.println("Confira se o serviço MySQL está ativo, se o banco foi criado e se as variáveis MERCADO_DB_USUARIO e MERCADO_DB_SENHA estão corretas.");
    }
  }
}
