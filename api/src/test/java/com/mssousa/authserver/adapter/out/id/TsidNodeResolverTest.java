package com.mssousa.authserver.adapter.out.id;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TsidNodeResolverTest {

    @AfterEach
    void cleanup() {
        System.clearProperty(TsidNodeResolver.NODE_ID_PROPERTY);
        System.clearProperty(TsidNodeResolver.NODE_COUNT_PROPERTY);
    }

    @Test
    void deveUsarValoresPadraoQuandoPropriedadesNaoDefinidas() {
        assertEquals(0, TsidNodeResolver.nodeId());
        assertEquals(1, TsidNodeResolver.nodeCount());
    }

    @Test
    void deveLerNodeIdDaPropriedadeDeSistema() {
        System.setProperty(TsidNodeResolver.NODE_ID_PROPERTY, "3");
        assertEquals(3, TsidNodeResolver.nodeId());
    }

    @Test
    void deveLerNodeCountDaPropriedadeDeSistema() {
        System.setProperty(TsidNodeResolver.NODE_COUNT_PROPERTY, "4");
        assertEquals(4, TsidNodeResolver.nodeCount());
    }

    @Test
    void deveIgnorarValorInvalidoEUsarPadrao() {
        System.setProperty(TsidNodeResolver.NODE_ID_PROPERTY, "nao-e-um-numero");
        assertEquals(0, TsidNodeResolver.nodeId());
    }
}
