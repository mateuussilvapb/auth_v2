package com.mssousa.authserver.adapter.out.id;

import com.mssousa.authserver.application.port.out.IdGeneratorPort;
import io.hypersistence.tsid.TSID;
import org.springframework.stereotype.Component;

@Component
public final class TsidGenerator implements IdGeneratorPort {

    private static final TSID.Factory FACTORY = TSID.Factory.builder()
            .withNodeBits(nodeBits(TsidNodeResolver.nodeCount()))
            .withNode(TsidNodeResolver.nodeId())
            .build();

    @Override
    public Long generate() {
        return FACTORY.generate().toLong();
    }

    private static int nodeBits(int nodeCount) {
        return (int) (Math.log(nodeCount) / Math.log(2));
    }
}
