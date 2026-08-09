package com.mssousa.authserver.config.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

/**
 * Redireciona para {@code /login} preservando a query string original de
 * {@code GET /oauth2/authorize} (seção 2.2 do plano: "preservando a URL original") — o
 * {@link LoginUrlAuthenticationEntryPoint} padrão descarta a query string, e sem
 * {@code client_id}/{@code redirect_uri}/os parâmetros PKCE, a SPA Angular não tem como
 * resolver o branding do tenant nem retomar {@code GET /oauth2/authorize} depois do login.
 */
class SpaLoginAuthenticationEntryPoint extends LoginUrlAuthenticationEntryPoint {

    SpaLoginAuthenticationEntryPoint(String loginFormUrl) {
        super(loginFormUrl);
    }

    @Override
    protected String determineUrlToUseForThisRequest(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) {
        String queryString = request.getQueryString();
        return queryString == null ? getLoginFormUrl() : getLoginFormUrl() + "?" + queryString;
    }
}
