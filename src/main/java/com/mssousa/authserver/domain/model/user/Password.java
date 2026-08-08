package com.mssousa.authserver.domain.model.user;

import com.mssousa.authserver.domain.exception.DomainException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Objects;

/**
 * Value Object representando uma senha com hash BCrypt (custo 12).
 * Reaproveitado também pelo PlatformAdmin.
 */
public final class Password {

    public static final String DEFAULT_ERROR_PASSWORD = "Senha não pode ser nula ou vazia";
    public static final String DEFAULT_ERROR_PASSWORD_MIN_LENGTH = "Senha deve ter pelo menos 8 caracteres";

    private static final int MIN_LENGTH = 8;
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder(12);

    private final String hashedValue;

    private Password(String hashedValue) {
        this.hashedValue = hashedValue;
    }

    /**
     * Cria um Password a partir de uma senha em texto plano.
     * A senha será validada e convertida em hash BCrypt.
     *
     * @param plainPassword senha em texto plano
     * @return Password com hash da senha
     * @throws DomainException se a senha for inválida
     */
    public static Password fromPlainText(String plainPassword) {
        validatePlainPassword(plainPassword);
        String hash = ENCODER.encode(plainPassword);
        return new Password(hash);
    }

    /**
     * Reconstrói um Password a partir de um hash existente.
     * Usado para carregar senhas já armazenadas no banco de dados.
     *
     * @param hashedPassword hash BCrypt da senha
     * @return Password com o hash fornecido
     * @throws DomainException se o hash for inválido
     */
    public static Password fromHash(String hashedPassword) {
        if (hashedPassword == null || hashedPassword.isBlank()) {
            throw new DomainException(DEFAULT_ERROR_PASSWORD);
        }
        return new Password(hashedPassword);
    }

    private static void validatePlainPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.isBlank()) {
            throw new DomainException(DEFAULT_ERROR_PASSWORD);
        }

        if (plainPassword.length() < MIN_LENGTH) {
            throw new DomainException(DEFAULT_ERROR_PASSWORD_MIN_LENGTH);
        }
    }

    /**
     * Verifica se a senha em texto plano corresponde ao hash armazenado.
     *
     * @param plainPassword senha em texto plano para verificar
     * @return true se a senha corresponder, false caso contrário
     */
    public boolean matches(String plainPassword) {
        if (plainPassword == null) {
            return false;
        }
        return ENCODER.matches(plainPassword, hashedValue);
    }

    /**
     * Retorna o hash BCrypt da senha.
     * Deve ser usado apenas para persistência.
     *
     * @return hash BCrypt da senha
     */
    public String hashedValue() {
        return hashedValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Password password = (Password) o;
        return hashedValue.equals(password.hashedValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hashedValue);
    }

    // Não implementar toString() por questões de segurança
}
