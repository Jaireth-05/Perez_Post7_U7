package com.tienda.pedidos.service;

public interface EmailService {
    void enviar(String destinatario, String asunto, String cuerpo);
}
