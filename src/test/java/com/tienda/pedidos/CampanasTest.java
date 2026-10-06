package com.tienda.pedidos;

import com.tienda.pedidos.dto.ItemPedido;
import com.tienda.pedidos.dto.PedidoRequest;
import com.tienda.pedidos.dto.ResultadoPedido;
import com.tienda.pedidos.service.GestorPedidos;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Campañas CORPORATIVO y VOLUMEN, con Black Friday INACTIVO (valor por defecto).
 * Estos mismos casos deben dar el mismo total con la version de eslabones de cadena
 * (Golden Hammer) y con la version corregida con Strategy.
 */
@SpringBootTest
@Transactional
class CampanasTest {

    private static final double DELTA = 0.01;

    @Autowired GestorPedidos gestor;

    private static PedidoRequest pedido(long clienteId, long productoId, int cantidad) {
        ItemPedido i = new ItemPedido();
        i.setProductoId(productoId);
        i.setCantidad(cantidad);
        PedidoRequest r = new PedidoRequest();
        r.setClienteId(clienteId);
        r.setClienteEmail("cliente" + clienteId + "@tienda.com");
        r.setItems(List.of(i));
        return r;
    }

    @Test
    void corporativoConNitRecibe10PorCiento() {
        // 100000 ; desc 10% ; base 90000 ; IVA 17100 ; total 107100
        ResultadoPedido res = gestor.procesarPedido(pedido(5, 1, 1));
        assertTrue(res.isConfirmado());
        assertEquals(107_100.0, res.getTotal(), DELTA);
    }

    @Test
    void volumenMasDe20UnidadesRecibe12PorCiento() {
        // 21 x 100000 = 2100000 ; desc 12% ; base 1848000 ; IVA 351120 ; total 2199120
        ResultadoPedido res = gestor.procesarPedido(pedido(4, 1, 21));
        assertTrue(res.isConfirmado());
        assertEquals(2_199_120.0, res.getTotal(), DELTA);
    }

    @Test
    void exactamente20UnidadesNoActivaVolumen() {
        // 20 x 100000 = 2000000 ; sin descuento ; IVA 380000 ; total 2380000
        ResultadoPedido res = gestor.procesarPedido(pedido(4, 1, 20));
        assertTrue(res.isConfirmado());
        assertEquals(2_380_000.0, res.getTotal(), DELTA);
    }

    @Test
    void entreVipYVolumenGanaElMayor() {
        // VIP >1M = 15% frente a volumen 12% -> 15% ; base 1785000 ; total 2124150
        ResultadoPedido res = gestor.procesarPedido(pedido(1, 1, 21));
        assertTrue(res.isConfirmado());
        assertEquals(2_124_150.0, res.getTotal(), DELTA);
    }

    @Test
    void entreCorporativoYVolumenGanaElMayor() {
        // corporativo 10% frente a volumen 12% -> 12% ; total 2199120
        ResultadoPedido res = gestor.procesarPedido(pedido(5, 1, 21));
        assertTrue(res.isConfirmado());
        assertEquals(2_199_120.0, res.getTotal(), DELTA);
    }

    @Test
    void blackFridayInactivoNoAplicaDescuento() {
        ResultadoPedido res = gestor.procesarPedido(pedido(4, 1, 1));
        assertTrue(res.isConfirmado());
        assertEquals(119_000.0, res.getTotal(), DELTA);
    }
}
