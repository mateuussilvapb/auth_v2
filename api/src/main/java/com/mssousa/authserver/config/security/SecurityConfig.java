package com.mssousa.authserver.config.security;

import com.mssousa.authserver.adapter.in.web.security.PlatformAdminAuthenticationProvider;
import com.mssousa.authserver.adapter.in.web.security.UserAuthenticationProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.http.MediaType;

import java.util.Set;

/**
 * Filter chains para {@code /oauth2/**} + OIDC (o servidor de autorização propriamente
 * dito), {@code /admin/api/**} (resource server, exige token de platform admin),
 * {@code /api/auth/**} (público — login/consentimento/esqueci-senha, seção 2.2 e 7.4; a
 * verificação de credenciais acontece dentro do controller via {@code AuthenticationManager},
 * não neste filtro) e a documentação OpenAPI (mesma exigência de acesso do
 * {@code /admin/api/**} que ela documenta).
 * <p>
 * O filter chain de {@code /oauth2/**} precisa ser declarado explicitamente aqui: a
 * autoconfiguração do Spring Boot para o Authorization Server
 * ({@code OAuth2AuthorizationServerWebSecurityConfiguration}) só se ativa via
 * {@code @ConditionalOnMissingBean(SecurityFilterChain.class)} — como este projeto já
 * declara outros {@code SecurityFilterChain} (admin/api, api/auth), a autoconfiguração
 * nunca dispara e {@code /oauth2/authorize} cairia como 404. O corpo do bean abaixo
 * replica exatamente o que a autoconfiguração faria por padrão.
 * </p>
 * <p>
 * CSRF desabilitado nos filter chains de {@code /admin/api/**} e {@code /api/auth/**}:
 * o primeiro é stateless (bearer token, sem cookie de sessão — CSRF não se aplica); o
 * segundo conta com o cookie de sessão {@code SameSite=Lax} (seção 7.4) como defesa —
 * revisar quando o controller de login for implementado (Fase 7) e o formato real do
 * cookie estiver definido.
 * </p>
 */
@Configuration
public class SecurityConfig {

    @Bean
    public AuthenticationManager authenticationManager(UserAuthenticationProvider userAuthenticationProvider,
                                                         PlatformAdminAuthenticationProvider platformAdminAuthenticationProvider) {
        return new ProviderManager(userAuthenticationProvider, platformAdminAuthenticationProvider);
    }

    @Bean
    @Order(0)
    public SecurityFilterChain authorizationServerSecurityFilterChain(
            HttpSecurity http,
            @Value("${authserver.frontend.login-url}") String loginUrl,
            @Value("${authserver.frontend.consent-url}") String consentUrl) throws Exception {
        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer = new OAuth2AuthorizationServerConfigurer();
        // Rota pública do SPA Angular (seção 2.2/9 do plano) — mesmo padrão do /login: o
        // Angular lê client_id/scope/state da URL, chama POST /api/auth/consent com a
        // decisão do usuário, e refaz GET /oauth2/authorize (que já sucede, pois o
        // consentimento foi gravado).
        authorizationServerConfigurer.authorizationEndpoint(endpoint -> endpoint.consentPage(consentUrl));

        http.securityMatcher(authorizationServerConfigurer.getEndpointsMatcher())
                .with(authorizationServerConfigurer, Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated());

        authorizationServerConfigurer.oidc(Customizer.withDefaults());

        http.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                .exceptionHandling(exceptions -> exceptions.defaultAuthenticationEntryPointFor(
                        new SpaLoginAuthenticationEntryPoint(loginUrl), htmlRequestMatcher()))
                .cors(Customizer.withDefaults());

        return http.build();
    }

    private MediaTypeRequestMatcher htmlRequestMatcher() {
        MediaTypeRequestMatcher matcher = new MediaTypeRequestMatcher(MediaType.TEXT_HTML);
        matcher.setIgnoredMediaTypes(Set.of(MediaType.ALL));
        return matcher;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain adminApiSecurityFilterChain(
            HttpSecurity http, PlatformAdminJwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
        http.securityMatcher("/admin/api/**")
                .authorizeHttpRequests(auth -> auth.anyRequest().hasAuthority("ROLE_PLATFORM_ADMIN"))
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable());
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain apiAuthSecurityFilterChain(
            HttpSecurity http, SecurityContextRepository securityContextRepository) throws Exception {
        http.securityMatcher("/api/auth/**")
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .securityContext(context -> context.securityContextRepository(securityContextRepository))
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable());
        return http.build();
    }

    /**
     * A documentação descreve a superfície inteira da API administrativa — mesma
     * exigência de acesso que os endpoints que ela documenta (seção 9 do plano).
     */
    @Bean
    @Order(3)
    public SecurityFilterChain openApiSecurityFilterChain(
            HttpSecurity http, PlatformAdminJwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
        http.securityMatcher("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                .authorizeHttpRequests(auth -> auth.anyRequest().hasAuthority("ROLE_PLATFORM_ADMIN"))
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf.disable());
        return http.build();
    }

    /**
     * Explícito (em vez de depender do default do Spring Security) porque
     * {@link com.mssousa.authserver.adapter.in.web.auth.AuthController} precisa da mesma
     * instância para persistir a sessão após {@code POST /api/auth/login} — a leitura
     * dessa sessão por {@code GET /oauth2/authorize} depende de os dois usarem o mesmo
     * mecanismo (cookie de sessão HTTP, seção 2.2 do plano).
     */
    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }
}
