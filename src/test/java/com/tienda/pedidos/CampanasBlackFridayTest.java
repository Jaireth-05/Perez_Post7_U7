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

/** Campaña BLACK_FRIDAY ACTIVA (25 % fijo). Mismos totales con ambas versiones del diseño. */
@SpringBootTest(properties = "promo.black-friday.activa=true")
@Transactional
class CampanasBlackFridayTest {

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
    void clienteEstandarRecibe25PorCiento() {
        // 100000 ; desc 25% ; base 75000 ; IVA 14250 ; total 89250
        ResultadoPedido res = gestor.procesarPedido(pedido(4, 1, 1));
        assertTrue(res.isConfirmado());
        assertEquals(89_250.0, res.getTotal(), DELTA);
    }

    @Test
    void blackFridayGanaAVipBajo() {
        // 2 x 100000 = 200000 ; VIP 5% frente a BF 25% -> 25% ; base 150000 ; total 178500
        ResultadoPedido res = gestor.procesarPedido(pedido(1, 1, 2));
        assertTrue(res.isConfirmado());
        assertEquals(178_500.0, res.getTotal(), DELTA);
    }

    @Test
    void blackFridayGanaAVipAlto() {
        // 5 x 250000 = 1250000 ; VIP 15% frente a BF 25% -> 25% ; base 937500 ; total 1115625
        ResultadoPedido res = gestor.procesarPedido(pedido(1, 2, 5));
        assertTrue(res.isConfirmado());
        assertEquals(1_115_625.0, res.getTotal(), DELTA);
    }
}
