package com.mssousa.authserver.config.security.jackson;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Mixins Jackson para os Value Objects que compõem {@code AuthenticatedUser} — necessários
 * porque {@code JdbcOAuth2AuthorizationService} (seção 7.3 do plano) precisa
 * (des)serializar o principal do usuário autenticado ao persistir {@code OAuth2Authorization}.
 * Ficam fora do pacote {@code domain} de propósito — anotação Jackson nos VOs violaria a
 * regra de "domain sem dependência de framework" (seção 5.1).
 * <p>
 * Cada VO vira um escalar puro no JSON (via {@code @JsonValue}/{@code @JsonCreator}, creator
 * delegante de um único argumento), não um objeto com metadado de tipo — evita qualquer
 * necessidade de allowlist no {@code PolymorphicTypeValidator} para eles.
 * </p>
 */
final class ValueObjectMixins {

    private ValueObjectMixins() {
    }

    abstract static class UserIdMixin {
        @JsonCreator
        static Object of(Long value) {
            return null;
        }

        @JsonValue
        abstract Long value();
    }

    abstract static class TenantIdMixin {
        @JsonCreator
        static Object of(Long value) {
            return null;
        }

        @JsonValue
        abstract Long value();
    }

    abstract static class SystemIdMixin {
        @JsonCreator
        static Object of(Long value) {
            return null;
        }

        @JsonValue
        abstract Long value();
    }

    abstract static class PlatformAdminIdMixin {
        @JsonCreator
        static Object of(Long value) {
            return null;
        }

        @JsonValue
        abstract Long value();
    }

    abstract static class UsernameMixin {
        @JsonCreator
        static Object of(String value) {
            return null;
        }

        @JsonValue
        abstract String value();
    }

    abstract static class EmailMixin {
        @JsonCreator
        static Object of(String value) {
            return null;
        }

        @JsonValue
        abstract String value();
    }
}
