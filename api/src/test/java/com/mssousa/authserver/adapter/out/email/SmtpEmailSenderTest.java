package com.mssousa.authserver.adapter.out.email;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SmtpEmailSenderTest {

    private JavaMailSender mailSender;
    private SmtpEmailSender emailSender;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);
        emailSender = new SmtpEmailSender(mailSender, "noreply@seudominio.com");
    }

    @Test
    void deveEnviarEmailDeBoasVindasComRemetenteEDestinatarioCorretos() {
        emailSender.sendWelcomeEmail("joao@acme.com", "João da Silva");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage sent = captor.getValue();
        assertEquals("noreply@seudominio.com", sent.getFrom());
        assertArrayEquals(new String[]{"joao@acme.com"}, sent.getTo());
        assertTrue(sent.getText().contains("João da Silva"));
    }

    @Test
    void deveEnviarEmailDeRedefinicaoComOLinkNoCorpo() {
        emailSender.sendPasswordResetEmail("joao@acme.com", "João", "https://auth.seudominio.com/reset?token=abc123");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage sent = captor.getValue();
        assertTrue(sent.getText().contains("https://auth.seudominio.com/reset?token=abc123"));
    }
}
