/*
 * Copyright (c) 2020 Fraunhofer FOKUS and others. All rights reserved.
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contact: mosaic@fokus.fraunhofer.de
 */

package org.eclipse.mosaic.app.ts.kosnet_applications.mcs;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.mosaic.app.ts.kosnet_applications.mcs.strategy.CooperationStrategy;
import org.eclipse.mosaic.app.ts.kosnet_applications.mcs.strategy.StrategyCandidate;
import org.eclipse.mosaic.app.ts.kosnet_applications.mcs.strategy.StrategyDecision;
import org.eclipse.mosaic.app.ts.kosnet_applications.mcs.strategy.StrategyPlanner;
import org.eclipse.mosaic.app.ts.kosnet_applications.mcs.trajectory.conflict.Conflict;
import org.eclipse.mosaic.app.ts.kosnet_applications.mcs.trajectory.conflict.ConflictAnalysis;
import org.eclipse.mosaic.app.ts.kosnet_applications.mcs.trajectory.conflict.ConflictInstance;
import org.eclipse.mosaic.app.ts.kosnet_applications.mcs.trajectory.conflict.ConflictPair;
import org.eclipse.mosaic.app.ts.kosnet_applications.mcs.trajectory.conflict.ConflictRegistry;
import org.eclipse.mosaic.app.ts.kosnet_applications.mcs.trajectory.conflict.ConflictSeverity;
import org.eclipse.mosaic.app.ts.kosnet_applications.mcs.trajectory.conflict.TrajectoryConflictDetector;
import org.eclipse.mosaic.app.ts.kosnet_applications.mcs.trajectory.conflict.Urgency;
import org.eclipse.mosaic.app.ts.kosnet_applications.mcs.trajectory.conflict.VehicleConflictPair;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.AdHocModuleConfiguration;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.CamBuilder;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedAcknowledgement;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedV2xMessage;
import org.eclipse.mosaic.fed.application.app.AbstractApplication;
import org.eclipse.mosaic.fed.application.app.api.CommunicationApplication;
import org.eclipse.mosaic.fed.application.app.api.os.VehicleOperatingSystem;
import org.eclipse.mosaic.interactions.communication.V2xMessageTransmission;
import org.eclipse.mosaic.lib.enums.AdHocChannel;
import org.eclipse.mosaic.lib.geo.GeoPoint;
import org.eclipse.mosaic.lib.objects.v2x.V2xMessage;
import org.eclipse.mosaic.lib.objects.v2x.etsi.Mcm;
import org.eclipse.mosaic.lib.objects.v2x.etsi.McmContent;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.CooperationCost;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.FrenetTrajectory;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.Maneuver;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.ManeuverAdviceContainer;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.McmCategoryType;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.McmTrajectory;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.VehicleManeuverContainer;
import org.eclipse.mosaic.lib.objects.vehicle.SurroundingVehicle;
import org.eclipse.mosaic.lib.objects.vehicle.VehicleData;
import org.eclipse.mosaic.lib.util.scheduling.Event;
import org.eclipse.mosaic.rti.TIME;

public class AgreementSeekingVehicleApplication extends AbstractApplication<VehicleOperatingSystem> implements CommunicationApplication {

	private final boolean useCells;
	private FrenetTrajectory currentTrajectory;
	private TrajectoryConflictDetector conflictDetector;
	private ConflictRegistry conflictRegistry;
	private StrategyPlanner strategyPlanner;
	private Map<String, Mcm> receivedMcms = new HashMap<String, Mcm>();
	
	public AgreementSeekingVehicleApplication(boolean useCells) {
		this.useCells = useCells;
	}

	@Override
	public void onStartup() {
		
		getLog().infoSimTime(this, "Initialize application");
		if (this.useCells) {
			getOperatingSystem().getCellModule().enable();
			getLog().infoSimTime(this, "Cell module enabled");
		} else {
			getOperatingSystem().getAdHocModule().enable(new AdHocModuleConfiguration()
					.addRadio()
					.channel(AdHocChannel.CCH)
					.power(50.)
					.create());
			getLog().infoSimTime(this, "AdHoc module enabled");
		}
		
		conflictDetector = new TrajectoryConflictDetector();
		conflictRegistry = new ConflictRegistry();
		strategyPlanner = new StrategyPlanner(getOs());
		
		// trigger processEvent
		getOperatingSystem().getEventManager().addEvent(new Event(getOperatingSystem().getSimulationTime() + 200 * TIME.MILLI_SECOND, this));
		
	}

