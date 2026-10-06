package com.tienda.pedidos.validacion;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// Nuevo eslabon: Black Friday. No valida nada, solo escribe un descuento --
// se agrego a la cadena porque "los eslabones ya sabian como conectarse entre si"
@Component
public class PromocionBlackFriday extends ValidadorPedido {
    private final boolean campanaActiva; // inyectado desde application.properties

    public PromocionBlackFriday(@Value("${promo.black-friday.activa}") boolean campanaActiva) {
        this.campanaActiva = campanaActiva;
    }

    @Override
    protected void ejecutar(ContextoPedido contexto) {
        if (campanaActiva) {
            contexto.aplicarDescuentoCampana(0.25);
        }
        // nunca rechaza -- este eslabon no valida nada, solo aprovecha que la cadena
        // ya existe para "engancharse" y modificar el contexto
    }
}
