package br.com.supermercado.infrastructure.mysql;

import br.com.supermercado.domain.inventory.MovementType;
import br.com.supermercado.domain.product.Category;
import br.com.supermercado.domain.product.Product;
import br.com.supermercado.repositories.ProductRepository;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/** Repositório persistente de produtos e categorias. A quantidade é reconstruída pelo histórico. */
public final class MySQLProductRepository implements ProductRepository {
    @Override public void save(Product p) {
        String sql = "INSERT INTO produtos (codigo,nome,categoria_nome,preco,estoque_minimo,data_cadastro,ativo) VALUES (?,?,?,?,?,?,TRUE) ON DUPLICATE KEY UPDATE nome=VALUES(nome),categoria_nome=VALUES(categoria_nome),preco=VALUES(preco),estoque_minimo=VALUES(estoque_minimo),ativo=TRUE";
        try (Connection c=ConexaoMySQL.abrir(); PreparedStatement s=c.prepareStatement(sql)) {
            s.setString(1,p.code()); s.setString(2,p.name()); s.setString(3,p.category().name()); s.setDouble(4,p.price()); s.setInt(5,p.minimumStock()); s.setTimestamp(6,Timestamp.valueOf(p.registeredAt())); s.executeUpdate();
        } catch(SQLException e) { throw erro("salvar produto",e); }
    }
    @Override public Optional<Product> findByCode(String code) {
        if(code==null) return Optional.empty();
        String sql="SELECT p.codigo,p.nome,p.categoria_nome,p.preco,p.estoque_minimo,p.data_cadastro, COALESCE(SUM(CASE WHEN m.tipo='ENTRY' THEN m.quantidade WHEN m.tipo='EXIT' THEN -m.quantidade ELSE 0 END),0) quantidade FROM produtos p LEFT JOIN movimentacoes_estoque m ON m.codigo_produto=p.codigo WHERE p.codigo=? AND p.ativo=TRUE GROUP BY p.codigo,p.nome,p.categoria_nome,p.preco,p.estoque_minimo,p.data_cadastro";
        try(Connection c=ConexaoMySQL.abrir(); PreparedStatement s=c.prepareStatement(sql)){s.setString(1,code.trim());try(ResultSet r=s.executeQuery()){return r.next()?Optional.of(mapear(r)):Optional.empty();}}catch(SQLException e){throw erro("consultar produto",e);}
    }
    @Override public boolean existsByCode(String code){return findByCode(code).isPresent();}
    @Override public List<Product> findAll(){
        String sql="SELECT p.codigo,p.nome,p.categoria_nome,p.preco,p.estoque_minimo,p.data_cadastro, COALESCE(SUM(CASE WHEN m.tipo='ENTRY' THEN m.quantidade WHEN m.tipo='EXIT' THEN -m.quantidade ELSE 0 END),0) quantidade FROM produtos p LEFT JOIN movimentacoes_estoque m ON m.codigo_produto=p.codigo WHERE p.ativo=TRUE GROUP BY p.codigo,p.nome,p.categoria_nome,p.preco,p.estoque_minimo,p.data_cadastro ORDER BY p.nome";
        List<Product> out=new ArrayList<>();try(Connection c=ConexaoMySQL.abrir();PreparedStatement s=c.prepareStatement(sql);ResultSet r=s.executeQuery()){while(r.next())out.add(mapear(r));return out;}catch(SQLException e){throw erro("listar produtos",e);}
    }
    private Product mapear(ResultSet r)throws SQLException{
        Category cat=new Category(r.getString("categoria_nome")); Product p=new Product(r.getString("codigo"),r.getString("nome"),cat,r.getDouble("preco"),r.getInt("estoque_minimo"),r.getTimestamp("data_cadastro").toLocalDateTime());
        int q=r.getInt("quantidade"); if(q>0)p.updateStock(q,MovementType.ENTRY); else if(q<0)throw new SQLException("Histórico de estoque inconsistente para o produto "+p.code()); return p;
    }
    @Override public void saveCategory(Category category){try(Connection c=ConexaoMySQL.abrir();PreparedStatement s=c.prepareStatement("INSERT INTO categorias(nome) VALUES (?) ON DUPLICATE KEY UPDATE nome=VALUES(nome)")){s.setString(1,category.name());s.executeUpdate();}catch(SQLException e){throw erro("salvar categoria",e);}}
    @Override public List<Category> findAllCategories(){List<Category> out=new ArrayList<>();try(Connection c=ConexaoMySQL.abrir();PreparedStatement s=c.prepareStatement("SELECT nome FROM categorias ORDER BY nome");ResultSet r=s.executeQuery()){while(r.next())out.add(new Category(r.getString(1)));return out;}catch(SQLException e){throw erro("listar categorias",e);}}
    private RuntimeException erro(String acao,SQLException e){return new IllegalStateException("Não foi possível "+acao+" no MySQL: "+e.getMessage(),e);}
}