	@Override
	public void onShutdown() {
		
        getLog().infoSimTime(this, "Agreement seeking vehicle application stopped");
		
	}

	@Override
	public void processEvent(Event event) throws Exception {
		
//		generateAndBroadcastMcm();
		
		collectAllConflicts();
		
		ConflictAnalysis analysis = analyzeConflicts();
		
		StrategyDecision decision = planOptimalStrategy(analysis);
		
		executeStrategy(decision);
		
		cleanOldData();
		
		getOperatingSystem().getEventManager().addEvent(new Event(getOperatingSystem().getSimulationTime() + 200 * TIME.MILLI_SECOND, this));
		
	}

	private StrategyDecision planOptimalStrategy(ConflictAnalysis analysis) {
		
		if (analysis.getUrgency() == Urgency.NONE) {
            return StrategyDecision.maintain();
        }
		
		getLog().infoSimTime(this, "Planning optimal strategy for {} total conflicts",
                analysis.getTotalConflictCount());
		
		 List<StrategyCandidate> candidates = strategyPlanner.generateCandidates(
		            analysis, currentTrajectory
		        );
		 
		 getLog().infoSimTime(this, "Generated {} strategy candidates", 
                 candidates.size());
		 
		 for (StrategyCandidate candidate : candidates) {
	            evaluateCandidate(candidate, analysis);
	        }
		 
		return null;
	}
	
	private void evaluateCandidate(StrategyCandidate candidate, 
            ConflictAnalysis analysis) {

		// Predict conflicts with new trajectory
		Map<String, List<Conflict>> newConflicts = predictConflictsWithStrategy(candidate.getResultingTrajectory());
		
		// Score based on conflict resolution
		candidate.setConflictsResolved(countResolvedConflicts(analysis, newConflicts));
		candidate.setConflictsWorsened(countWorsenedConflicts(analysis, newConflicts));
		candidate.setConflictsCreated(countNewConflicts(analysis, newConflicts));
		
		// Calculate scores
		double resolutionScore = candidate.getConflictsResolved() * 10.0;
		double worsenPenalty = candidate.getConflictsWorsened() * -20.0;
		double creationPenalty = candidate.getConflictsCreated() * -15.0;
		double efficiencyScore = -candidate.getStrategy().getEffortCost();
		
		candidate.setTotalScore(resolutionScore + worsenPenalty + 
		       creationPenalty + efficiencyScore);
		
		// Confidence based on how many conflicts are addressed
		int totalConflicts = analysis.getTotalConflictCount();
		candidate.setConfidence(totalConflicts > 0 
		? (double) candidate.getConflictsResolved() / totalConflicts 
		: 0.0);
		
		// Build outcome description
		candidate.setOutcomeDescription(String.format(
		"Resolves %d/%d conflicts (-%d worsened, +%d new)",
		candidate.getConflictsResolved(), totalConflicts,
		candidate.getConflictsWorsened(), candidate.getConflictsCreated()
		));
	}
	
	private Map<String, List<Conflict>> predictConflictsWithStrategy(
            FrenetTrajectory alternativeTrajectory) {
        
        Map<String, List<Conflict>> newConflicts = new HashMap<>();
        
        for (Map.Entry<String, Mcm> entry : receivedMcms.entrySet()) {
            String vehicleId = entry.getKey();
            Mcm data = entry.getValue();
            
            if (data.getContent().getVehicleManeuverContainer().getMcmTrajectories().size() < 1) {
                continue;
            }
            
            List<Conflict> conflicts = conflictDetector.detectConflicts(
                alternativeTrajectory,
                data.getContent().getVehicleManeuverContainer().getMcmTrajectories().get(0).getTrajectory()
            );
            
            if (!conflicts.isEmpty()) {
                newConflicts.put(vehicleId, conflicts);
            }
        }
        
        return newConflicts;
    }
	
