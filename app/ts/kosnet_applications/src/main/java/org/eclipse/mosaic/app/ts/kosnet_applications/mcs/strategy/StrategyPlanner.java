package org.eclipse.mosaic.app.ts.kosnet_applications.mcs.strategy;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.mosaic.app.ts.kosnet_applications.mcs.trajectory.TrajectoryGenerator;
import org.eclipse.mosaic.app.ts.kosnet_applications.mcs.trajectory.conflict.ConflictAnalysis;
import org.eclipse.mosaic.fed.application.app.api.os.VehicleOperatingSystem;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.FrenetTrajectory;

public class StrategyPlanner {
    private VehicleOperatingSystem os;
    
    public StrategyPlanner(VehicleOperatingSystem os) {
        this.os = os;
    }
    
    public List<StrategyCandidate> generateCandidates(ConflictAnalysis analysis,
                                               FrenetTrajectory currentTrajectory) {
        List<StrategyCandidate> candidates = new ArrayList<>();
        
        // Emergency strategies for critical conflicts
        if (!analysis.getCriticalConflicts().isEmpty()) {
            candidates.add(createCandidate(CooperationStrategy.EMERGENCY_BRAKE));
            candidates.add(createCandidate(CooperationStrategy.EMERGENCY_LANE_CHANGE));
        }
        
        // Cooperative strategies for warnings
        if (!analysis.getWarningConflicts().isEmpty()) {
            candidates.add(createCandidate(CooperationStrategy.SLOW_DOWN));
            candidates.add(createCandidate(CooperationStrategy.SPEED_UP));
            candidates.add(createCandidate(CooperationStrategy.LANE_CHANGE));
        }
        
        // Always include maintain option
        candidates.add(createCandidate(CooperationStrategy.MAINTAIN));
        
        return candidates;
    }
    
    private StrategyCandidate createCandidate(CooperationStrategy strategy) {
        StrategyCandidate candidate = new StrategyCandidate();
        candidate.setStrategy(strategy);
        
        // Generate resulting trajectory
        try {
            candidate.setResultingTrajectory(generateTrajectoryForStrategy(strategy));
        } catch (Exception e) {
            candidate.setResultingTrajectory(null);
        }
        
        return candidate;
    }
    
    private FrenetTrajectory generateTrajectoryForStrategy(
            CooperationStrategy strategy) {
        
        switch (strategy) {
            case EMERGENCY_BRAKE:
            case SLOW_DOWN:
                return TrajectoryGenerator.generateTrajectoryWithAcceleration(os, os.getSimulationTime(), -5.0);
                
            case SPEED_UP:
                return TrajectoryGenerator.generateTrajectoryWithAcceleration(
                    os, os.getSimulationTime(), 2.0
                );
                
            case EMERGENCY_LANE_CHANGE:
            case LANE_CHANGE:
            case MAINTAIN:
            default:
                return TrajectoryGenerator.generateTrajectory(
                    os, os.getSimulationTime()
                );
        }
    }
}