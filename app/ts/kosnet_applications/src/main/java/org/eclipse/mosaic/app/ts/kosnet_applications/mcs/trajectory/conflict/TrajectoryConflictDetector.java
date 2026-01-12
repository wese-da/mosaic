package org.eclipse.mosaic.app.ts.kosnet_applications.mcs.trajectory.conflict;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.FrenetTrajectory;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.TrajectoryState;

public class TrajectoryConflictDetector {


    // Configuration parameters
    private double safetyDistance;      // Minimum safe distance (meters)
    private double vehicleLength;       // Average vehicle length (meters)
    private double vehicleWidth;        // Average vehicle width (meters)
    private double samplingInterval;    // Time sampling interval (seconds)
    private double criticalTTC;         // Critical TTC threshold (seconds)
    private double warningTTC;          // Warning TTC threshold (seconds)
    
    public TrajectoryConflictDetector() {
    	this(2.0, 5.0, 2.0, 0.1, 2.0, 4.0);
    }
    
    public TrajectoryConflictDetector(double safetyDistance, double vehicleLength,
        double vehicleWidth, double samplingInterval,
        double criticalTTC, double warningTTC) {
    	this.safetyDistance = safetyDistance;
    	this.vehicleLength = vehicleLength;
    	this.vehicleWidth = vehicleWidth;
    	this.samplingInterval = samplingInterval;
    	this.criticalTTC = criticalTTC;
    	this.warningTTC = warningTTC;
    }
    
    public List<Conflict> detectConflicts(FrenetTrajectory trajectoryA,
            FrenetTrajectory trajectoryB) {
    	List<Conflict> conflicts = new ArrayList<>();

		// Step 1: Check temporal overlap (early exit optimization)
		if (!hasTemporalOverlap(trajectoryA, trajectoryB)) {
			return conflicts; // No overlap in time → no conflict
		}

		// Step 2: Determine time range to check
		double startTime = Math.max(trajectoryA.getStartTime(), 
		      trajectoryB.getStartTime());
		double endTime = Math.min(trajectoryA.getEndTime(), 
		    trajectoryB.getEndTime());

		// Step 3: Sample trajectories at regular intervals
		List<ConflictSample> samples = new ArrayList<>();
		
		for (double t = startTime; t <= endTime; t += samplingInterval) {
			try {
				TrajectoryState stateA = trajectoryA.evaluateAt(t);
				TrajectoryState stateB = trajectoryB.evaluateAt(t);
				
				// Calculate 2D distance in Frenet space
				double distance = calculate2DDistance(stateA, stateB);
				
				// Check if this is a conflict
				double criticalDistance = safetyDistance + vehicleLength;
				if (distance < criticalDistance) {
					// Calculate TTC
					double ttc = calculateTTC(stateA, stateB);
					
					ConflictSample sample = new ConflictSample(t, distance, ttc, stateA, stateB);
					samples.add(sample);
				}
			} catch (IllegalArgumentException e) {
				// Time outside valid range for one trajectory
				continue;
			}
		}

		// Step 4: Group consecutive samples into conflict periods
		conflicts = groupIntoConflictPeriods(samples);
		
		return conflicts;
	}

	/**
	* Check if trajectories overlap in time
	*/
	private boolean hasTemporalOverlap(FrenetTrajectory trajA, FrenetTrajectory trajB) {
		double startA = trajA.getStartTime();
		double endA = trajA.getEndTime();
		double startB = trajB.getStartTime();
		double endB = trajB.getEndTime();
		
		// No overlap if one ends before the other starts
		return !(endA < startB || endB < startA);
	}
	
	/**
	* Calculate 2D Euclidean distance between two trajectory states
	*/
	private double calculate2DDistance(TrajectoryState stateA, TrajectoryState stateB) {
		double dx = stateA.getX() - stateB.getX();
		double dy = stateA.getY() - stateB.getY();
		return Math.sqrt(dx * dx + dy * dy);
	}

	/**
	* Calculate Time-To-Collision (TTC)
	* 
	* TTC = longitudinal_distance / relative_velocity
	* 
	* Positive TTC: B is ahead of A and A is catching up
	* Negative TTC: A is ahead of B and B is catching up
	* Infinity: Same velocity (parallel driving)
	*/
	private double calculateTTC(TrajectoryState stateA, TrajectoryState stateB) {
		double dx = stateB.getX() - stateA.getX();
		double dv = stateA.getVelocity() - stateB.getVelocity();
		
		if (Math.abs(dv) < 0.01) {
			return Double.POSITIVE_INFINITY; // Same velocity
		}
		
		double ttc = dx / dv;
		
		// Only return positive TTC (when catching up)
		return ttc > 0 ? ttc : Double.POSITIVE_INFINITY;
	}

	/**
	* Group consecutive conflict samples into conflict periods
	*/
	private List<Conflict> groupIntoConflictPeriods(List<ConflictSample> samples) {
		List<Conflict> conflicts = new ArrayList<>();
		
		if (samples.isEmpty()) {
			return conflicts;
		}
		
		// Start first conflict
		Conflict currentConflict = new Conflict();
		currentConflict.startTime = samples.get(0).time;
		currentConflict.samples.add(samples.get(0));
		
		for (int i = 1; i < samples.size(); i++) {
			ConflictSample sample = samples.get(i);
			ConflictSample prevSample = samples.get(i - 1);
			
			// Check if this sample is consecutive (within 2x sampling interval)
			if (sample.time - prevSample.time <= 2 * samplingInterval) {
				// Continue current conflict
				currentConflict.samples.add(sample);
			} else {
				// End current conflict and start new one
				currentConflict.endTime = prevSample.time;
				currentConflict.computeMetrics();
				conflicts.add(currentConflict);
				
				currentConflict = new Conflict();
				currentConflict.startTime = sample.time;
				currentConflict.samples.add(sample);
			}
		}

		// Add final conflict
		currentConflict.endTime = samples.get(samples.size() - 1).time;
		currentConflict.computeMetrics();
		conflicts.add(currentConflict);
		
		// Classify conflicts by severity
		for (Conflict conflict : conflicts) {
			conflict.severity = classifySeverity(conflict);
		}
		
		return conflicts;
	}

	/**
	* Classify conflict severity based on TTC and distance
	*/
	private ConflictSeverity classifySeverity(Conflict conflict) {
		if (conflict.minimumTTC < criticalTTC || conflict.minimumDistance < vehicleLength) {
			return ConflictSeverity.CRITICAL;
		} else if (conflict.minimumTTC < warningTTC) {
			return ConflictSeverity.WARNING;
		} else {
			return ConflictSeverity.NOTICE;
		}
	}

	public double getSafetyDistance() { 
		return safetyDistance;
	}
	
	public void setSafetyDistance(double safetyDistance) { 
		this.safetyDistance = safetyDistance; 
	}

	public double getSamplingInterval() {
		return samplingInterval;
	}

	public void setSamplingInterval(double samplingInterval) { 
		this.samplingInterval = samplingInterval; 
	}
}

/**
* Represents a single sample point where conflict is detected
*/
class ConflictSample {
	double time;                    // Time of conflict
	double distance;                // 2D distance between vehicles
	double ttc;                     // Time-to-collision
	TrajectoryState stateA;         // State of vehicle A
	TrajectoryState stateB;         // State of vehicle B
	
	ConflictSample(double time, double distance, double ttc,
	TrajectoryState stateA, TrajectoryState stateB) {
		this.time = time;
		this.distance = distance;
		this.ttc = ttc;
		this.stateA = stateA;
		this.stateB = stateB;
	}
}

	