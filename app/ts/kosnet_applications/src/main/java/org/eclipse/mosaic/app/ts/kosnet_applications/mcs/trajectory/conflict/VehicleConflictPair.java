package org.eclipse.mosaic.app.ts.kosnet_applications.mcs.trajectory.conflict;

import java.util.ArrayList;
import java.util.List;

public class VehicleConflictPair {
    private String vehicleId;
    private List<Conflict> conflicts = new ArrayList<>();
    
    public VehicleConflictPair(String vehicleId) {
        this.vehicleId = vehicleId;
    }
    
    public String getVehicleId() {
    	return vehicleId;
    }
    
    public List<Conflict> getConflicts() {
    	return new ArrayList<Conflict>(conflicts);
    }
}