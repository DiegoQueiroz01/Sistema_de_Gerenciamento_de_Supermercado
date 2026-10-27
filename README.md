# Sistema de Gerenciamento de Supermercado

Aplicativo de gerenciamento de supermercado executado pelo terminal, baseado nos
relatórios de controle de produtos e estoque e de gerenciamento de filas e caixas.

## Escopo

- Cadastro, pesquisa, edição, categorização e ordenação de produtos.
- Entradas e saídas de estoque com histórico e alertas de estoque mínimo.
- Registro de vendas e consulta de produtos mais vendidos.
- Gerenciamento de caixas, filas comuns e prioritárias, atendimento e histórico.
- Acesso organizado por perfil: administrador, estoquista e operador de caixa.
- Interface exclusivamente textual, sem frontend gráfico ou aplicação web.

## Arquitetura

O projeto segue uma separação em camadas:

- `domain`: entidades e regras centrais do supermercado.
- `application`: serviços que coordenam os casos de uso.
- `repositories`: contratos de acesso aos dados.
- `infrastructure/memory`: armazenamento em memória e estruturas de dados.
- `infrastructure/mysql`: armazenamento de produtos e estoque no MySQL.
- `presentation/cli`: menus, leitura de comandos e apresentação no terminal.

O armazenamento em memória segue as estruturas de dados dos relatórios: mapa por
código para produtos, listas para consultas e históricos e filas para o
atendimento. Produtos, categorias e movimentações de estoque também podem ser
gravados no MySQL.

Veja [docs/architecture.md](docs/architecture.md) para o detalhamento dos módulos,
responsabilidades e regras contempladas.

## Como executar

### Requisitos

- JDK 17 ou superior.
- Maven 3.9 ou superior (`mvn -version`).
- MySQL 8, apenas para a versão com banco de dados.

Todos os comandos abaixo são executados na pasta que contém o `pom.xml`.

### Aplicação (em memória, sem banco de dados)

```bash
mvn clean compile exec:java
```

O sistema abre já com produtos, 5 caixas (3 abertos) e um usuário de cada perfil:

| Perfil            | Login    | Senha        |
|-------------------|----------|--------------|
| Administrador     | `admin`  | `admin123`   |
| Estoquista        | `carlos` | `estoque123` |
| Operador de caixa | `marta`  | `caixa123`   |

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

Produtos e estoque:

```bash
mvn test-compile exec:java -Dexec.mainClass=br.com.supermercado.E1Tests -Dexec.classpathScope=test
```

Caixas e filas:

```bash
mvn test-compile exec:java -Dexec.mainClass=br.com.supermercado.E2Tests -Dexec.classpathScope=test
```

O resultado aparece no final, por exemplo: `Resultado: 37 passaram, 0 falharam.`

### Aplicação com MySQL

1. Crie o banco e as tabelas executando `sql/01_criar_banco.sql` no MySQL
   (pelo MySQL Workbench ou pelo terminal):

   ```bash
   mysql -u root -p < sql/01_criar_banco.sql
   ```

2. Opcional: carregue os dados de exemplo.

   ```bash
   mysql -u root -p < sql/02_inserir_dados.sql
   ```

3. Informe as credenciais do MySQL por variáveis de ambiente.

   Linux/macOS:

   ```bash
   export MERCADO_DB_USUARIO=root
   export MERCADO_DB_SENHA='sua_senha'
   ```

   Windows (PowerShell):

   ```powershell
   $env:MERCADO_DB_USUARIO = "root"
   $env:MERCADO_DB_SENHA = "sua_senha"
   ```

   Por padrão a conexão usa `jdbc:mysql://localhost:3306/mercado_facil`. Para
   outro endereço ou porta, defina também `MERCADO_DB_URL`.

4. Execute com a opção `--mysql`:

   ```bash
   mvn clean compile exec:java -Dexec.args=--mysql
   ```

   Nesse modo, produtos, categorias e movimentações de estoque ficam no MySQL; os
   demais dados (usuários, vendas, caixas e filas) ficam em memória durante a sessão.

Não coloque senhas reais em arquivos versionados. Mais detalhes sobre o banco
estão em [LEIA-ME-MYSQL.md](LEIA-ME-MYSQL.md).
