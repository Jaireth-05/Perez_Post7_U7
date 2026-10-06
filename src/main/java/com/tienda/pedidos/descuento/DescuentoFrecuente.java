package com.tienda.pedidos.descuento;

import com.tienda.pedidos.validacion.ContextoPedido;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DescuentoFrecuente implements EstrategiaDescuento {
    private final JdbcTemplate jdbcTemplate;

    public DescuentoFrecuente(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public double calcular(ContextoPedido contexto) {
        Integer pedidosPrevios = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM pedidos WHERE cliente_id = ?", Integer.class,
            contexto.getRequest().getClienteId());
        if (pedidosPrevios != null && pedidosPrevios > 10) return 0.08;
        if (pedidosPrevios != null && pedidosPrevios > 3) return 0.04;
        return 0.0;
    }
}
