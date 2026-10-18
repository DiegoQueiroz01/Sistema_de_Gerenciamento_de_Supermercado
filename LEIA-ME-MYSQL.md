# Mercado Fácil — configuração MySQL

## 1. Requisitos
- JDK 21 (ou compatível com o `pom.xml`)
- MySQL Server em execução e MySQL Workbench
- Maven instalado (`mvn -version`)

## 2. Criar as tabelas
No MySQL Workbench, abra e execute o arquivo `sql/01_criar_banco.sql`. Ele cria `mercado_facil` e as tabelas `categorias`, `produtos` e `movimentacoes_estoque`.

## 3. Configurar credenciais no Windows PowerShell
Defina as variáveis no terminal antes de executar o programa (troque pela sua senha local):

```powershell
$env:MERCADO_DB_USUARIO = "root"
$env:MERCADO_DB_SENHA = "SUA_SENHA_DO_MYSQL"
```

Se seu usuário não tiver senha, defina `$env:MERCADO_DB_SENHA = ""`. Não coloque senhas reais em arquivos versionados.

## 4. Executar
Na pasta que contém `pom.xml`:

```powershell
mvn clean compile exec:java
```

O Maven baixa o driver JDBC. O programa usa `jdbc:mysql://localhost:3306/mercado_facil` por padrão. Se necessário, defina `MERCADO_DB_URL`.

## 5. Persistência e histórico
- Produtos e categorias são gravados no MySQL.
- O estoque disponível é reconstruído somando entradas e subtraindo saídas do histórico.
- Movimentações são preservadas; não apague diretamente produtos que tenham movimentações.
- A demonstração antiga `E1Demo` em `src/test` usa repositórios em memória e permanece separada. Execute `br.com.supermercado.MercadoFacilApp` para usar o MySQL.
- A integração com vendas e o ranking de mais vendidos ainda dependem da fonte de vendas do módulo E.2; o histórico de entradas/saídas do estoque é persistido neste módulo.

## Observação
O esquema usa `ENTRY` e `EXIT` porque esses são os valores do enum `MovementType` no código existente. Os nomes das tabelas e colunas estão em português.
