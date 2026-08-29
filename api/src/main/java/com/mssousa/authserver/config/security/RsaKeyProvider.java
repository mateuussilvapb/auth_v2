package com.mssousa.authserver.config.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

/**
 * Carrega o par de chaves RSA usado para assinar os tokens (seção 7.3 do plano).
 * <p>
 * Em produção, a chave privada deve vir de um arquivo PEM (PKCS8) num volume persistente
 * ou Secrets Manager — nunca gerada em memória a cada boot, sob pena de invalidar todos
 * os tokens emitidos e o JWKS publicado no redeploy anterior (armadilha da seção 12). A
 * chave pública é derivada da privada (par RSA gerado via {@link KeyPairGenerator}
 * sempre produz uma {@code RSAPrivateCrtKey}, que carrega o expoente público).
 * <p>
 * Só gera uma chave efêmera em memória quando o profile "dev" está ativo e nenhum
 * caminho foi configurado — conveniência para desenvolvimento local, nunca para produção.
 */
@Component
public class RsaKeyProvider {

    private final KeyPair keyPair;

    public RsaKeyProvider(@Value("${authserver.jwt.rsa.private-key-path:}") String privateKeyPath,
                           Environment environment) {
        this.keyPair = privateKeyPath.isBlank()
                ? ephemeralKeyPair(environment)
                : loadFromPem(privateKeyPath);
    }

    public RSAPublicKey publicKey() {
        return (RSAPublicKey) keyPair.getPublic();
    }

    public RSAPrivateKey privateKey() {
        return (RSAPrivateKey) keyPair.getPrivate();
    }

    private KeyPair ephemeralKeyPair(Environment environment) {
        if (!environment.acceptsProfiles(org.springframework.core.env.Profiles.of("dev"))) {
            throw new IllegalStateException(
                    "authserver.jwt.rsa.private-key-path não configurado fora do profile dev. "
                            + "Chave RSA nunca deve ser gerada em memória em produção (seção 12 do plano).");
        }
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo RSA indisponível na JVM", e);
        }
    }

    private KeyPair loadFromPem(String path) {
        try {
            String pem = Files.readString(Path.of(path));
            String base64 = pem
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");

            byte[] decoded = Base64.getDecoder().decode(base64);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            RSAPrivateKey privateKey = (RSAPrivateKey) keyFactory.generatePrivate(new PKCS8EncodedKeySpec(decoded));

            if (!(privateKey instanceof java.security.interfaces.RSAPrivateCrtKey crtKey)) {
                throw new IllegalStateException(
                        "Chave privada RSA em " + path + " não é uma RSAPrivateCrtKey — não é possível derivar a chave pública");
            }

            RSAPublicKey publicKey = (RSAPublicKey) keyFactory.generatePublic(
                    new java.security.spec.RSAPublicKeySpec(crtKey.getModulus(), crtKey.getPublicExponent()));

            return new KeyPair(publicKey, privateKey);
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao ler o arquivo de chave RSA: " + path, e);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Chave RSA inválida em " + path, e);
        }
    }
}
