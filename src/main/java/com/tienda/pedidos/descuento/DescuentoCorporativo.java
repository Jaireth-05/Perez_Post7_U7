package com.tienda.pedidos.descuento;

import com.tienda.pedidos.validacion.ContextoPedido;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DescuentoCorporativo implements EstrategiaDescuento {
    private final JdbcTemplate jdbcTemplate;

    public DescuentoCorporativo(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public double calcular(ContextoPedido contexto) {
        String nit = jdbcTemplate.queryForObject(
            "SELECT nit FROM clientes WHERE id = ?", String.class,
            contexto.getRequest().getClienteId());
        return (nit != null && !nit.isBlank()) ? 0.10 : 0.0;
    }
}
