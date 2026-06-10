package org.example.adapter.out.persistence;

import jakarta.persistence.EntityManager;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.example.application.port.out.AdvisoryLockPort;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdvisoryLockAdapter implements AdvisoryLockPort {

    private final EntityManager entityManager;

    @Override
    public void acquireXact(Namespace namespace, UUID entityId) {
        long key = lockKey(namespace, entityId);
        entityManager.createNativeQuery("SELECT pg_advisory_xact_lock(:k)")
                .setParameter("k", key)
                .getSingleResult();
    }

    // Deterministic 64-bit key: namespace in the high byte, XOR of the UUID halves below it.
    private static long lockKey(Namespace namespace, UUID id) {
        return (((long) namespace.ordinal()) << 56)
                ^ id.getMostSignificantBits()
                ^ id.getLeastSignificantBits();
    }
}
