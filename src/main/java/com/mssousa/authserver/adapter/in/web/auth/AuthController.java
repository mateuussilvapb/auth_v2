package com.mssousa.authserver.adapter.in.web.auth;

import com.mssousa.authserver.adapter.in.web.security.ClientAwareAuthenticationToken;
import com.mssousa.authserver.application.exception.AuthenticationFailedException;
import com.mssousa.authserver.application.model.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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

    public AuthController(AuthenticationManager authenticationManager,
                           SecurityContextRepository securityContextRepository) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
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

        return LoginResponse.from((AuthenticatedUser) authenticated.getPrincipal());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationFailure() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(AuthenticationFailedException.GENERIC_MESSAGE));
    }
}
