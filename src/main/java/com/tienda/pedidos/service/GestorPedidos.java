package com.tienda.pedidos.service;

import com.tienda.pedidos.descuento.CalculadorDescuentoFinal;
import com.tienda.pedidos.dto.ItemPedido;
import com.tienda.pedidos.dto.PedidoRequest;
import com.tienda.pedidos.dto.ResultadoPedido;
import com.tienda.pedidos.validacion.ContextoPedido;
import com.tienda.pedidos.validacion.ValidadorCliente;
import com.tienda.pedidos.validacion.ValidadorPedido;
import com.tienda.pedidos.validacion.ValidadorStock;
import org.springframework.stereotype.Service;

/** Orquestador delgado: la cadena solo contiene validaciones reales; el descuento se delega por completo. */
@Service
public class GestorPedidos {
    private static final double TASA_IVA = 0.19;

    private final ValidadorPedido primerValidador;
    private final CalculadorDescuentoFinal calculadorDescuento;
    private final ProductoRepository productos;
    private final PedidoRepository repository;
    private final NotificacionPedidoService notificacion;

    public GestorPedidos(ValidadorStock stock, ValidadorCliente cliente,
                         CalculadorDescuentoFinal calculadorDescuento, ProductoRepository productos,
                         PedidoRepository repository, NotificacionPedidoService notificacion) {
        stock.encadenar(cliente);
        this.primerValidador = stock;
        this.calculadorDescuento = calculadorDescuento;
        this.productos = productos;
        this.repository = repository;
        this.notificacion = notificacion;
    }

    public ResultadoPedido procesarPedido(PedidoRequest request) {
        ContextoPedido contexto = new ContextoPedido(request);
        primerValidador.validar(contexto);
        if (contexto.isRechazado()) return ResultadoPedido.rechazado(contexto.getMotivoRechazo());

        double subtotal = calcularSubtotal(request);
        contexto.setSubtotal(subtotal);

        double descuento = calculadorDescuento.calcular(contexto);
        double impuesto = (subtotal - subtotal * descuento) * TASA_IVA;
        double total = subtotal - (subtotal * descuento) + impuesto;

        Long pedidoId = repository.guardar(contexto, descuento, impuesto, total);
        notificacion.notificarConfirmacion(contexto, pedidoId, descuento, impuesto, total);
        return ResultadoPedido.confirmado(pedidoId, total);
    }

    private double calcularSubtotal(PedidoRequest request) {
        double subtotal = 0;
        for (ItemPedido item : request.getItems()) {
            subtotal += productos.precioUnitario(item.getProductoId()) * item.getCantidad();
        }
        return subtotal;
    }
}
