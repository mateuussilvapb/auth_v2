package com.mssousa.authserver.adapter.in.web.auth;

import com.mssousa.authserver.adapter.in.web.security.ClientAwareAuthenticationToken;
import com.mssousa.authserver.application.exception.AuthenticationFailedException;
import com.mssousa.authserver.application.exception.ResourceNotFoundException;
import com.mssousa.authserver.application.model.AuthenticatedPlatformAdmin;
import com.mssousa.authserver.application.model.AuthenticatedUser;
import com.mssousa.authserver.application.port.in.GetTenantBrandingUseCase;
import com.mssousa.authserver.application.port.in.ResetPasswordUseCase;
import com.mssousa.authserver.domain.exception.DomainException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints públicos de autenticação consumidos pela SPA Angular (seção 2.2 e 7.1 do
 * plano) — baseados em sessão, não são endpoints OAuth2. Depois de
 * {@code POST /api/auth/login} bem-sucedido, o Angular redireciona de volta para
 * {@code GET /oauth2/authorize}, que agora sucede porque a sessão já tem o
 * {@code Authentication} salvo.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final GetTenantBrandingUseCase getTenantBrandingUseCase;
    private final ResetPasswordUseCase resetPasswordUseCase;
    private final RegisteredClientRepository registeredClientRepository;
    private final OAuth2AuthorizationConsentService oauth2AuthorizationConsentService;

    public AuthController(AuthenticationManager authenticationManager,
                           SecurityContextRepository securityContextRepository,
                           GetTenantBrandingUseCase getTenantBrandingUseCase,
                           ResetPasswordUseCase resetPasswordUseCase,
                           RegisteredClientRepository registeredClientRepository,
                           OAuth2AuthorizationConsentService oauth2AuthorizationConsentService) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.getTenantBrandingUseCase = getTenantBrandingUseCase;
        this.resetPasswordUseCase = resetPasswordUseCase;
        this.registeredClientRepository = registeredClientRepository;
        this.oauth2AuthorizationConsentService = oauth2AuthorizationConsentService;
    }

    @GetMapping("/branding")
    public BrandingResponse branding(@RequestParam String clientId) {
        return BrandingResponse.from(getTenantBrandingUseCase.resolveByClientId(clientId));
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request,
                                HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        Authentication unauthenticated = ClientAwareAuthenticationToken.unauthenticated(
                request.clientId(), request.usernameOrEmail(), request.password());

        Authentication authenticated = authenticationManager.authenticate(unauthenticated);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authenticated);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, httpRequest, httpResponse);

        // Duas identidades caem no mesmo endpoint (seção 2.1/2.2/D6): usuário de tenant
        // (ClientAwareAuthenticationToken/AuthenticatedUser) e platform admin do console
        // administrativo Angular (fallback do ProviderManager para
        // PlatformAdminAuthenticationProvider/AuthenticatedPlatformAdmin — client_id não
        // resolve nenhum System).
        Object principal = authenticated.getPrincipal();
        if (principal instanceof AuthenticatedPlatformAdmin authenticatedPlatformAdmin) {
            return LoginResponse.from(authenticatedPlatformAdmin);
        }
        return LoginResponse.from((AuthenticatedUser) principal);
    }

    @PostMapping("/forgot-password")
    public void forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        resetPasswordUseCase.requestReset(request.clientId(), request.usernameOrEmail());
    }

    @PostMapping("/reset-password")
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        resetPasswordUseCase.confirmReset(request.token(), request.newPassword());
    }

    /**
     * Grava a decisão de consentimento do usuário (seção 2.2/9 do plano — clients de
     * terceiro, {@code System.thirdParty=true}). Não repete a requisição a
     * {@code GET /oauth2/authorize} — quem faz isso é o Angular, navegando o browser de
     * volta para lá após esta chamada suceder; nesse retorno, o Authorization Server
     * encontra o consentimento já registrado e emite o {@code code} normalmente.
     */
    @PostMapping("/consent")
    public void consent(@Valid @RequestBody ConsentRequest request) {
        RegisteredClient registeredClient = registeredClientRepository.findByClientId(request.clientId());
        if (registeredClient == null) {
            throw new ResourceNotFoundException("Client desconhecido: " + request.clientId());
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new InsufficientAuthenticationException("Sessão não autenticada");
        }

        OAuth2AuthorizationConsent.Builder consent =
                OAuth2AuthorizationConsent.withId(registeredClient.getId(), authentication.getName());
        request.scopes().forEach(scope -> consent.authority(new SimpleGrantedAuthority("SCOPE_" + scope)));

        oauth2AuthorizationConsentService.save(consent.build());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationFailure() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(AuthenticationFailedException.GENERIC_MESSAGE));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> handleDomainException(DomainException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(exception.getMessage()));
    }
}
