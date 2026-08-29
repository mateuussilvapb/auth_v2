package com.mssousa.authserver.application.port.out;

/**
 * Porta de saída para geração de identificadores (TSID, seção 6.4 do plano). O ID é
 * sempre gerado na aplicação, antes do insert — as tabelas usam {@code BIGINT PRIMARY KEY},
 * nunca {@code BIGSERIAL}/{@code @GeneratedValue}.
 */
public interface IdGeneratorPort {
    Long generate();
}
