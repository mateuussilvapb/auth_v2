package com.mssousa.authserver.adapter.in.web.common;

import java.util.Map;

/**
 * Corpo de erro padrão da API administrativa (seção 9 do plano). {@code fieldErrors} só é
 * preenchido para falhas de Bean Validation ({@code 400}); {@code null} nos demais casos.
 */
public record ApiErrorResponse(String message, Map<String, String> fieldErrors) {

    public static ApiErrorResponse of(String message) {
        return new ApiErrorResponse(message, null);
    }

    public static ApiErrorResponse of(String message, Map<String, String> fieldErrors) {
        return new ApiErrorResponse(message, fieldErrors);
    }
}
