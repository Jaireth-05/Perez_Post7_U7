package com.tienda.pedidos.validacion;

import com.tienda.pedidos.dto.ItemPedido;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

// Eslabon 1: pedido con items y stock suficiente (primer filtro; sin esto no tiene sentido seguir)
@Component
public class ValidadorStock extends ValidadorPedido {
    private final JdbcTemplate jdbcTemplate;

    public ValidadorStock(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    protected void ejecutar(ContextoPedido contexto) {
        List<ItemPedido> items = contexto.getRequest().getItems();
        // Se conserva el rechazo del original para pedidos vacios (evita un NullPointerException)
        if (items == null || items.isEmpty()) {
            contexto.rechazar("El pedido no contiene items");
            return;
        }
        for (ItemPedido item : items) {
            Integer stock = jdbcTemplate.queryForObject(
                "SELECT stock FROM inventario WHERE producto_id = ?", Integer.class, item.getProductoId());
            if (stock == null || stock < item.getCantidad()) {
                contexto.rechazar("Stock insuficiente: producto " + item.getProductoId());
                return;
            }
        }
    }
}
