package org.eclipse.mosaic.app.ts.kosnet_applications.mcs.strategy;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.mosaic.app.ts.kosnet_applications.mcs.trajectory.conflict.ConflictInstance;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.FrenetTrajectory;

public class StrategyDecision {
    private CooperationStrategy strategy;
    private FrenetTrajectory alternativeTrajectory;
    private List<ConflictInstance> affectedConflicts;
    private String expectedOutcome;
    private double confidence;
    
    public static StrategyDecision maintain() {
        StrategyDecision decision = new StrategyDecision();
        decision.strategy = CooperationStrategy.MAINTAIN;
        decision.affectedConflicts = new ArrayList<>();
        decision.expectedOutcome = "No action needed";
        decision.confidence = 1.0;
        return decision;
    }

	public CooperationStrategy getStrategy() {
		return strategy;
	}

	public void setStrategy(CooperationStrategy strategy) {
		this.strategy = strategy;
	}

	public FrenetTrajectory getAlternativeTrajectory() {
		return alternativeTrajectory;
	}

	public void setAlternativeTrajectory(FrenetTrajectory alternativeTrajectory) {
		this.alternativeTrajectory = alternativeTrajectory;
	}

	public List<ConflictInstance> getAffectedConflicts() {
		return new ArrayList<ConflictInstance>(affectedConflicts);
	}

	public void setAffectedConflicts(List<ConflictInstance> affectedConflicts) {
		this.affectedConflicts = new ArrayList<ConflictInstance>(affectedConflicts);
	}

	public String getExpectedOutcome() {
		return expectedOutcome;
	}

	public void setExpectedOutcome(String expectedOutcome) {
		this.expectedOutcome = expectedOutcome;
	}

	public double getConfidence() {
		return confidence;
	}

	public void setConfidence(double confidence) {
		this.confidence = confidence;
	}
}