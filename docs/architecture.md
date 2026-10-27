# Arquitetura do projeto

## Diretrizes

- Aplicação Java 17, organizada como projeto Maven.
- Uso exclusivamente pelo terminal; não há telas gráficas, navegador ou frontend.
- Separação entre apresentação, casos de uso, domínio, contratos de dados e
  infraestrutura.
- Os dados ficam em estruturas de dados em memória. O MySQL é opcional e cobre
  apenas produtos, categorias e movimentações de estoque.

## Estrutura

```text
.
├── docs/architecture.md
├── sql/                         scripts do MySQL (opcional)
├── src/main/java/br/com/supermercado/
│   ├── Main.java                ponto de entrada (memória ou --mysql)
│   ├── AppServices.java         liga serviços e repositórios
│   ├── DemoData.java            dados iniciais (produtos, usuários, caixas)
│   ├── application/
│   │   ├── auth/AuthService.java
│   │   ├── checkout/QueueService.java
│   │   ├── inventory/InventoryService.java
│   │   ├── product/ProductService.java
│   │   ├── reporting/DashboardService.java
│   │   ├── sales/{Cart, SaleService}.java
│   │   └── store/SupermarketService.java
│   ├── domain/
│   │   ├── checkout/{AttendanceQueue, Checkout, CheckoutStatus, Customer, ServiceRecord}.java
│   │   ├── inventory/{MovementType, StockMovement}.java
│   │   ├── product/{Category, Product}.java
│   │   ├── sales/{Sale, SaleItem}.java
│   │   ├── store/Supermarket.java
│   │   └── user/{User, UserRole}.java
│   ├── repositories/            contratos de acesso aos dados
│   ├── infrastructure/
│   │   ├── memory/              implementações em memória (estruturas de dados)
│   │   └── mysql/               produtos e estoque no MySQL
│   └── presentation/cli/
│       ├── CliApplication.java  tela inicial e login
│       ├── MenuRouter.java      abre o menu do perfil
│       ├── AdminMenu.java, UserAdminMenu.java
│       ├── StockistMenu.java, ProductAdminMenu.java, StockMenuBase.java
│       ├── CashierMenu.java, SaleViews.java
│       ├── QueueMenu.java
│       ├── ProductSelector.java
│       └── InputReader.java, TerminalOutput.java
└── src/test/java/br/com/supermercado/
    ├── E1Tests, E2Tests, SalesTests, UserTests, StoreTests, AdminTests
    ├── SuiteTest.java           executa todas as suítes no "mvn test"
    └── E1Demo, E2Demo           demonstrações isoladas dos módulos
```

## Responsabilidades

### Apresentação (terminal)

`Main` escolhe o armazenamento, carrega os dados iniciais e inicia a
`CliApplication`, que mostra a tela inicial e faz o login (até 3 tentativas).
O `MenuRouter` abre o menu do perfil do usuário; ao sair dele, a sessão é
encerrada e o programa volta para a tela inicial.

- **Administrador** (`AdminMenu`): visão geral, produtos/categorias/estoque,
  caixas e filas, relatórios de vendas, usuários, dados do supermercado,
  parâmetros e troca de senha.
- **Estoquista** (`StockistMenu`): consultas de produtos, entradas e saídas de
  estoque, alertas, itens parados, histórico e troca de senha.
- **Operador de caixa** (`CashierMenu` + `QueueMenu`): vendas (carrinho, cupom,
  vendas do dia), operação de caixas e filas e troca de senha.

`InputReader` concentra a leitura validada (números, decimais, datas, s/n,
menus) e `TerminalOutput` a escrita e a formatação (moeda, datas, tabelas).

### Aplicação

- `AuthService`: login, cadastro e edição de usuários, troca/redefinição de
  senha, ativação e desativação.
- `SupermarketService`: dados cadastrais do estabelecimento e cabeçalho do cupom.
- `ProductService`: cadastro sem código duplicado, pesquisa, edição, filtro,
  ordenação, categorias e desativação de produtos.
- `InventoryService`: entradas e saídas, histórico, alertas de estoque mínimo,
  itens sem movimentação, ranking de mais vendidos e baixa de estoque das vendas.
- `SaleService` e `Cart`: montagem da venda, finalização, consultas e resumo.
- `QueueService`: caixas, chegada de clientes, menor fila, prioridade,
  transferência, atendimento, fechamento com redistribuição e capacidade das filas.
- `DashboardService`: indicadores de atendimento e visão geral do supermercado.

### Domínio

Os modelos representam o supermercado, os usuários e seus perfis, produtos e
categorias, movimentações de estoque, vendas e itens, caixas, filas, clientes em
espera e registros de atendimento. `MovementType` distingue entrada de saída,
`CheckoutStatus` o estado de um caixa e `UserRole` o perfil de acesso.

### Dados e estruturas

As interfaces em `repositories` definem as operações de armazenamento; as
implementações em `infrastructure/memory` usam:

| Repositório / classe | Estrutura |
|----------------------|-----------|
| `InMemoryProductRepository` | mapa por código + mapa de desativados + lista de categorias |
| `InMemoryUserRepository` | mapa por login |
| `InMemoryCheckoutRepository` | mapa por número do caixa, na ordem de criação |
| `AttendanceQueue` (fila de cada caixa) | lista encadeada: FIFO e inserção prioritária |
| `InMemoryStockMovementRepository` | lista em ordem de registro |
| `InMemorySaleRepository` | lista em ordem de registro + mapa por número da venda |
| `InMemoryServiceRecordRepository` | lista em ordem de registro + mapa por caixa |
| `Cart` | mapa ordenado por código + pilha de inclusões (desfazer) |

Com `--mysql`, `MySQLProductRepository` e `MySQLStockMovementRepository`
substituem os repositórios de produtos e movimentações; o saldo de cada produto
é recalculado somando entradas e subtraindo saídas do histórico.

## Regras contempladas

### Produtos e estoque

- Código de produto único; busca por código/nome (sem diferenciar acentos e
  maiúsculas) e filtro por categoria.
- Edição de dados cadastrais e listagem por nome, preço ou quantidade.
- Quantidade só muda por movimentações; entradas e saídas registradas com data,
  produto, quantidade, tipo e responsável (usuário logado).
- Saída não pode ultrapassar o estoque disponível.
- Produtos abaixo do mínimo ou sem movimentação ficam consultáveis.
- Produto só é desativado com estoque zerado; o histórico é mantido.

### Vendas

- O carrinho confere o estoque a cada item e permite desfazer o último item.
- A finalização é tudo ou nada: baixa o estoque de todos os itens (uma saída por
  produto) e grava a venda; se faltar estoque em algum item, nada muda.
- O cupom guarda nome e preço do momento da venda e, se houver, o caixa e a
  senha do cliente em atendimento.
- O ranking de mais vendidos é calculado a partir dos itens das vendas.

### Caixas e filas

- Senha sequencial (S001) e horário de chegada para cada cliente.
- Direcionamento padrão à menor fila aberta com espaço; capacidade máxima por fila.
- Atendimento FIFO para clientes comuns e precedência para prioritários.
- Cliente não fica em mais de uma fila ao mesmo tempo.
- Fechar caixa com clientes aguardando os redistribui para caixas abertos; sem
  destino ou sem espaço, o fechamento é recusado.
- Cada atendimento gera um registro com caixa e horários para o cálculo da espera.

### Usuários

- Login único; senha guardada como hash SHA-256 com sal, nunca em texto.
- Usuário desativado não entra; ninguém desativa a própria conta.
- Sempre existe ao menos um administrador ativo.
