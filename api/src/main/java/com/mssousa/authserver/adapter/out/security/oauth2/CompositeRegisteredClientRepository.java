package com.mssousa.authserver.adapter.out.security.oauth2;

import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

import java.util.List;
import java.util.Objects;

/**
 * Combina múltiplos {@link RegisteredClientRepository} em ordem, retornando o primeiro
 * resultado não nulo. Usado para juntar {@link SystemRegisteredClientRepository}
 * (clients tenant-scoped, baseados na tabela {@code system}) com o {@code RegisteredClient}
 * estático do console administrativo Angular (não vinculado a nenhum tenant — seção 2.2/D6
 * do plano, ver {@code RegisteredClientRepositoryConfig}).
 */
public class CompositeRegisteredClientRepository implements RegisteredClientRepository {

    private final List<RegisteredClientRepository> delegates;

    public CompositeRegisteredClientRepository(RegisteredClientRepository... delegates) {
        this.delegates = List.of(delegates);
    }

    @Override
    public void save(RegisteredClient registeredClient) {
        throw new UnsupportedOperationException(
                "Clients são administrados via ManageSystemUseCase ou configuração estática do console, não pelo Authorization Server");
    }

    @Override
    public RegisteredClient findById(String id) {
        return delegates.stream()
                .map(delegate -> delegate.findById(id))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    @Override
    public RegisteredClient findByClientId(String clientId) {
        return delegates.stream()
                .map(delegate -> delegate.findByClientId(clientId))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }
}
