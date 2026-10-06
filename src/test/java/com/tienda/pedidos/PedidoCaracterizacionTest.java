package com.tienda.pedidos;

import com.tienda.pedidos.dto.ItemPedido;
import com.tienda.pedidos.dto.PedidoRequest;
import com.tienda.pedidos.dto.ResultadoPedido;
import com.tienda.pedidos.service.EmailService;
import com.tienda.pedidos.service.GestorPedidos;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.verify;

/**
 * Pruebas de caracterización: fijan el comportamiento observable de procesarPedido()
 * ANTES de refactorizar. Deben pasar igual con el diseño original y con el refactorizado.
 * Cada prueba corre en una transacción que se revierte, así los datos semilla no se contaminan.
 *
 * Datos semilla (data.sql): cliente 1 VIP, 2 FRECUENTE (4 pedidos previos), 3 MOROSO (deuda 300000),
 * 4 ESTANDAR, 5 ESTANDAR con NIT, 6 FRECUENTE (11 previos), 7 sin tipo.
 * Productos: 1 = 100000, 2 = 250000, 3 = 50000 (stock 5).
 */
@SpringBootTest
@Transactional
class PedidoCaracterizacionTest {

    private static final double DELTA = 0.01;

    @Autowired GestorPedidos gestor;
    @Autowired JdbcTemplate jdbc;
    @MockBean EmailService emailService;

    private static ItemPedido item(long productoId, int cantidad) {
        ItemPedido i = new ItemPedido();
        i.setProductoId(productoId);
        i.setCantidad(cantidad);
        return i;
    }

    private static PedidoRequest pedido(long clienteId, ItemPedido... items) {
        PedidoRequest r = new PedidoRequest();
        r.setClienteId(clienteId);
        r.setClienteEmail("cliente" + clienteId + "@tienda.com");
        r.setItems(List.of(items));
        return r;
    }

    // ---------- Rutas de validación ----------

    @Test
    void pedidoSinItemsEsRechazado() {
        PedidoRequest r = pedido(1);
        ResultadoPedido res = gestor.procesarPedido(r);
        assertFalse(res.isConfirmado());
        assertEquals("El pedido no contiene items", res.getMotivoRechazo());
    }

    @Test
    void stockInsuficienteEsRechazado() {
        ResultadoPedido res = gestor.procesarPedido(pedido(1, item(3, 10)));
        assertFalse(res.isConfirmado());
        assertEquals("Stock insuficiente: producto 3", res.getMotivoRechazo());
    }

    @Test
    void clienteInexistenteLanzaExcepcionDeAcceso() {
        // Comportamiento heredado: queryForObject lanza excepcion cuando NO hay fila,
        // por lo que el mensaje "Cliente no registrado" es inalcanzable para un id inexistente.
        assertThrows(EmptyResultDataAccessException.class,
            () -> gestor.procesarPedido(pedido(99, item(1, 1))));
    }

    @Test
    void clienteSinTipoEsRechazadoComoNoRegistrado() {
        ResultadoPedido res = gestor.procesarPedido(pedido(7, item(1, 1)));
        assertFalse(res.isConfirmado());
        assertEquals("Cliente no registrado", res.getMotivoRechazo());
    }

    @Test
    void clienteMorosoDependeDelHorarioDeCorte() {
        // La regla usa LocalTime.now(): antes de las 20:00 se rechaza, despues se permite.
        boolean antesDelCorte = LocalTime.now().isBefore(LocalTime.of(20, 0));
        ResultadoPedido res = gestor.procesarPedido(pedido(3, item(1, 1)));
        if (antesDelCorte) {
            assertFalse(res.isConfirmado());
            assertEquals("Cliente con deuda pendiente: $300000.0", res.getMotivoRechazo());
        } else {
            assertTrue(res.isConfirmado());
            assertEquals(119_000.0, res.getTotal(), DELTA); // sin descuento: 100000 + 19% IVA
        }
    }

    // ---------- Descuentos por tipo de cliente ----------

    @Test
    void vipConSubtotalMedioRecibe10PorCiento() {
        // 3 x 250000 = 750000 ; desc 10% ; base 675000 ; IVA 128250 ; total 803250
        ResultadoPedido res = gestor.procesarPedido(pedido(1, item(2, 3)));
        assertTrue(res.isConfirmado());
        assertEquals(803_250.0, res.getTotal(), DELTA);

        // Persistencia: pedido guardado y stock descontado (500 -> 497)
        Integer stock = jdbc.queryForObject("SELECT stock FROM inventario WHERE producto_id = 2", Integer.class);
        assertEquals(497, stock);
        Integer detalles = jdbc.queryForObject(
            "SELECT COUNT(*) FROM detalle_pedido WHERE pedido_id = ?", Integer.class, res.getPedidoId());
        assertEquals(1, detalles);

        // Notificación
        ArgumentCaptor<String> cuerpo = ArgumentCaptor.forClass(String.class);
        verify(emailService).enviar(eq("cliente1@tienda.com"),
            startsWith("Confirmacion de pedido #"), cuerpo.capture());
        assertTrue(cuerpo.getValue().contains("Descuento aplicado: 10%"));
    }

    @Test
    void vipConSubtotalAltoRecibe15PorCiento() {
        // 5 x 250000 = 1250000 ; desc 15% ; base 1062500 ; IVA 201875 ; total 1264375
        ResultadoPedido res = gestor.procesarPedido(pedido(1, item(2, 5)));
        assertTrue(res.isConfirmado());
        assertEquals(1_264_375.0, res.getTotal(), DELTA);
    }

    @Test
    void vipConSubtotalBajoRecibe5PorCiento() {
        // 2 x 100000 = 200000 ; desc 5% ; base 190000 ; IVA 36100 ; total 226100
        ResultadoPedido res = gestor.procesarPedido(pedido(1, item(1, 2)));
        assertTrue(res.isConfirmado());
        assertEquals(226_100.0, res.getTotal(), DELTA);
    }

    @Test
    void frecuenteCon4PedidosPreviosRecibe4PorCiento() {
        // 10 x 100000 = 1000000 ; desc 4% ; base 960000 ; IVA 182400 ; total 1142400
        ResultadoPedido res = gestor.procesarPedido(pedido(2, item(1, 10)));
        assertTrue(res.isConfirmado());
        assertEquals(1_142_400.0, res.getTotal(), DELTA);
    }

    @Test
    void frecuenteCon11PedidosPreviosRecibe8PorCiento() {
        // 10 x 100000 = 1000000 ; desc 8% ; base 920000 ; IVA 174800 ; total 1094800
        ResultadoPedido res = gestor.procesarPedido(pedido(6, item(1, 10)));
        assertTrue(res.isConfirmado());
        assertEquals(1_094_800.0, res.getTotal(), DELTA);
    }

    @Test
    void clienteEstandarNoRecibeDescuento() {
        // 1 x 100000 ; sin descuento ; IVA 19000 ; total 119000
        ResultadoPedido res = gestor.procesarPedido(pedido(4, item(1, 1)));
        assertTrue(res.isConfirmado());
        assertEquals(119_000.0, res.getTotal(), DELTA);
    }
}
