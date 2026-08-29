package com.mssousa.authserver.adapter.out.id;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TsidGeneratorTest {

    private final TsidGenerator generator = new TsidGenerator();

    @Test
    void deveGerarIdPositivo() {
        assertTrue(generator.generate() > 0);
    }

    @Test
    void deveGerarIdsUnicos() {
        Set<Long> ids = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            ids.add(generator.generate());
        }
        assertEquals(1000, ids.size());
    }

    @Test
    void idsGeradosDevemSerCrescentes() {
        long first = generator.generate();
        long second = generator.generate();
        assertTrue(second > first);
    }
}
