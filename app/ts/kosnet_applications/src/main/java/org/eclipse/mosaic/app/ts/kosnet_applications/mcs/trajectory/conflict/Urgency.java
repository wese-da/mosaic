package org.eclipse.mosaic.app.ts.kosnet_applications.mcs.trajectory.conflict;

public enum Urgency {
    NONE,        // No conflicts
    LOW,         // Only notices
    MODERATE,    // Single warning
    HIGH,        // Multiple warnings
    IMMEDIATE    // Critical conflicts
}