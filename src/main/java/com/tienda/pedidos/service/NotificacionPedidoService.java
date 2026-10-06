package com.tienda.pedidos.service;

import com.tienda.pedidos.validacion.ContextoPedido;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NotificacionPedidoService {
    private static final Logger log = LoggerFactory.getLogger(NotificacionPedidoService.class);

    private final EmailService emailService;

    public NotificacionPedidoService(EmailService emailService) { this.emailService = emailService; }

    public void notificarConfirmacion(ContextoPedido contexto, Long pedidoId, double descuento,
                                      double impuesto, double total) {
        String asunto = "Confirmacion de pedido #" + pedidoId;
        String cuerpo = "Estimado cliente,\n\nSu pedido ha sido confirmado.\n"
            + "Subtotal: $" + contexto.getSubtotal() + "\n"
            + (descuento > 0 ? "Descuento aplicado: " + (int) (descuento * 100) + "%\n" : "")
            + "Impuesto: $" + impuesto + "\nTotal: $" + total + "\n";
        try {
            emailService.enviar(contexto.getRequest().getClienteEmail(), asunto, cuerpo);
        } catch (Exception e) {
            // Se continua el flujo aunque falle el envio del correo (igual que el original)
            log.error("No se pudo enviar la notificacion del pedido {}: {}", pedidoId, e.getMessage());
        }
    }
}
