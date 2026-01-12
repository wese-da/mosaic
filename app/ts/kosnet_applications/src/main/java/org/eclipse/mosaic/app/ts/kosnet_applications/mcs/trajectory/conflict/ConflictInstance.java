package org.eclipse.mosaic.app.ts.kosnet_applications.mcs.trajectory.conflict;

public class ConflictInstance {
    private String vehicleId;
    private Conflict conflict;
    
    public ConflictInstance(String vehicleId, Conflict conflict) {
        this.vehicleId = vehicleId;
        this.conflict = conflict;
    }

	public String getVehicleId() {
		return vehicleId;
	}

	public Conflict getConflict() {
		return conflict;
	}

}