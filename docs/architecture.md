# Arquitetura do projeto

## Diretrizes

- Aplicação Java 17, organizada como projeto Maven.
- Uso pelo terminal; não há telas gráficas, navegador ou frontend.
- Separação entre apresentação, casos de uso, domínio, contratos de dados e
  infraestrutura.

## Estrutura

```text
.
├── docs/
│   └── architecture.md
├── src/
│   ├── main/java/br/com/supermercado/
│   │   ├── Main.java
│   │   ├── application/
│   │   │   ├── auth/AuthService.java
│   │   │   ├── checkout/QueueService.java
│   │   │   ├── inventory/InventoryService.java
│   │   │   ├── product/ProductService.java
│   │   │   ├── reporting/DashboardService.java
│   │   │   ├── sales/SaleService.java
│   │   │   └── store/SupermarketService.java
│   │   ├── domain/
│   │   │   ├── checkout/{AttendanceQueue, Checkout, CheckoutStatus, Customer, ServiceRecord}.java
│   │   │   ├── inventory/{MovementType, StockMovement}.java
│   │   │   ├── product/{Category, Product}.java
│   │   │   ├── sales/{Sale, SaleItem}.java
│   │   │   ├── store/Supermarket.java
│   │   │   └── user/{User, UserRole}.java
│   │   ├── infrastructure/memory/
│   │   │   ├── InMemoryCheckoutRepository.java
│   │   │   ├── InMemoryProductRepository.java
│   │   │   ├── InMemorySaleRepository.java
│   │   │   ├── InMemoryServiceRecordRepository.java
│   │   │   ├── InMemoryStockMovementRepository.java
│   │   │   ├── InMemorySupermarketRepository.java
│   │   │   └── InMemoryUserRepository.java
│   │   ├── presentation/cli/
│   │   │   ├── AdminMenu.java
│   │   │   ├── CashierMenu.java
│   │   │   ├── CliApplication.java
│   │   │   ├── InputReader.java
│   │   │   ├── MenuRouter.java
│   │   │   ├── StockistMenu.java
│   │   │   └── TerminalOutput.java
│   │   └── repositories/
│   │       ├── CheckoutRepository.java
│   │       ├── ProductRepository.java
│   │       ├── SaleRepository.java
│   │       ├── ServiceRecordRepository.java
│   │       ├── StockMovementRepository.java
│   │       ├── SupermarketRepository.java
│   │       └── UserRepository.java
├── .gitignore
├── pom.xml
└── README.md
```

## Responsabilidades

### Apresentação CLI

`Main` inicia a aplicação; `CliApplication` controla o ciclo de interação.
`MenuRouter` direciona o usuário autenticado ao menu correspondente ao seu
perfil. Os menus de administrador, estoquista e operador de caixa recebem
comandos em texto. `InputReader` e `TerminalOutput` concentram, respectivamente,
a leitura e a apresentação no terminal.

### Aplicação

Os serviços coordenam ações e validações dos casos de uso sem depender dos
menus:

- `AuthService`: autenticação e consulta do perfil autorizado.
- `ProductService`: cadastro sem código duplicado, pesquisa, edição, filtro e
  ordenação de produtos.
- `SupermarketService`: dados cadastrais do estabelecimento.
- `InventoryService`: entrada/saída, consulta do histórico, alertas de estoque
  mínimo e identificação de itens sem movimentação.
- `SaleService`: composição de vendas e itens; ao concluir uma venda, solicita a
  baixa de estoque e o registro da movimentação correspondente.
- `QueueService`: abertura/fechamento de caixas, distribuição para a menor fila,
  prioridade, transferência de clientes e conclusão do atendimento.
- `DashboardService`: métricas de estoque e atendimento, incluindo mais
  vendidos, caixas abertos, filas, atendimentos e tempo médio de espera.

O `QueueService` e as entidades de caixa já implementam a abertura/fechamento,
a distribuição de chegadas para a menor fila, a transferência manual para o
final de outra fila e o registro do tempo entre chegada e início do atendimento.
Ao fechar um caixa com clientes aguardando, eles são redistribuídos para caixas
abertos; se não houver destino, o fechamento é recusado.

### Domínio

Os modelos representam o supermercado; usuários e seus perfis; produtos e
categorias; entradas e saídas de estoque; vendas e itens; caixas, filas, clientes
em espera e registros de atendimento. `MovementType` distingue entrada de
saída, `CheckoutStatus` representa o estado de um caixa e `UserRole` representa
administrador, estoquista e operador de caixa.

### Dados e estruturas

As interfaces em `repositories` definem as operações de armazenamento. As
implementações em `infrastructure/memory` manterão os dados em memória. A
estrutura prevista segue os relatórios:

- mapa por código para pesquisa rápida e prevenção de produtos duplicados;
- coleções de produtos, movimentações, vendas e registros para listagem,
  filtragem, ordenação e histórico;
- uma fila por caixa, mantendo FIFO para clientes comuns e inserção prioritária
  no início;
- coleção de caixas para localizar a menor fila e consolidar métricas.

O armazenamento de dados e as estruturas concretas ainda serão implementados.

## Regras contempladas

### Produtos e estoque

- Código de produto único; busca por código/nome e filtro por categoria.
- Edição de dados cadastrais e listagem por nome, preço ou quantidade.
- Entradas e saídas registradas com data, produto, quantidade e tipo.
- Saída não pode ultrapassar o estoque disponível.
- Produtos abaixo do mínimo ou sem movimentação ficam consultáveis.
- Relatório de mais vendidos baseado nos itens das vendas.

### Vendas

Cada venda agrupa um ou mais itens. Sua finalização atualiza o estoque e registra
uma saída para cada produto, mantendo o histórico consistente.

### Caixas e filas

- Senha/identificador sequencial e horário de chegada para cada cliente.
- Direcionamento padrão à menor fila disponível.
- Atendimento FIFO para clientes comuns e precedência para prioritários.
- Cliente não deve estar em mais de uma fila ao mesmo tempo.
- Fechar caixa com clientes pendentes exige transferi-los para caixas ativos.
- Atendimento concluído gera registro com caixa e horários para cálculo da espera.

## Fluxos de terminal previstos

- **Administrador:** visão geral, produtos, categorias, usuários, caixas,
  parâmetros e relatórios.
- **Estoquista:** consulta de produtos, entradas, alertas e histórico de estoque.
- **Operador de caixa:** registro de venda e operação de atendimento/caixa.

Os fluxos acima descrevem a navegação planejada.
