package com.mssousa.authserver.config.security;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Publica a chave pública em {@code /oauth2/jwks} via {@link JWKSource} (seção 7.3). O
 * {@code JwtDecoder} usado pelos resource servers é auto-configurado pelo Spring Boot a
 * partir deste bean — não precisa ser declarado manualmente.
 */
@Configuration
public class JwksConfig {

    private final RsaKeyProvider rsaKeyProvider;

    public JwksConfig(RsaKeyProvider rsaKeyProvider) {
        this.rsaKeyProvider = rsaKeyProvider;
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        RSAKey rsaKey = new RSAKey.Builder(rsaKeyProvider.publicKey())
                .privateKey(rsaKeyProvider.privateKey())
                .keyID(keyId())
                .build();
        JWKSet jwkSet = new JWKSet(rsaKey);
        return new ImmutableJWKSet<>(jwkSet);
    }

    /**
     * kid derivado do módulo da chave pública — estável entre reinicializações enquanto
     * a mesma chave estiver configurada, muda automaticamente se a chave for rotacionada
     * (seção 7.3: "rotação com dois kid ativos durante a transição").
     */
    private String keyId() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rsaKeyProvider.publicKey().getModulus().toByteArray());
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash).substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo SHA-256 indisponível na JVM", e);
        }
    }
}
