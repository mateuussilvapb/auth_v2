package com.mssousa.authserver.adapter.out.persistence.adapter;

import com.mssousa.authserver.adapter.out.persistence.entity.UserEntity;
import com.mssousa.authserver.adapter.out.persistence.mapper.AuthMapper;
import com.mssousa.authserver.adapter.out.persistence.repository.PasswordResetTokenJpaRepository;
import com.mssousa.authserver.adapter.out.persistence.repository.UserJpaRepository;
import com.mssousa.authserver.application.port.out.PasswordResetTokenRepository;
import com.mssousa.authserver.domain.model.token.passwordResetToken.PasswordResetToken;
import com.mssousa.authserver.domain.model.token.passwordResetToken.PasswordResetTokenId;
import com.mssousa.authserver.domain.model.token.passwordResetToken.ResetTokenValue;
import com.mssousa.authserver.domain.model.user.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PasswordResetTokenRepositoryImpl implements PasswordResetTokenRepository {

    private final PasswordResetTokenJpaRepository jpaRepository;
    private final UserJpaRepository userJpaRepository;
    private final AuthMapper mapper;

    @Override
    public PasswordResetToken save(PasswordResetToken token) {
        UserEntity user = userJpaRepository.findById(token.getUserId().value())
                .orElseThrow(() -> new IllegalStateException("User não encontrado: " + token.getUserId()));

        return mapper.toDomain(jpaRepository.save(mapper.toEntity(token, user)));
    }

    @Override
    public Optional<PasswordResetToken> findById(PasswordResetTokenId id) {
        return jpaRepository.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public Optional<PasswordResetToken> findByValue(ResetTokenValue value) {
        return jpaRepository.findByTokenHash(value.value()).map(mapper::toDomain);
    }

    @Override
    public Optional<PasswordResetToken> findByUserId(UserId userId) {
        return jpaRepository.findByUserId(userId.value()).map(mapper::toDomain);
    }

    @Override
    public void deleteById(PasswordResetTokenId id) {
        jpaRepository.deleteById(id.value());
    }
}
