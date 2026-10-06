package com.tienda.pedidos.descuento;

import com.tienda.pedidos.validacion.ContextoPedido;

// Strategy: cada regla de descuento encapsula su propio calculo,
// sin depender de un orden de evaluacion frente a las demas
public interface EstrategiaDescuento {
    double calcular(ContextoPedido contexto);
}
