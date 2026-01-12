package org.eclipse.mosaic.app.ts.kosnet_applications.mcs.strategy;

public enum CooperationStrategy {
    EMERGENCY_BRAKE(1.0),
    EMERGENCY_LANE_CHANGE(0.9),
    SLOW_DOWN(0.5),
    SPEED_UP(0.3),
    LANE_CHANGE(0.7),
    MAINTAIN(0.0);
    
    private final double effortCost;
    
    CooperationStrategy(double effortCost) {
        this.effortCost = effortCost;
    }
    
    public double getEffortCost() {
        return effortCost;
    }
}