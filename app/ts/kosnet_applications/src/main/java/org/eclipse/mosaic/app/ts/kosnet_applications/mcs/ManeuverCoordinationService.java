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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.AdHocModuleConfiguration;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedV2xMessage;
import org.eclipse.mosaic.fed.application.app.AbstractApplication;
import org.eclipse.mosaic.fed.application.app.api.CommunicationApplication;
import org.eclipse.mosaic.fed.application.app.api.os.VehicleOperatingSystem;
import org.eclipse.mosaic.lib.enums.AdHocChannel;
import org.eclipse.mosaic.lib.objects.road.IConnection;
import org.eclipse.mosaic.lib.objects.v2x.etsi.Cam;
import org.eclipse.mosaic.lib.objects.v2x.etsi.Mcm;
import org.eclipse.mosaic.lib.objects.v2x.etsi.McmContent;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.CooperationCost;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.IntermediatePoint;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.IntermediatePointLane;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.Lane;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.Maneuver;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.ManeuverAdviceContainer;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.McmCategoryType;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.McmTrajectory;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.Reason;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.TimeOfPos;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.Trajectory;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.VehicleManeuverContainer;
import org.eclipse.mosaic.lib.util.scheduling.Event;
import org.eclipse.mosaic.rti.TIME;

import javax.swing.plaf.IconUIResource;

public abstract class ManeuverCoordinationService extends AbstractApplication<VehicleOperatingSystem> implements CommunicationApplication {

	private final boolean useCells;
	
	public ManeuverCoordinationService(boolean useCells) {
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
		
		// trigger processEvent
		getOperatingSystem().getEventManager().addEvent(new Event(getOperatingSystem().getSimulationTime() + TIME.SECOND, this));
	}
	
	/**
	 * provide status of vehicular objects via CAMs
	 * provide event notifications via DENMs
	 * provide data about VRUs via VAMs
	 * provide status of perceived unconnected objects via CPMs
	 */
	protected void shareStatus() {
		getOperatingSystem().getAdHocModule().sendCam();
	}
	
	protected Mcm constructReferenceTrajectory() {
		return assembleMessage();
	}

	protected Mcm assembleMessage() {
		// vehicle maneuver container
		List<McmTrajectory> mcmTrajectories = new ArrayList<McmTrajectory>();
		mcmTrajectories.add(assembleCurrentTrajectory());
		if (getOperatingSystem().getVehicleData().getName().equals("veh_1")){
			for (IntermediatePointLane elem : findPotentialConflicts(mcmTrajectories)){
				System.out.println(elem.getTimeOfPos().getValue());
			};
		}
		VehicleManeuverContainer vmc = new VehicleManeuverContainer(getOperatingSystem().getPosition(), mcmTrajectories);
		// maneuver advice container
		Set<Maneuver> maneuvers = new HashSet<Maneuver>();
		ManeuverAdviceContainer mac = new ManeuverAdviceContainer(maneuvers);
		// content
		McmContent content = new McmContent(getOperatingSystem().getSimulationTime(), vmc, mac);
		Mcm mcm = new Mcm(getOperatingSystem().getAdHocModule().createMessageRouting().topoBroadCast(), content, 200);
		return mcm;
	}
	
	private McmTrajectory assembleCurrentTrajectory() {
		
		double posFromCurrentEdgeStart = getOperatingSystem().getNavigationModule().getRoadPosition().getOffset();
		List<String> edgeIds = getOperatingSystem().getNavigationModule().getCurrentRoute().getConnectionIds();
		int startIdx = edgeIds.indexOf(getOperatingSystem().getNavigationModule().getRoadPosition().getConnectionId());
		
		double currentSpeed = getOperatingSystem().getVehicleData().getSpeed();
		double accelerationTime = Math.max(13.89 - currentSpeed, accelTime(13.89, currentSpeed)); // TODO
		
		double length = -posFromCurrentEdgeStart;
		double currentPos = posFromCurrentEdgeStart;
		List<IntermediatePoint> ipl = new ArrayList<IntermediatePoint>();
		// parse the list from the current edge to the end of the route


		double timeIntervall = 1 / (double) 10;
		//System.out.println(timeIntervall);

		for (String edgeId : edgeIds.subList(startIdx, edgeIds.size())) {

			IConnection current = getOperatingSystem().getNavigationModule().getConnection(edgeId);

			double distanceCovered = 0;

			while (distanceCovered < current.getLength()) {
				ipl.add(new IntermediatePointLane(new Lane(currentPos + distanceCovered, current.getLanes(), getOperatingSystem().getRoadPosition().getLaneIndex()), Reason.NONE,
						new TimeOfPos(getOperatingSystem().getSimulationTime())));
				distanceCovered += currentSpeed * timeIntervall;
			}

			length += current.getLength();

			if (length / currentSpeed >= Mcm.MCM_HORIZON) {
				break;
			}
			currentPos = 0;

		}
		
		Trajectory trajectory = new Trajectory(ipl);
		CooperationCost cost = new CooperationCost(0.0f);
		McmTrajectory current = new McmTrajectory(0, trajectory, McmCategoryType.NONE, cost);

//		if (getOperatingSystem().getVehicleData().getName().equals("veh_1")){
//			String log_msg = "(" + getOperatingSystem().getVehicleData().getName() + ")";
//
//			for (IntermediatePoint element:current.getTrajectory().getIntermediatePoints()){
//				log_msg += ((IntermediatePointLane) element).getLane().toString();
//			}
//			System.out.println(log_msg);
//			System.out.println();
//		}


		return current;
	}