	private int countResolvedConflicts(ConflictAnalysis analysis,
            Map<String, List<Conflict>> newConflicts) {
		int resolved = 0;
		
		for (VehicleConflictPair pair : conflictRegistry.getAllConflicts()) {
			List<Conflict> oldConflicts = pair.getConflicts();
			List<Conflict> newConflictsWithVehicle = newConflicts.get(pair.getVehicleId());
			
			// If had conflicts before, but none now → resolved
			if (newConflictsWithVehicle == null || newConflictsWithVehicle.isEmpty()) {
				resolved += oldConflicts.size();
			} else {
				// Count conflicts that improved significantly
				for (Conflict oldConflict : oldConflicts) {
					boolean improved = true;
					for (Conflict newConflict : newConflictsWithVehicle) {
						// If new conflict is worse or same, not improved
						if (newConflict.getMinimumTTC() <= oldConflict.getMinimumTTC() * 1.2) {
						  improved = false;
						  break;
						}
					}
					if (improved) {
					resolved++;
					}
				}
			}
		}
		
		return resolved;
	}
	
	private int countWorsenedConflicts(ConflictAnalysis analysis,
            Map<String, List<Conflict>> newConflicts) {
		int worsened = 0;
		
		for (VehicleConflictPair pair : conflictRegistry.getAllConflicts()) {
			List<Conflict> newConflictsWithVehicle = newConflicts.get(pair.getVehicleId());
			
			if (newConflictsWithVehicle == null) {
				continue;
			}
			
			// Check if any new conflict is significantly worse
			for (Conflict newConflict : newConflictsWithVehicle) {
				for (Conflict oldConflict : pair.getConflicts()) {
					// If TTC decreased by >20% → worsened
					if (newConflict.getMinimumTTC() < oldConflict.getMinimumTTC() * 0.8) {
						worsened++;
					}
				}
			}
		}
		
		return worsened;
	}
	
	private int countNewConflicts(ConflictAnalysis analysis,
            Map<String, List<Conflict>> newConflicts) {
		int created = 0;
		
		for (Map.Entry<String, List<Conflict>> entry : newConflicts.entrySet()) {
			String vehicleId = entry.getKey();
			
			// If no conflicts before with this vehicle, but now has conflicts
			if (!conflictRegistry.hasConflictsWith(vehicleId)) {
				created += entry.getValue().size();
			}
		}
		
		return created;
	}
	
	private StrategyCandidate selectBestCandidate(List<StrategyCandidate> candidates,
            ConflictAnalysis analysis) {
		if (candidates.isEmpty()) {
			return null;
		}
		
		// For CRITICAL conflicts, prioritize resolution over efficiency
		if (analysis.getUrgency() == Urgency.IMMEDIATE) {
			return candidates.stream()
				.filter(c -> c.getConflictsResolved() > 0)
				.filter(c -> c.getConflictsWorsened() == 0)
				.max(Comparator.comparingDouble(c -> c.getConflictsResolved()))
				.orElse(candidates.get(0)); // Fallback to first
		}
		
		// For WARNING, balance resolution and efficiency
		return candidates.stream()
			.filter(c -> c.getConflictsWorsened() == 0) // Don't worsen anything
			.max(Comparator.comparingDouble(c -> c.getTotalScore()))
			.orElse(null);
	}
	
	private void executeStrategy(StrategyDecision decision) {
		
        if (decision == null || decision.getStrategy() == CooperationStrategy.MAINTAIN) {
            // No action needed - just broadcast current trajectory
        	generateAndBroadcastMcm(this.currentTrajectory, new CooperationCost(0.0f));
            return;
        }
        
        getLog().infoSimTime(this, "Executing strategy: {} (confidence={:.0f}%)",
                           decision.getStrategy(), decision.getConfidence() * 100);
        getLog().infoSimTime(this, "Expected: {}", decision.getExpectedOutcome());
        
        // Apply vehicle control commands
        try {
            applyVehicleControl(decision.getStrategy());
        } catch (Exception e) {
            getLog().infoSimTime(this, "Failed to apply vehicle control: {}", 
                                e.getMessage());
            return;
        }
        
        // Calculate cooperation cost
        double cooperationCost = calculateCooperationCost(
            decision.getStrategy(), 
            decision.getAffectedConflicts()
        );
        
        // Broadcast new trajectory
        if (decision.getAlternativeTrajectory() != null) {
        	generateAndBroadcastMcm(decision.getAlternativeTrajectory(), new CooperationCost(cooperationCost));
        }
    }
	
