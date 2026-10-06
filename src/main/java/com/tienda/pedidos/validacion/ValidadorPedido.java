package com.tienda.pedidos.validacion;

// Eslabon de la cadena: cada validador decide si el pedido continua o se rechaza
public abstract class ValidadorPedido {
    private ValidadorPedido siguiente;

    /** Enlaza el siguiente eslabon y lo devuelve, para poder escribir a.encadenar(b).encadenar(c). */
    public ValidadorPedido encadenar(ValidadorPedido siguiente) {
        this.siguiente = siguiente;
        return siguiente;
    }

    public final void validar(ContextoPedido contexto) {
        ejecutar(contexto);
        if (!contexto.isRechazado() && siguiente != null) {
            siguiente.validar(contexto);
        }
    }

    protected abstract void ejecutar(ContextoPedido contexto);
}
