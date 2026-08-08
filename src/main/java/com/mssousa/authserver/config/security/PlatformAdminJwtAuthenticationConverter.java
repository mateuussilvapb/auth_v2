package com.mssousa.authserver.config.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Converte o claim {@code platform_admin} (seção 7.2/9 do plano) na authority
 * {@code ROLE_PLATFORM_ADMIN} — usado pelo resource server que protege
 * {@code /admin/api/**}. Tokens de usuário de tenant (sem esse claim) não recebem
 * nenhuma authority daqui, então nunca passam no {@code hasAuthority(...)}.
 */
@Component
public class PlatformAdminJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final String PLATFORM_ADMIN_CLAIM = "platform_admin";
    private static final String ROLE_PLATFORM_ADMIN = "ROLE_PLATFORM_ADMIN";

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        List<GrantedAuthority> authorities = Boolean.TRUE.equals(jwt.getClaimAsBoolean(PLATFORM_ADMIN_CLAIM))
                ? List.of(new SimpleGrantedAuthority(ROLE_PLATFORM_ADMIN))
                : List.of();
        return new JwtAuthenticationToken(jwt, authorities);
    }
}