	private void applyVehicleControl(CooperationStrategy strategy) throws Exception {
        switch (strategy) {
            case EMERGENCY_BRAKE:
                getOs().changeSpeedWithInterval(0.0, TIME.SECOND);
                getLog().warnSimTime(this, "Applied EMERGENCY BRAKE");
                break;
                
            case EMERGENCY_LANE_CHANGE:
                int currentLane = getOs().getVehicleData()
                    .getRoadPosition().getLaneIndex();
                getOs().changeLane(currentLane + 1, 2 * TIME.SECOND);
                getLog().warnSimTime(this, "Applied EMERGENCY LANE CHANGE");
                break;
                
            case SLOW_DOWN:
                getOs().changeSpeedWithInterval(-3.0, 200 * TIME.MILLI_SECOND);
                getLog().infoSimTime(this, "Applied SLOW DOWN");
                break;
                
            case SPEED_UP:
                getOs().changeSpeedWithInterval(2.0, 200 * TIME.MILLI_SECOND);
                getLog().infoSimTime(this, "Applied SPEED UP");
                break;
                
            case LANE_CHANGE:
                currentLane = getOs().getVehicleData()
                    .getRoadPosition().getLaneIndex();
                getOs().changeLane(currentLane + 1, 4 * TIME.SECOND);
                getLog().infoSimTime(this, "Applied LANE CHANGE");
                break;
                
            case MAINTAIN:
                // No action
                break;
        }
    }
	
	private double calculateCooperationCost(CooperationStrategy strategy, List<ConflictInstance> conflicts) {
		// Base cost on strategy effort
		double baseCost = strategy.getEffortCost();
		
		// Adjust based on most critical conflict
		double maxSeverity = conflicts.stream().map(c -> c.getConflict().getSeverity()).mapToDouble(s -> s == ConflictSeverity.CRITICAL ? 1.0 : 
			s == ConflictSeverity.WARNING ? 0.5 : 0.1)
			.max()
			.orElse(0.0);
		
		// Negative for requests (need help), positive for offers (can help)
		return -baseCost * maxSeverity;
	}

	@Override
	public void onMessageReceived(ReceivedV2xMessage receivedV2xMessage) {
		
		V2xMessage message = receivedV2xMessage.getMessage();
		
		if(message instanceof Mcm) {

			String sourceId = message.getRouting().getSource().getSourceName();
	        getLog().infoSimTime(this, "Received new MCM from {}", sourceId);
			this.receivedMcms.put(sourceId, (Mcm) message);
		}
		
	}

	@Override
	public void onAcknowledgementReceived(ReceivedAcknowledgement acknowledgement) {
		
	}

	@Override
	public void onCamBuilding(CamBuilder camBuilder) {
		
	}

	@Override
	public void onMessageTransmitted(V2xMessageTransmission v2xMessageTransmission) {
		
	}
	
	protected void handleNewCurrentTrajectory(Event event) {
		getLog().infoSimTime(this, "Received a new trajectory.");
		this.currentTrajectory = (FrenetTrajectory) event.getResource();
	}
	
