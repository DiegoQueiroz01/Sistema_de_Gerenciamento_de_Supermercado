-- Esquema do Mercado Fácil (nomes em português)
CREATE DATABASE IF NOT EXISTS mercado_facil CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE mercado_facil;

CREATE TABLE IF NOT EXISTS categorias (
  nome VARCHAR(100) PRIMARY KEY
);

CREATE TABLE IF NOT EXISTS produtos (
  codigo VARCHAR(50) PRIMARY KEY,
  nome VARCHAR(150) NOT NULL,
  categoria_nome VARCHAR(100) NOT NULL,
  preco DECIMAL(10,2) NOT NULL,
  estoque_minimo INT NOT NULL DEFAULT 0,
  data_cadastro DATETIME NOT NULL,
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  CONSTRAINT fk_produtos_categorias FOREIGN KEY (categoria_nome) REFERENCES categorias(nome)
);

CREATE TABLE IF NOT EXISTS movimentacoes_estoque (
  id BIGINT PRIMARY KEY,
  codigo_produto VARCHAR(50) NOT NULL,
  quantidade INT NOT NULL,
  tipo ENUM('ENTRY','EXIT') NOT NULL,
  data_movimentacao DATETIME NOT NULL,
  responsavel VARCHAR(150) NOT NULL,
  CONSTRAINT fk_movimentacoes_produtos FOREIGN KEY (codigo_produto) REFERENCES produtos(codigo),
  CONSTRAINT chk_movimentacao_quantidade CHECK (quantidade > 0),
  INDEX idx_movimentacoes_produto_data (codigo_produto, data_movimentacao)
);
