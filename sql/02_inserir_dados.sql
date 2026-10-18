-- Dados iniciais do Mercado Fácil
-- Execute DEPOIS de sql/01_criar_banco.sql (que cria o banco e as tabelas).
--
-- No projeto, a quantidade em estoque NÃO é uma coluna de "produtos":
-- ela é calculada somando as ENTRY e subtraindo as EXIT de "movimentacoes_estoque".
-- Por isso a quantidade de cada produto entra aqui como uma movimentação de ENTRY.
--
-- O script pode ser executado mais de uma vez sem duplicar dados (INSERT IGNORE).

USE mercado_facil;

-- ---------------------------------------------------------
-- 1) Categorias
-- ---------------------------------------------------------
INSERT IGNORE INTO categorias (nome) VALUES
  ('Bebidas'),
  ('Laticínios'),
  ('Limpeza'),
  ('Mercearia'),
  ('Hortifruti');

-- ---------------------------------------------------------
-- 2) Produtos (código, nome, categoria, preço, estoque mínimo, cadastro, ativo)
-- ---------------------------------------------------------
INSERT IGNORE INTO produtos (codigo, nome, categoria_nome, preco, estoque_minimo, data_cadastro, ativo) VALUES
  ('78910001', 'Refrigerante Coca-Cola 2L',           'Bebidas',    8.99, 20, '2026-10-18 08:00:00', TRUE),
  ('78910002', 'Leite Integral Aurora 1L',            'Laticínios', 4.20, 10, '2026-10-18 08:00:00', TRUE),
  ('78910003', 'Manteiga Aviação com Sal 200g',       'Laticínios', 9.50, 12, '2026-10-18 08:00:00', TRUE),
  ('78910004', 'Detergente Ypê Coco 500ml',           'Limpeza',    2.20, 10, '2026-10-18 08:00:00', TRUE),
  ('78910005', 'Arroz Agulhinha Tipo 1 Tio João 5kg', 'Mercearia', 24.90, 15, '2026-10-18 08:00:00', TRUE),
  ('78910006', 'Detergente Líquido Limpol 500ml',     'Limpeza',    3.80, 20, '2026-10-18 08:00:00', TRUE),
  ('78910011', 'Feijão Carioca 1kg',                  'Mercearia',  7.50, 20, '2026-10-18 08:00:00', TRUE),
  ('78910012', 'Banana Prata (kg)',                   'Hortifruti', 5.99, 10, '2026-10-18 08:00:00', TRUE),
  ('78910007', 'Azeite de Oliva Extra Virgem 500ml',  'Mercearia', 39.90, 10, '2026-10-18 08:00:00', TRUE),
  ('78910008', 'Sabão em Pó Omo Multiação 1kg',       'Limpeza',   14.50, 10, '2026-10-18 08:00:00', TRUE),
  ('78910009', 'Cerveja Heineken Lata 350ml',         'Bebidas',    5.49, 24, '2026-10-18 08:00:00', TRUE),
  ('78910010', 'Saco de Lixo Embalixo 50L',           'Limpeza',   12.00, 10, '2026-10-18 08:00:00', TRUE);

-- ---------------------------------------------------------
-- 3) Estoque inicial = uma ENTRY por produto
-- ---------------------------------------------------------
INSERT IGNORE INTO movimentacoes_estoque (id, codigo_produto, quantidade, tipo, data_movimentacao, responsavel) VALUES
  ( 1, '78910001',  45, 'ENTRY', '2026-10-18 08:30:00', 'Carlos Oliveira'),
  ( 2, '78910002',   3, 'ENTRY', '2026-10-18 08:30:00', 'Carlos Oliveira'),
  ( 3, '78910003',  11, 'ENTRY', '2026-10-18 08:30:00', 'Carlos Oliveira'),
  ( 4, '78910004',  18, 'ENTRY', '2026-10-18 08:30:00', 'Carlos Oliveira'),
  ( 5, '78910005',   2, 'ENTRY', '2026-10-18 08:30:00', 'Carlos Oliveira'),
  ( 6, '78910006',   8, 'ENTRY', '2026-10-18 08:30:00', 'Carlos Oliveira'),
  ( 7, '78910011', 100, 'ENTRY', '2026-10-18 08:30:00', 'Carlos Oliveira'),
  ( 8, '78910012',  40, 'ENTRY', '2026-10-18 08:30:00', 'Carlos Oliveira'),
  ( 9, '78910007',  42, 'ENTRY', '2026-10-18 08:30:00', 'Carlos Oliveira'),
  (10, '78910008',  60, 'ENTRY', '2026-10-18 08:30:00', 'Carlos Oliveira'),
  (11, '78910009', 110, 'ENTRY', '2026-10-18 08:30:00', 'Carlos Oliveira'),
  (12, '78910010',  25, 'ENTRY', '2026-10-18 08:30:00', 'Carlos Oliveira');

-- ---------------------------------------------------------
-- Conferência (mesma lógica que o MySQLProductRepository usa)
-- ---------------------------------------------------------
SELECT p.codigo AS CODIGO,
       p.nome AS NOME,
       p.categoria_nome AS CATEGORIA,
       p.preco AS PRECO,
       COALESCE(SUM(CASE WHEN m.tipo = 'ENTRY' THEN m.quantidade
                         WHEN m.tipo = 'EXIT'  THEN -m.quantidade END), 0) AS QTD,
       p.estoque_minimo AS MIN,
       CASE WHEN COALESCE(SUM(CASE WHEN m.tipo = 'ENTRY' THEN m.quantidade
                                   WHEN m.tipo = 'EXIT'  THEN -m.quantidade END), 0) < p.estoque_minimo
            THEN 'ABAIXO DO MÍNIMO' ELSE 'ok' END AS SITUACAO
FROM produtos p
LEFT JOIN movimentacoes_estoque m ON m.codigo_produto = p.codigo
WHERE p.ativo = TRUE
GROUP BY p.codigo, p.nome, p.categoria_nome, p.preco, p.estoque_minimo
ORDER BY p.nome;
