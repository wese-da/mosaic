package org.eclipse.mosaic.lib.objects.v2x.etsi.mcm;

public class ManeuverAdviceContainer {

	/*
	 * 
	 * SEQUENCE(SIZE(1..16)) OF Manoeuvre
	 * 
	 * Manoeuvre ::= SEQUENCE
	 * 	manoeuvreID INTEGER(0..655335)
	 * executantID StationID
	 * executantPosition ReferencePosition
	 * executantHeading Heading
	 * trajectory Trajectory
	 * automationAdvice McmAutomationState OPTIONAL
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
	 * McmAutomationState ::= SEQUENCE
	 *  longitudinalAutomated BOOLEAN
	 *  lateralAutomated BOOLEAN
	 * 
	 * LaneCount ::= INTEGER (1..16)
	 * 
	 * TimeOfPos ::= INTEGER (1..655335) -- 0.01 seconds
	 * 
	 * Reason ::= ENUMERATED {none(0), laneOpening(1), laneClosing(2), laneChange(3)}
	 * 
	 */
	
}
