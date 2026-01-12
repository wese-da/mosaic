package org.eclipse.mosaic.app.ts.kosnet_applications.mcs.trajectory.conflict;

public enum ConflictSeverity {
	CRITICAL,   // Immediate action required (TTC < 2s)
	WARNING,    // Cooperation recommended (TTC < 4s)
	NOTICE      // Awareness (TTC > 4s but distance close)
}
