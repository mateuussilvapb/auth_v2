package com.mssousa.authserver.adapter.out.email;

import com.mssousa.authserver.application.port.out.EmailSenderPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class SmtpEmailSender implements EmailSenderPort {

    private final JavaMailSender mailSender;
    private final String sender;

    public SmtpEmailSender(JavaMailSender mailSender, @Value("${authserver.email.sender}") String sender) {
        this.mailSender = mailSender;
        this.sender = sender;
    }

    @Override
    public void sendWelcomeEmail(String toEmail, String recipientName) {
        send(toEmail, "Bem-vindo(a)", "Olá " + recipientName + ", seu cadastro foi realizado com sucesso.");
    }

    @Override
    public void sendPasswordResetEmail(String toEmail, String recipientName, String resetLink) {
        send(toEmail, "Redefinição de senha",
                "Olá " + recipientName + ", clique no link a seguir para redefinir sua senha: " + resetLink
                        + "\n\nSe você não solicitou essa redefinição, ignore este e-mail.");
    }

    private void send(String toEmail, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(sender);
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }
}
