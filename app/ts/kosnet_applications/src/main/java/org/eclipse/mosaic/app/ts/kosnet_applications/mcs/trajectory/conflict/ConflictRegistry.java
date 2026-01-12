package org.eclipse.mosaic.app.ts.kosnet_applications.mcs.trajectory.conflict;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConflictRegistry {
    private Map<String, VehicleConflictPair> conflicts = new HashMap<>();
    
    public void addConflict(String vehicleId, Conflict conflict) {
        conflicts.computeIfAbsent(vehicleId, 
            k -> new VehicleConflictPair(vehicleId))
            .getConflicts().add(conflict);
    }
    
    public boolean hasConflictsWith(String vehicleId) {
        return conflicts.containsKey(vehicleId);
    }
    
    public List<VehicleConflictPair> getAllConflicts() {
        return new ArrayList<>(conflicts.values());
    }
    
    public int getTotalConflictCount() {
        return conflicts.values().stream()
            .mapToInt(p -> p.getConflicts().size())
            .sum();
    }
    
    public int getVehicleCount() {
        return conflicts.size();
    }
    
    public boolean isEmpty() {
        return conflicts.isEmpty();
    }
    
    public void clear() {
        conflicts.clear();
    }
}