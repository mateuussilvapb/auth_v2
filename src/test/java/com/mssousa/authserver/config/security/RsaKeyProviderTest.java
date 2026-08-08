package com.mssousa.authserver.config.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.env.MockEnvironment;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class RsaKeyProviderTest {

    @Test
    void deveGerarChaveEfemeraQuandoProfileDevENenhumCaminhoConfigurado() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("dev");

        RsaKeyProvider provider = new RsaKeyProvider("", environment);

        assertNotNull(provider.publicKey());
        assertNotNull(provider.privateKey());
        assertEquals(2048, provider.publicKey().getModulus().bitLength());
    }

    @Test
    void deveLancarExcecaoQuandoSemCaminhoEForaDoProfileDev() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        assertThrows(IllegalStateException.class, () -> new RsaKeyProvider("", environment));
    }

    @Test
    void deveCarregarChaveDeArquivoPemEDerivarChavePublicaFuncional(@TempDir Path tempDir) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair generated = generator.generateKeyPair();

        String pem = "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(generated.getPrivate().getEncoded())
                + "\n-----END PRIVATE KEY-----\n";
        Path keyFile = tempDir.resolve("private.pem");
        Files.writeString(keyFile, pem);

        RsaKeyProvider provider = new RsaKeyProvider(keyFile.toString(), new MockEnvironment());

        assertEquals(generated.getPrivate(), provider.privateKey());

        byte[] data = "dados de teste".getBytes();
        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(provider.privateKey());
        signer.update(data);
        byte[] signature = signer.sign();

        Signature verifier = Signature.getInstance("SHA256withRSA");
        verifier.initVerify(provider.publicKey());
        verifier.update(data);
        assertTrue(verifier.verify(signature));
    }
}
