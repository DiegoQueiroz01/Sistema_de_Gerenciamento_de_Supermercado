# Sistema de Gerenciamento de Supermercado

Aplicativo de gerenciamento de supermercado executado pelo terminal, baseado nos
relatórios de controle de produtos e estoque e de gerenciamento de filas e caixas.

## Escopo

- Cadastro, pesquisa, edição, categorização, ordenação e desativação de produtos.
- Entradas e saídas de estoque com histórico e alertas de estoque mínimo.
- Registro de vendas com carrinho e cupom, baixa automática de estoque e ranking
  de produtos mais vendidos.
- Gerenciamento de caixas, filas comuns e prioritárias, atendimento e histórico.
- Login com perfis (administrador, estoquista e operador de caixa) e gestão de usuários.
- Visão geral do supermercado, dados do estabelecimento e parâmetros.
- Interface exclusivamente textual, sem frontend gráfico ou aplicação web.

## Arquitetura

O projeto segue uma separação em camadas:

- `domain`: entidades e regras centrais do supermercado.
- `application`: serviços que coordenam os casos de uso.
- `repositories`: contratos de acesso aos dados.
- `infrastructure/memory`: armazenamento em memória e estruturas de dados.
- `presentation/cli`: menus, leitura de comandos e apresentação no terminal.

## Estruturas de dados

Não há banco de dados: todos os dados ficam em memória, nas estruturas abaixo,
durante a execução do programa:

| Dado | Estrutura | Por quê |
|------|-----------|---------|
| Produtos | mapa por código (`LinkedHashMap`) | busca direta pelo código e bloqueio de código repetido |
| Usuários | mapa por login | busca direta no login e bloqueio de login repetido |
| Caixas | mapa por número, na ordem de criação | busca pelo número e percurso para achar a menor fila |
| Fila de cada caixa | lista encadeada (`LinkedList`) | FIFO para comuns; prioritários inseridos à frente dos comuns |
| Movimentações, vendas e atendimentos | listas em ordem de registro | histórico cronológico; atendimentos também indexados por caixa |
| Carrinho da venda | mapa ordenado + pilha (`ArrayDeque`) | soma itens repetidos; "desfazer último item" em ordem inversa (LIFO) |

Ordenações usam `Comparator` (nome, preço, quantidade, déficit, mais vendidos).

Veja [docs/architecture.md](docs/architecture.md) para o detalhamento dos módulos,
responsabilidades e regras contempladas.

## Como executar

### Requisitos

- JDK 17 ou superior.
- Maven 3.9 ou superior (`mvn -version`).

Todos os comandos abaixo são executados na pasta que contém o `pom.xml`.

### Aplicação

```bash
mvn clean compile exec:java
```

O sistema abre já com produtos, 5 caixas (3 abertos) e um usuário de cada perfil.

### Usuários e acessos

Na tela inicial escolha `1) Entrar (login)` e informe o login e a senha. Cada
perfil vê um menu diferente:

| Perfil            | Login        | Senha        | O que pode acessar |
|-------------------|--------------|--------------|--------------------|
| Administrador     | `admin`      | `admin123`   | Tudo: visão geral, produtos/categorias/estoque, caixas e filas, vendas, usuários, dados do supermercado e parâmetros |
| Estoquista        | `estoquista` | `estoque123` | Produtos e estoque: listar, pesquisar, filtrar, ordenar, entradas, saídas, alertas de estoque mínimo e histórico |
| Operador de caixa | `caixa`      | `caixa123`   | Vendas (carrinho, cupom, consulta de vendas) e caixas e filas (abrir/fechar caixa, novos clientes, atendimento) |

Todos os perfis podem trocar a própria senha. Como os dados ficam só em memória,
senhas alteradas e usuários novos voltam ao padrão quando o programa é fechado.

### Demonstrações isoladas dos módulos

Menus do estoquista e do administrador (produtos e estoque), sem login:

```bash
mvn test-compile exec:java -Dexec.mainClass=br.com.supermercado.E1Demo -Dexec.classpathScope=test
```

Menu de caixas e filas, com 5 caixas (3 abertos), 7 clientes e 2 atendimentos já
carregados:

```bash
mvn test-compile exec:java -Dexec.mainClass=br.com.supermercado.E2Demo -Dexec.classpathScope=test
```

### Testes

Todas as suítes de uma vez:

```bash
mvn test
```

Cada suíte também pode ser executada sozinha, trocando o nome da classe:
`E1Tests` (produtos e estoque), `E2Tests` (caixas e filas), `SalesTests` (vendas),
`UserTests` (usuários e login), `StoreTests` (dados do supermercado) e
`AdminTests` (saídas, desativação, visão geral e parâmetros).

```bash
mvn test-compile exec:java -Dexec.mainClass=br.com.supermercado.E1Tests -Dexec.classpathScope=test
```

O resultado aparece no final, por exemplo: `Resultado: 37 passaram, 0 falharam.`
