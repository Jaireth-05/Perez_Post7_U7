package com.tienda.pedidos.service;

import org.springframework.stereotype.Service;

/** Implementación de desarrollo: imprime el correo en consola en lugar de usar SMTP. */
@Service
public class ConsoleEmailService implements EmailService {
    @Override
    public void enviar(String destinatario, String asunto, String cuerpo) {
        System.out.println("=== CORREO ===");
        System.out.println("Para: " + destinatario);
        System.out.println("Asunto: " + asunto);
        System.out.println(cuerpo);
        System.out.println("==============");
    }
}
