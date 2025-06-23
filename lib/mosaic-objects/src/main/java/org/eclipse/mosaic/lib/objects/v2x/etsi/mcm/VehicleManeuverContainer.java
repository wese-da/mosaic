package org.eclipse.mosaic.lib.objects.v2x.etsi.mcm;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import org.eclipse.mosaic.lib.geo.GeoPoint;
import org.eclipse.mosaic.lib.objects.ToDataOutput;
import org.eclipse.mosaic.lib.util.SerializationUtils;

/**
 * 
 * As proposed by ETSI TR 103 578.<br>
 * 
 */
public class VehicleManeuverContainer implements ToDataOutput, Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -7551602045572235542L;
	private final GeoPoint currentPoint;
	private List<McmTrajectory> mcmTrajectories;
	private final McmAutomationState automationState;
	
	public VehicleManeuverContainer(
			@Nonnull GeoPoint currentPoint,
			@Nonnull List<McmTrajectory> mcmTrajectories) {
		this.currentPoint = currentPoint;
		this.mcmTrajectories = mcmTrajectories;
		this.automationState = new McmAutomationState(false, false);
	}
	
	public VehicleManeuverContainer(
			@Nonnull GeoPoint currentPoint,
			@Nonnull List<McmTrajectory> mcmTrajectories,
			McmAutomationState automationState) {
		this.currentPoint = currentPoint;
		this.mcmTrajectories = mcmTrajectories;
		this.automationState = automationState;
	}
	
	public VehicleManeuverContainer(DataInput in) throws IOException {
		this.mcmTrajectories = new ArrayList<McmTrajectory>();
		this.currentPoint = SerializationUtils.decodeGeoPoint(in);
		int size = in.readInt();
		for (int i = 0; i < size; i++) {
			mcmTrajectories.add(new McmTrajectory(in));
		}
		automationState = new McmAutomationState(in);
	}
	
	public GeoPoint getCurrentPoint() {
		return currentPoint;
	}
	
	public List<McmTrajectory> getMcmTrajectories() {
		return mcmTrajectories;
	}
	
	public McmAutomationState getAutomationState() {
		return automationState;
	}

	@Override
	public void toDataOutput(DataOutput dataOutput) throws IOException {
		SerializationUtils.encodeGeoPoint(dataOutput, currentPoint);
		dataOutput.writeInt(mcmTrajectories.size());
		for (McmTrajectory t : mcmTrajectories) {
			t.toDataOutput(dataOutput);
		}
		automationState.toDataOutput(dataOutput);
	}

	/*
	 * currentPoint [McmStartPoint]
	 * mcmTrajectories [SEQUENCE SIZE(1..16) OF McmTrajectory]
	 * automationState [McmAutomationState OPTIONAL]
	 * 
	 * McmTrajectory ::= SEQUENCE
	 *  trajectoryID INTEGER (0..655335)
	 *  trajectory Trajectory
	 *  categories SEQUENCE SIZE(1..4) OF McmCategory OPTIONAL
	 *  cost CooperationCost
	 *  
	 * McmStartPoint ::= CHOICE
	 *  intermediatePointReference IntermediatePointReference
	 *  intermediatePointOffroad IntermediatePointOffroad
	 * 
	 * McmCategory ::= SEQUENCE
	 *  type McmCategoryType
	 *  objectID StationID OPTIONAL
	 *  referencedTrajectoryID INTEGER (0..655335) OPTIONAL
	 * 
	 * McmCategoryType ::= INTEGER {none(0), emergency(1), cooperationOffer(2), cooperationDecline(3), cooperationAcceptance(4)}
	 * 
	 * McmAutomationState ::= SEQUENCE
	 *  longitudinalAutomated BOOLEAN
	 *  lateralAutomated BOOLEAN
	 *  
	 * CooperationCost ::= INTEGER {zero(0), oneThousandth(1)} (-1000..1000)
	 * 
	 * Trajectory ::= SEQUENCE
	 *  intermediatePoints SEQUENCE SIZE(1..10) OF IntermediatePoint
	 *  longitudinalPositions SEQUENCE SIZE(1..11) OF Polynom
	 *  lateralPositions SEQUENCE SIZE(1..11) OF Polynom
	 *  headings SEQUENCE SIZE(1..11) OF Polynom OPTIONAL
	 * 
	 * IntermediatePoint ::= CHOICE
	 *  reference IntermediatePointReference
	 *  lane IntermediatePointLane
	 *  intersection IntermediatePointIntersection
	 *  offroad IntermediatePointOffroad
	 * 
	 * Polynom ::= SEQUENCE
	 *  coefficients SEQUENCE SIZE(1..6) OF PolynomCoefficient
	 *  start PolynomStartX
	 *  end PolynomEndX
	 *  xOffset PolynomXOffset
	 *  
	 *  IntermediatePointReference ::= SEQUENCE
	 *   referencePosition ReferencePosition
	 *   referenceHeading Heading
	 *   lane Lane
	 *   timeOfPos TimeOfPos
	 *  
	 *  IntermediatePointLane ::= SEQUENCE
	 *   lane Lane
	 *   reason Reason
	 *   timeOfPos TimeOfPos
	 *  
	 *  IntermediatePointIntersection ::= SEQUENCE
	 *   exitLane SEQUENCE
	 *    lanePosition LanePosition
	 *    laneCount
	 *   exitHeading Heading
	 *   timeOfPosEntry TimeOfPos
	 *   timeOfPosExit TimeOfPos
	 *  
	 *  IntermediatePointOffroad ::= SEQUENCE
	 *   referencePosition ReferencePosition
	 *   referenceHeading Heading
	 *   timeOfPos TimeOfPos
	 *  
	 *  Lane ::= SEQUENCE
	 *   lanePosition LanePosition
	 *   laneCount LaneCount -- NUMER OF LANES AT THE POSITION
	 *  
	 *  PolynomCoefficient ::= REAL -- 0.001 meter or seconds
	 *  PolynomStartX ::= INTEGER (0..2097151) -- 0.001 meter or seconds
	 *  PolynomEndX ::= INTEGER (0..2097151) -- 0.001 meter or seconds
	 *  PolynomXOffset ::= INTEGER (-8000000..8000000) -- 0.001 meter or seconds
	 * 
	 * LaneCount ::= INTEGER (1..16)
	 * 
	 * TimeOfPos ::= INTEGER (1..655335) -- 0.01 seconds
	 * 
	 */
	
}