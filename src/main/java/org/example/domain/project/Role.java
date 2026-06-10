package org.example.domain.project;

/** Project membership roles, ordered low→high so {@link #atLeast} expresses the RBAC hierarchy. */
public enum Role {
    VIEWER,
    MEMBER,
    PROJECT_LEAD,
    ADMIN;

    public boolean atLeast(Role other) {
        return ordinal() >= other.ordinal();
    }
}
