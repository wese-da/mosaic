package org.eclipse.mosaic.app.ts.kosnet_applications.mcs.trajectory.conflict;

import java.util.ArrayList;
import java.util.List;

/**
* Represents a continuous conflict period
*/
public class Conflict {
	
	double startTime;               // When conflict starts
	double endTime;                 // When conflict ends
	List<ConflictSample> samples;   // All samples in this conflict
	ConflictSeverity severity;      // Severity classification
	
	// Computed metrics
	double minimumDistance;         // Closest approach
	double minimumTTC;              // Most critical TTC
	double timeAtClosest;           // Time of closest approach
	
	Conflict() {
		this.samples = new ArrayList<>();
		this.minimumDistance = Double.POSITIVE_INFINITY;
		this.minimumTTC = Double.POSITIVE_INFINITY;
	}
	
	/**
	* Compute conflict metrics from samples
	*/
	void computeMetrics() {
		for (ConflictSample sample : samples) {
			if (sample.distance < minimumDistance) {
				minimumDistance = sample.distance;
				timeAtClosest = sample.time;
			}
			if (sample.ttc < minimumTTC) {
				minimumTTC = sample.ttc;
			}
		}
	}

	/**
	* Get duration of conflict
	*/
	public double getDuration() {
		return endTime - startTime;
	}
	
	public ConflictSeverity getSeverity() {
		return this.severity;
	}
	
	public double getMinimumTTC() {
		return this.minimumTTC;
	}
	
	public double getMinimumDistance() {
		return this.minimumDistance;
	}
	
	public double getStartTime() {
		return startTime;
	}
	
	public double getEndTime() {
		return endTime;
	}

	@Override
	public String toString() {
		return String.format(
		"Conflict{severity=%s, period=[%.2fs-%.2fs], duration=%.2fs, " +
		"minDistance=%.2fm @ t=%.2fs, minTTC=%.2fs}",
		severity, startTime, endTime, getDuration(),
		minimumDistance, timeAtClosest, minimumTTC
		);
	}
	
}