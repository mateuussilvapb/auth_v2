package com.mssousa.authserver.adapter.out.security.oauth2;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

import static org.junit.jupiter.api.Assertions.*;

class CompositeRegisteredClientRepositoryTest {

    private RegisteredClient client(String id) {
        return RegisteredClient.withId(id).clientId(id)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("https://example.com/callback")
                .build();
    }

    private RegisteredClientRepository stub(RegisteredClient... clients) {
        return new RegisteredClientRepository() {
            @Override
            public void save(RegisteredClient registeredClient) {
                throw new UnsupportedOperationException();
            }

            @Override
            public RegisteredClient findById(String id) {
                for (RegisteredClient c : clients) {
                    if (c.getId().equals(id)) return c;
                }
                return null;
            }

            @Override
            public RegisteredClient findByClientId(String clientId) {
                for (RegisteredClient c : clients) {
                    if (c.getClientId().equals(clientId)) return c;
                }
                return null;
            }
        };
    }

    @Test
    void deveRetornarPrimeiroDelegateQueResolverOClient() {
        RegisteredClientRepository first = stub(client("console"));
        RegisteredClientRepository second = stub(client("1"));
        CompositeRegisteredClientRepository composite = new CompositeRegisteredClientRepository(first, second);

        assertNotNull(composite.findByClientId("console"));
        assertNotNull(composite.findById("console"));
    }

    @Test
    void deveConsultarProximoDelegateQuandoOPrimeiroNaoResolve() {
        RegisteredClientRepository first = stub(client("console"));
        RegisteredClientRepository second = stub(client("1"));
        CompositeRegisteredClientRepository composite = new CompositeRegisteredClientRepository(first, second);

        RegisteredClient found = composite.findByClientId("1");
        assertNotNull(found);
        assertEquals("1", found.getClientId());
    }

    @Test
    void deveRetornarNuloQuandoNenhumDelegateResolve() {
        CompositeRegisteredClientRepository composite =
                new CompositeRegisteredClientRepository(stub(), stub());

        assertNull(composite.findByClientId("desconhecido"));
        assertNull(composite.findById("desconhecido"));
    }

    @Test
    void deveLancarExcecaoAoTentarSalvar() {
        CompositeRegisteredClientRepository composite = new CompositeRegisteredClientRepository(stub());
        assertThrows(UnsupportedOperationException.class, () -> composite.save(client("x")));
    }
}
