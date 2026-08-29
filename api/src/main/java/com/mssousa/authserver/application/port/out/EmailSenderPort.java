package com.mssousa.authserver.application.port.out;

/**
 * Porta de saída para envio de e-mails transacionais (seção 5 do plano). Cobre os dois
 * casos exigidos pela Fase 4/5: boas-vindas ao criar um usuário e redefinição de senha.
 */
public interface EmailSenderPort {

    void sendWelcomeEmail(String toEmail, String recipientName);

    void sendPasswordResetEmail(String toEmail, String recipientName, String resetLink);
}
