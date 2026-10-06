package com.tienda.pedidos.descuento;

import com.tienda.pedidos.validacion.ContextoPedido;
import org.springframework.stereotype.Component;

import java.util.List;

// El descuento final combina la estrategia por tipo de cliente con las campañas,
// tomando el mayor. La regla "el mayor gana" vive aqui, de forma explicita, y no en un
// campo mutable compartido escrito por clases que no son responsables de validar nada.
@Component
public class CalculadorDescuentoFinal {
    private final SelectorEstrategiaDescuento selectorPorCliente;
    private final List<EstrategiaDescuento> campanas;

    public CalculadorDescuentoFinal(SelectorEstrategiaDescuento selectorPorCliente,
                                    DescuentoBlackFriday blackFriday, DescuentoCorporativo corporativo,
                                    DescuentoVolumen volumen) {
        this.selectorPorCliente = selectorPorCliente;
        this.campanas = List.of(blackFriday, corporativo, volumen);
    }

    public double calcular(ContextoPedido contexto) {
        double porTipoCliente = selectorPorCliente.seleccionar(contexto.getTipoCliente()).calcular(contexto);
        double porCampana = campanas.stream()
            .mapToDouble(estrategia -> estrategia.calcular(contexto))
            .max().orElse(0.0);
        return Math.max(porTipoCliente, porCampana);
    }
}
