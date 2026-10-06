package com.tienda.pedidos.validacion;

import com.tienda.pedidos.dto.PedidoRequest;

// Contexto mutable que viaja a traves de la cadena de validacion
public class ContextoPedido {
    private final PedidoRequest request;
    private String tipoCliente;
    private double subtotal;
    private boolean rechazado = false;
    private String motivoRechazo;
    // Se agrega un campo mutable mas al contexto para que los nuevos eslabones puedan escribir el descuento
    private double descuentoCampana = 0;

    public ContextoPedido(PedidoRequest request) { this.request = request; }

    public PedidoRequest getRequest() { return request; }
    public String getTipoCliente() { return tipoCliente; }
    public void setTipoCliente(String tipoCliente) { this.tipoCliente = tipoCliente; }
    public double getSubtotal() { return subtotal; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }
    public boolean isRechazado() { return rechazado; }
    public String getMotivoRechazo() { return motivoRechazo; }
    public void rechazar(String motivo) { this.rechazado = true; this.motivoRechazo = motivo; }

    public double getDescuentoCampana() { return descuentoCampana; }
    public void aplicarDescuentoCampana(double valor) {
        if (valor > this.descuentoCampana) this.descuentoCampana = valor; // el mayor descuento gana
    }
}