	private void generateAndBroadcastMcm(FrenetTrajectory t, CooperationCost c) {
		
//		VehicleData vehicleData = getOs().getVehicleData();
		
		if (t != null) {
			long generationTime = getOs().getSimulationTime();
			GeoPoint position = getOs().getPosition();
			
			List<McmTrajectory> trajectories = new ArrayList<McmTrajectory>();
			trajectories.add(new McmTrajectory(0, t, McmCategoryType.NONE, c));
			
			VehicleManeuverContainer vmc = new VehicleManeuverContainer(position, trajectories);
			Set<Maneuver> maneuvers = new HashSet<Maneuver>();
			ManeuverAdviceContainer mac = new ManeuverAdviceContainer(maneuvers);
			McmContent content = new McmContent(generationTime, vmc, mac);
			Mcm mcm = new Mcm(getOs().getAdHocModule().createMessageRouting().topoBroadCast(), content, 0);
			
			getOs().getAdHocModule().sendV2xMessage(mcm);
			
	        getLog().infoSimTime(this, "Broadcasted MCM with trajectory");
		}
		
	}
	
	private void collectAllConflicts() {
		
		conflictRegistry.clear();
		
		if (currentTrajectory == null) {
			return;
		}
		
		getLog().infoSimTime(this, "Checking for conflicts with {} vehicles.", receivedMcms.size());
		
		for(Map.Entry<String, Mcm> entry : receivedMcms.entrySet()) {
			
			String otherVehId = entry.getKey();
			Mcm otherMcm = entry.getValue();
			List<McmTrajectory> mcmTrajectories = otherMcm.getContent().getVehicleManeuverContainer().getMcmTrajectories();
			if (mcmTrajectories.size() < 1) {
				continue;
			}
		
			List<Conflict> conflicts = conflictDetector.detectConflicts(currentTrajectory, mcmTrajectories.get(0).getTrajectory());
			if (!conflicts.isEmpty()) {
				getLog().infoSimTime(this, "Handling conflicts with vehicle {}", otherVehId);
				for (Conflict conflict : conflicts) {
					conflictRegistry.addConflict(otherVehId, conflict);
				}
			}
			
		}
		
	}
	
	private ConflictAnalysis analyzeConflicts() {

        ConflictAnalysis analysis = new ConflictAnalysis();
        
        if (conflictRegistry.isEmpty()) {
            getLog().infoSimTime(this, "No conflicts detected");
            return analysis;
        }
        
        classifyConflictsBySeverity(analysis);
        
        identifyMostCriticalConflict(analysis);
        
        detectTemporalOverlap(analysis);
        
        detectSpatialClustering(analysis);
        
        assessUrgency(analysis);
        
        logAnalysis(analysis);
        
        return analysis;
	}
	
	private void logAnalysis(ConflictAnalysis analysis) {
        if (analysis.getUrgency() == Urgency.NONE) {
            return;
        }
        
        getLog().infoSimTime(this, "=== CONFLICT ANALYSIS ===");
        getLog().infoSimTime(this, "Urgency: {}", analysis.getUrgency());
        getLog().infoSimTime(this, "Critical: {}, Warning: {}, Notice: {}",
                           analysis.getCriticalConflicts().size(),
                           analysis.getWarningConflicts().size(),
                           analysis.getNoticeConflicts().size());
        
        if (analysis.getMostCriticalConflict() != null) {
            getLog().warnSimTime(this, "Most critical: {} (TTC={:.2f}s, d={:.2f}m)",
                               analysis.getMostCriticalConflict().getVehicleId(),
                               analysis.getMostCriticalConflict().getConflict().getMinimumTTC(),
                               analysis.getMostCriticalConflict().getConflict().getMinimumDistance());
        }
        
        if (!analysis.getOverlappingConflicts().isEmpty()) {
            getLog().infoSimTime(this, "Detected {} overlapping conflict pairs",
                               analysis.getOverlappingConflicts().size());
        }
        
        if (!analysis.getClusteredConflicts().isEmpty()) {
            getLog().infoSimTime(this, "Detected {} conflict clusters",
                               analysis.getClusteredConflicts().size());
        }
    }
	
	private void classifyConflictsBySeverity(ConflictAnalysis analysis) {
		
		for (VehicleConflictPair pair : conflictRegistry.getAllConflicts()) {
			for (Conflict conflict : pair.getConflicts()) {
				switch(conflict.getSeverity()) {
					case CRITICAL:
						analysis.addCriticalConflict(new ConflictInstance(pair.getVehicleId(), conflict));
						break;
					case WARNING:
						analysis.addWarningConflict(new ConflictInstance(pair.getVehicleId(), conflict));
						break;
					case NOTICE:
						analysis.addNoticeConflict(new ConflictInstance(pair.getVehicleId(), conflict));
					default:
						break;
				}
			}
		}
		
	}
	
