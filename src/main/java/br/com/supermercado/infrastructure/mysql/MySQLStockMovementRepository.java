package br.com.supermercado.infrastructure.mysql;

import br.com.supermercado.domain.inventory.MovementType;
import br.com.supermercado.domain.inventory.StockMovement;
import br.com.supermercado.repositories.StockMovementRepository;
import java.sql.*;
import java.util.*;

/** Histórico persistente de entradas e saídas. */
public final class MySQLStockMovementRepository implements StockMovementRepository {
    @Override public long nextId(){try(Connection c=ConexaoMySQL.abrir();Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT COALESCE(MAX(id),0)+1 FROM movimentacoes_estoque")){r.next();return r.getLong(1);}catch(SQLException e){throw erro(e);}}
    @Override public void save(StockMovement m){String sql="INSERT INTO movimentacoes_estoque(id,codigo_produto,quantidade,tipo,data_movimentacao,responsavel) VALUES(?,?,?,?,?,?)";try(Connection c=ConexaoMySQL.abrir();PreparedStatement s=c.prepareStatement(sql)){s.setLong(1,m.id());s.setString(2,m.productCode());s.setInt(3,m.quantity());s.setString(4,m.type().name());s.setTimestamp(5,Timestamp.valueOf(m.timestamp()));s.setString(6,m.responsible());s.executeUpdate();}catch(SQLException e){throw erro(e);}}
    @Override public List<StockMovement> findAll(){return consultar(null);}
    @Override public List<StockMovement> findByProductCode(String code){return consultar(code);}
    private List<StockMovement> consultar(String code){String sql="SELECT id,codigo_produto,quantidade,tipo,data_movimentacao,responsavel FROM movimentacoes_estoque"+(code==null?" ORDER BY id":" WHERE codigo_produto=? ORDER BY id");List<StockMovement> out=new ArrayList<>();try(Connection c=ConexaoMySQL.abrir();PreparedStatement s=c.prepareStatement(sql)){if(code!=null)s.setString(1,code);try(ResultSet r=s.executeQuery()){while(r.next())out.add(new StockMovement(r.getLong("id"),r.getString("codigo_produto"),r.getInt("quantidade"),MovementType.valueOf(r.getString("tipo")),r.getTimestamp("data_movimentacao").toLocalDateTime(),r.getString("responsavel")));}return out;}catch(SQLException e){throw erro(e);}}
    private RuntimeException erro(SQLException e){return new IllegalStateException("Erro ao acessar o histórico no MySQL: "+e.getMessage(),e);}
}
