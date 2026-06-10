package org.example.application.port.out;

import java.util.UUID;

/**
 * Transaction-scoped advisory locking. The lock is held until the surrounding
 * transaction commits or rolls back, so it cannot leak.
 */
public interface AdvisoryLockPort {

    enum Namespace {
        /** Serialises sprint start/complete per sprint. */
        SPRINT,
        /** Serialises moves into a WIP-limited status per (project, status). */
        WIP
    }

    void acquireXact(Namespace namespace, UUID entityId);
}