	public List<IntermediatePointLane> findPotentialConflicts(List<McmTrajectory> trajectoryList){ //does not work

		List<IntermediatePointLane> laneSwitches = new ArrayList<IntermediatePointLane>();

		for (McmTrajectory mcmTrajectory : trajectoryList){
			Trajectory trajectory = mcmTrajectory.getTrajectory();

			int previousLaneIndex = 0;

			for (IntermediatePoint interPoint : trajectory.getIntermediatePoints()){

				if (interPoint instanceof IntermediatePointLane){
					IntermediatePointLane lanePoint = (IntermediatePointLane) interPoint;
					int currentIndex = lanePoint.getLane().getLaneIndex();
					if (currentIndex != previousLaneIndex){
						previousLaneIndex = currentIndex;
						laneSwitches.add(lanePoint);
					}
				}
			}
		}

//		for (IntermediatePointLane pointLane : laneSwitches){
//			for
//		}
//
//		double currentSpeed = getOperatingSystem().getVehicleData().getSpeed();
//		TIME.SECOND


//        List<IntermediatePoint>  pointsTr1 = tr1.getIntermediatePoints();
//        List<IntermediatePoint>  pointsT21 = tr2.getIntermediatePoints();
//
//        getOperatingSystem().getNavigationModule().getConnection(edgeId);
//        getOperatingSystem().getRoadPosition().getConnectionId();
//        getOperatingSystem().getRoadPosition().getLaneIndex();

		return laneSwitches;
	}

	@Override
	public void onMessageReceived(ReceivedV2xMessage receivedV2xMessage) {
		if (receivedV2xMessage.getMessage() instanceof Cam){
			Cam camMessage = (Cam) receivedV2xMessage.getMessage();
			camMessage.getUnitID(); //"vehicle Id"
		}
	}

	public List<IntermediatePointLane> findPotentialConflicts2(McmTrajectory tr1, McmTrajectory tr2){

		List<IntermediatePointLane> konflicts = new ArrayList<IntermediatePointLane>();

		List<IntermediatePointLane> laneSwitchesTr1 = new ArrayList<IntermediatePointLane>();
		List<IntermediatePointLane> laneSwitchesTr2 = new ArrayList<IntermediatePointLane>();

		Trajectory trajectoryTr1 = tr1.getTrajectory();
		Trajectory trajectoryTr2 = tr2.getTrajectory();


		int previousLaneIndex = 0;

		for (IntermediatePoint interPoint : trajectoryTr1.getIntermediatePoints()){

			if (interPoint instanceof IntermediatePointLane){
				IntermediatePointLane lanePoint = (IntermediatePointLane) interPoint;
				int currentIndex = lanePoint.getLane().getLaneIndex();
				if (currentIndex != previousLaneIndex){
					previousLaneIndex = currentIndex;
					laneSwitchesTr1.add(lanePoint);
				}
			}
		}

		previousLaneIndex = 0;

		for (IntermediatePoint interPoint : trajectoryTr2.getIntermediatePoints()){

			if (interPoint instanceof IntermediatePointLane){
				IntermediatePointLane lanePoint = (IntermediatePointLane) interPoint;
				int currentIndex = lanePoint.getLane().getLaneIndex();
				if (currentIndex != previousLaneIndex){
					previousLaneIndex = currentIndex;
					laneSwitchesTr2.add(lanePoint);
				}
			}
		}

		double currentSpeed = getOperatingSystem().getVehicleData().getSpeed();

		for (IntermediatePointLane laneSwitchTr1 : laneSwitchesTr1){
			double laneSwitchTimeTr1 = laneSwitchTr1.getTimeOfPos().getValue() / TIME.SECOND;
			double distanceTraveledTr1 = laneSwitchTimeTr1 * currentSpeed;
			for (IntermediatePointLane laneSwitchTr2 : laneSwitchesTr2){
				double laneSwitchTimeTr2 = laneSwitchTr2.getTimeOfPos().getValue() / TIME.SECOND;
				double distanceTraveledTr2 = laneSwitchTimeTr2 * currentSpeed; //Hier anstatt currentSpeed geschwindigkeit von Fahrzeug der 2. Trajektorie
				if (distanceTraveledTr1 > (distanceTraveledTr2 - currentSpeed * 0.5) && distanceTraveledTr1 < (distanceTraveledTr2 + currentSpeed * 0.5)){ //Hier anstatt currentSpeed geschwindigkeit von Fahrzeug der 2. Trajektorie
					konflicts.add(laneSwitchTr1);
				}
			}
		}


//        List<IntermediatePoint>  pointsTr1 = tr1.getIntermediatePoints();
//        List<IntermediatePoint>  pointsT21 = tr2.getIntermediatePoints();
//
//        getOperatingSystem().getNavigationModule().getConnection(edgeId);
//        getOperatingSystem().getRoadPosition().getConnectionId();
//        getOperatingSystem().getRoadPosition().getLaneIndex();

		return konflicts;
	}

	@Override
	public void processEvent(Event event) throws Exception {

	}
	
	private double accelTime(double maxSpeed, double currentSpeed) {
		
		double a = getOperatingSystem().getVehicleParameters().getMaxAcceleration();
		double t = (maxSpeed - currentSpeed) / a;
		return t;
		
	}
	
	@Override
	public void onShutdown() {
		
	}
	
}