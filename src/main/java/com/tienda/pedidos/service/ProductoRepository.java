package com.tienda.pedidos.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ProductoRepository {
    private final JdbcTemplate jdbcTemplate;

    public ProductoRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public double precioUnitario(Long productoId) {
        Double precio = jdbcTemplate.queryForObject(
            "SELECT precio FROM productos WHERE id = ?", Double.class, productoId);
        return precio;
    }
}
