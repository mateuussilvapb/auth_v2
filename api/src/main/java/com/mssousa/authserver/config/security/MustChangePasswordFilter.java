package com.mssousa.authserver.config.security;

import com.mssousa.authserver.application.port.out.PlatformAdminRepository;
import com.mssousa.authserver.domain.model.platform.PlatformAdmin;
import com.mssousa.authserver.domain.model.platform.PlatformAdminId;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Bloqueia qualquer chamada a {@code /admin/api/**} de um platform admin com
 * {@code mustChangePassword=true} no banco, exceto a própria rota de troca de senha —
 * força o platform admin seedado com senha temporária (seção 10, Fase 10, migration
 * {@code V15}) a trocá-la antes de fazer qualquer outra operação administrativa.
 * <p>
 * Consulta o banco a cada requisição em vez de confiar no claim {@code must_change_password}
 * do próprio JWT: o claim é fixado no momento da emissão do token e ficaria obsoleto assim
 * que a senha fosse trocada (o access token de sessão continua válido por até 60 minutos,
 * ver Notas de {@code PROGRESS.md} sobre TTL) — o admin ficaria preso mesmo depois de trocar
 * a senha, até o token expirar e forçar um novo login.
 * </p>
 */
@Component
public class MustChangePasswordFilter extends OncePerRequestFilter {

    static final String CHANGE_PASSWORD_PATH = "/admin/api/v1/platform-admins/me/password";

    private final PlatformAdminRepository platformAdminRepository;

    public MustChangePasswordFilter(PlatformAdminRepository platformAdminRepository) {
        this.platformAdminRepository = platformAdminRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof JwtAuthenticationToken jwtAuthentication
                && !CHANGE_PASSWORD_PATH.equals(request.getRequestURI())
                && mustChangePassword(jwtAuthentication.getToken().getSubject())) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(
                    "{\"message\":\"Troca de senha obrigatória antes de continuar\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean mustChangePassword(String subject) {
        try {
            Optional<PlatformAdmin> admin = platformAdminRepository.findById(PlatformAdminId.of(Long.valueOf(subject)));
            return admin.map(PlatformAdmin::mustChangePassword).orElse(false);
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
