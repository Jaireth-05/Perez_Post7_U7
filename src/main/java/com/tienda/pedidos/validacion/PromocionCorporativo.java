package com.tienda.pedidos.validacion;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

// Nuevo eslabon: cliente corporativo
@Component
public class PromocionCorporativo extends ValidadorPedido {
    private final JdbcTemplate jdbcTemplate;

    public PromocionCorporativo(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    protected void ejecutar(ContextoPedido contexto) {
        String nit = jdbcTemplate.queryForObject(
            "SELECT nit FROM clientes WHERE id = ?", String.class, contexto.getRequest().getClienteId());
        if (nit != null && !nit.isBlank()) {
            contexto.aplicarDescuentoCampana(0.10);
        }
    }
}
