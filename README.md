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
- `presentation/cli`: menus, leitura de comandos e apresentação no terminal.

O armazenamento inicial está planejado para memória, alinhado às estruturas de
dados dos relatórios: mapa por código para produtos, listas para consultas e
históricos e filas para o atendimento. Persistência em arquivo ou banco de dados
não está definida pelos relatórios e não foi incluída nesta estrutura.

Veja [docs/architecture.md](docs/architecture.md) para o detalhamento dos módulos,
responsabilidades e regras contempladas.

## Estado atual

Este repositório contém somente a arquitetura e os arquivos de estrutura. Os
arquivos Java estão intencionalmente vazios; a implementação será feita quando
solicitada.
