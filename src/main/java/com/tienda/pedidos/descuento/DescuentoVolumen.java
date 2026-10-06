package com.tienda.pedidos.descuento;

import com.tienda.pedidos.validacion.ContextoPedido;
import org.springframework.stereotype.Component;

@Component
public class DescuentoVolumen implements EstrategiaDescuento {
    @Override
    public double calcular(ContextoPedido contexto) {
        int totalUnidades = contexto.getRequest().getItems().stream()
            .mapToInt(item -> item.getCantidad()).sum();
        return totalUnidades > 20 ? 0.12 : 0.0;
    }
}
