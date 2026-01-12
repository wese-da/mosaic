package org.eclipse.mosaic.app.ts.kosnet_applications.mcs.strategy;

import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.FrenetTrajectory;

public class StrategyCandidate {
    private CooperationStrategy strategy;
    private FrenetTrajectory resultingTrajectory;
    
    // Evaluation metrics
    private int conflictsResolved;
    private int conflictsWorsened;
    private int conflictsCreated;
    private double totalScore;
    private double confidence;
    private String outcomeDescription;
    
    public CooperationStrategy getStrategy() {
    	return strategy;
    }
    
    public void setStrategy(CooperationStrategy s) {
    	this.strategy = s;
    }
    
    public FrenetTrajectory getResultingTrajectory() {
    	return resultingTrajectory;
    }
    
    public void setResultingTrajectory(FrenetTrajectory t) {
    	this.resultingTrajectory = t;
    }

	public int getConflictsResolved() {
		return conflictsResolved;
	}

	public void setConflictsResolved(int conflictsResolved) {
		this.conflictsResolved = conflictsResolved;
	}

	public int getConflictsWorsened() {
		return conflictsWorsened;
	}

	public void setConflictsWorsened(int conflictsWorsened) {
		this.conflictsWorsened = conflictsWorsened;
	}

	public int getConflictsCreated() {
		return conflictsCreated;
	}

	public void setConflictsCreated(int conflictsCreated) {
		this.conflictsCreated = conflictsCreated;
	}

	public double getTotalScore() {
		return totalScore;
	}

	public void setTotalScore(double totalScore) {
		this.totalScore = totalScore;
	}

	public double getConfidence() {
		return confidence;
	}

	public void setConfidence(double confidence) {
		this.confidence = confidence;
	}

	public String getOutcomeDescription() {
		return outcomeDescription;
	}

	public void setOutcomeDescription(String outcomeDescription) {
		this.outcomeDescription = outcomeDescription;
	}
    
}