	private void identifyMostCriticalConflict(ConflictAnalysis analysis) {
        ConflictInstance mostCritical = null;
        double lowestTTC = Double.POSITIVE_INFINITY;
        
        // Check critical conflicts first
        for (ConflictInstance instance : analysis.getCriticalConflicts()) {
            if (instance.getConflict().getMinimumTTC() < lowestTTC) {
                lowestTTC = instance.getConflict().getMinimumTTC();
                mostCritical = instance;
            }
        }
        
        // If no critical, check warnings
        if (mostCritical == null) {
            for (ConflictInstance instance : analysis.getWarningConflicts()) {
                if (instance.getConflict().getMinimumTTC() < lowestTTC) {
                    lowestTTC = instance.getConflict().getMinimumTTC();
                    mostCritical = instance;
                }
            }
        }
        
        analysis.setMostCriticalConflict(mostCritical);
    }
	
	private void detectTemporalOverlap(ConflictAnalysis analysis) {
        List<ConflictInstance> allConflicts = new ArrayList<>();
        allConflicts.addAll(analysis.getCriticalConflicts());
        allConflicts.addAll(analysis.getWarningConflicts());
        
        // Find conflicts that overlap in time
        for (int i = 0; i < allConflicts.size(); i++) {
            for (int j = i + 1; j < allConflicts.size(); j++) {
                ConflictInstance c1 = allConflicts.get(i);
                ConflictInstance c2 = allConflicts.get(j);
                
                // Check for temporal overlap
                boolean overlaps = !(c1.getConflict().getEndTime() < c2.getConflict().getStartTime() ||
                                   c2.getConflict().getEndTime() < c1.getConflict().getStartTime());
                
                if (overlaps) {
                    analysis.addOverlappingConflict(new ConflictPair(c1, c2));
                }
            }
        }
    }
	
	private void detectSpatialClustering(ConflictAnalysis analysis) {
        // Simplified: Check if conflicts involve same lanes/positions
        // In reality, would check actual spatial proximity from trajectory states
        
        Map<String, List<ConflictInstance>> vehicleConflicts = new HashMap<>();
        
        for (ConflictInstance instance : analysis.getCriticalConflicts()) {
            vehicleConflicts.computeIfAbsent(instance.getVehicleId(), k -> new ArrayList<>())
                           .add(instance);
        }
        for (ConflictInstance instance : analysis.getWarningConflicts()) {
            vehicleConflicts.computeIfAbsent(instance.getVehicleId(), k -> new ArrayList<>())
                           .add(instance);
        }
        
        // If multiple conflicts with same vehicle, they form a cluster
        for (Map.Entry<String, List<ConflictInstance>> entry : vehicleConflicts.entrySet()) {
            if (entry.getValue().size() > 1) {
                analysis.addClusteredConflict(entry.getValue());
            }
        }
    }
	
	private void assessUrgency(ConflictAnalysis analysis) {
        // Critical conflicts = immediate action
        if (!analysis.getCriticalConflicts().isEmpty()) {
            analysis.setUrgency(Urgency.IMMEDIATE);
            return;
        }
        
        // Multiple warnings = high urgency
        if (analysis.getWarningConflicts().size() >= 2) {
            analysis.setUrgency(Urgency.HIGH);
            return;
        }
        
        // Single warning = moderate urgency
        if (analysis.getWarningConflicts().size() == 1) {
            analysis.setUrgency(Urgency.MODERATE);
            return;
        }
        
        // Only notices = low urgency
        analysis.setUrgency(Urgency.LOW);
    }
	
	private void cleanOldData() {
		long currentTime = getOs().getSimulationTime();
        long threshold = 1 * TIME.SECOND;
        
        receivedMcms.entrySet().removeIf(entry -> 
            currentTime - entry.getValue().getGenerationTime() > threshold
        );
	}

}