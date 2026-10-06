package com.tienda.pedidos.dto;

import java.util.List;

public class PedidoRequest {
    private Long clienteId;
    private String clienteEmail;
    private List<ItemPedido> items;

    public Long getClienteId() { return clienteId; }
    public void setClienteId(Long clienteId) { this.clienteId = clienteId; }
    public String getClienteEmail() { return clienteEmail; }
    public void setClienteEmail(String clienteEmail) { this.clienteEmail = clienteEmail; }
    public List<ItemPedido> getItems() { return items; }
    public void setItems(List<ItemPedido> items) { this.items = items; }
}
