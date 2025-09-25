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

import java.sql.Time;
import java.util.*;

import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.AdHocModuleConfiguration;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedV2xMessage;
import org.eclipse.mosaic.fed.application.app.AbstractApplication;
import org.eclipse.mosaic.fed.application.app.api.CommunicationApplication;
import org.eclipse.mosaic.fed.application.app.api.os.VehicleOperatingSystem;
import org.eclipse.mosaic.lib.enums.AdHocChannel;
import org.eclipse.mosaic.lib.geo.GeoPoint;
import org.eclipse.mosaic.lib.objects.road.IConnection;
import org.eclipse.mosaic.lib.objects.v2x.etsi.Cam;
import org.eclipse.mosaic.lib.objects.v2x.etsi.Mcm;
import org.eclipse.mosaic.lib.objects.v2x.etsi.McmContent;
import org.eclipse.mosaic.lib.objects.v2x.etsi.cam.VehicleAwarenessData;
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

public abstract class ManeuverCoordinationService extends AbstractApplication<VehicleOperatingSystem> implements CommunicationApplication {

	private final boolean useCells;
	
	public ManeuverCoordinationService(boolean useCells) {
		this.useCells = useCells;
	}

	//get the values out of the dictionary and put them into a list sorted with the custom comparator
	//list gets sorted and returned via a function
	private Map<String, VehicleInfo> otherVehicleInfo = new HashMap<>();

	private McmTrajectory currentTrajectory;
	private List<IntermediatePointLane> detectedConflikts = new ArrayList<IntermediatePointLane>();
	private McmTrajectory targetTrajectory;
	private boolean laneChangehasHappened = false;
	private String vehicleRole = "none";
	private int cooperationRequestId;
	
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

	Comparator<Map.Entry<String, VehicleInfo>> customComparator = new Comparator<Map.Entry<String, VehicleInfo>>() {
		@Override
		public int compare(Map.Entry<String, VehicleInfo> entry1, Map.Entry<String, VehicleInfo> entry2) {
			double value1 = entry1.getValue().getDistanceToEgo();
			double value2 = entry2.getValue().getDistanceToEgo();

			// If both values are negative, sort by proximity to zero
			if (value1 < 0 && value2 < 0) {
				return Double.compare(Math.abs(value1), Math.abs(value2));
			}
			// If entry1 is negative and entry2 is positive, entry1 comes first
			else if (value1 < 0 && value2 >= 0) {
				return -1;
			}
			// If entry1 is positive and entry2 is negative, entry2 comes first
			else if (value1 >= 0 && value2 < 0) {
				return 1;
			}
			// If both are positive or both are zero, maintain their order
			return 0;
		}
	};

	protected List<Map.Entry<String, VehicleInfo>> getSortedOtherVehicleInfo(){
		List<Map.Entry<String, VehicleInfo>> otherVehilceInfoList = new ArrayList<>(this.otherVehicleInfo.entrySet());
		otherVehilceInfoList.sort(customComparator);
		return otherVehilceInfoList;
	}
	
	protected Mcm constructReferenceTrajectory() {
		return assembleMessage();
	}

	protected Mcm assembleMessage() {
		// vehicle maneuver container
		List<McmTrajectory> mcmTrajectories = new ArrayList<McmTrajectory>();
		mcmTrajectories.add(assembleCurrentTrajectory());
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

		double timeIntervall = 1 / (double) 10;

		// parse the list from the current edge to the end of the route
		for (String edgeId : edgeIds.subList(startIdx, edgeIds.size())) {

			IConnection current = getOperatingSystem().getNavigationModule().getConnection(edgeId);
			double distanceCovered = 0;

			while (distanceCovered < current.getLength()) {
				ipl.add(new IntermediatePointLane(new Lane(currentPos + distanceCovered, current.getLanes(), getOperatingSystem().getRoadPosition().getLaneIndex()), Reason.NONE,
						new TimeOfPos(getOperatingSystem().getSimulationTime())));
				distanceCovered += currentSpeed * timeIntervall;

				if (length / currentSpeed >= Mcm.MCM_HORIZON / (double) TIME.SECOND) { //bricht zu früh ab -> alle Trajektorien haben Länge < 50
					//System.out.println(length/currentSpeed);
					break;
				}
				length += distanceCovered;
			}

			//length += current.getLength();
			currentPos = 0;
		}

		//System.out.println(ipl.size());
		Trajectory trajectory = new Trajectory(ipl);
		CooperationCost cost = new CooperationCost(0.0f);
		McmTrajectory current = new McmTrajectory(0, trajectory, McmCategoryType.NONE, cost);

		//return is a bit irrelavant -> maybe delete the return and change the code accordingly
		this.currentTrajectory = current;
		this.detectedConflikts = findPotentialConflicts(this.currentTrajectory.getTrajectory().getIntermediatePoints());
		return current;
	}

	public List<IntermediatePointLane> findPotentialConflicts(List<IntermediatePoint> currentPoints){

		List<IntermediatePointLane> conflicts = new ArrayList<IntermediatePointLane>();
		List<Integer> laneSwitcheIndices = new ArrayList<Integer>();
		int previousIndex = 0;

		int indexLaneSwitches = 0;
		for (IntermediatePoint interPoint : currentPoints){
			if (interPoint instanceof IntermediatePointLane){

				IntermediatePointLane lanePoint = (IntermediatePointLane) interPoint;
				int currentIndex = lanePoint.getLane().getLaneIndex();
				if (currentIndex != previousIndex){//previousLanePoint.getLane().getLaneIndex()){
					//previousLanePoint = lanePoint;
					previousIndex = currentIndex;
					//laneSwitches.add(previousLanePoint);
					laneSwitcheIndices.add(indexLaneSwitches);//-1);
				}
			}
			indexLaneSwitches++;
		}

		//System.out.println("here happened");
		for (int index : laneSwitcheIndices){
			IntermediatePointLane egoPoint = (IntermediatePointLane) currentPoints.get(index);
			int egoLaneIndex = egoPoint.getLane().getLaneIndex();
			double egoLanePosition = egoPoint.getLane().getLanePosition();
			for (Map.Entry<String, VehicleInfo> vehEntry : this.otherVehicleInfo.entrySet()) {
				if (vehEntry.getValue().getTrajectory() != null) {
					IntermediatePointLane otherPoint = (IntermediatePointLane) vehEntry.getValue().getTrajectory().getIntermediatePoints().get(index);    //get trajektorie returns null
					if (egoLaneIndex == otherPoint.getLane().getLaneIndex()) {
						double otherPosition = otherPoint.getLane().getLanePosition();
						if (egoLanePosition > (otherPosition - 25) && egoLanePosition < (otherPosition + 25)) {
							//System.out.println("ego: " + egoLanePosition + "|" + "other: " + otherPosition);
							conflicts.add(egoPoint);
						}
					}
				}
			}
		}

		return conflicts;
	}

	public List<IntermediatePointLane> findConflictsBetweenTwoTrajectories(Trajectory trajectory1, Trajectory trajectory2){
		//Konflikte zwischen zwei Trajektorien finden um Konflikte zwischen subject und target herauszufinden -> wenn konflikt abbremsen, wenn nicht Spurwechseln
		//noch Liste mit allen empfangenen Cams, nach Abstand sortiert muss implementiert werden -> vielleicht sortiertes dictionary verwenden

		List<IntermediatePointLane> conflicts = new ArrayList<IntermediatePointLane>();
		List<IntermediatePoint> pointsTrajectory1 = trajectory1.getIntermediatePoints();
		List<IntermediatePoint> pointsTrajectory2 = trajectory2.getIntermediatePoints();

		for (int i = 0; i < trajectory1.getIntermediatePoints().size(); i++){
			if (pointsTrajectory1.get(i) instanceof IntermediatePointLane && pointsTrajectory2.get(i) instanceof IntermediatePointLane){
				IntermediatePointLane pointTrajektorie1 = (IntermediatePointLane) pointsTrajectory1.get(i);
				IntermediatePointLane pointTrajektorie2 = (IntermediatePointLane) pointsTrajectory2.get(i);

				if (pointTrajektorie1.getLane().getLaneIndex() == pointTrajektorie2.getLane().getLaneIndex()){
					if (pointTrajektorie1.getLane().getLanePosition() > (pointTrajektorie2.getLane().getLanePosition() - 25) && pointTrajektorie1.getLane().getLanePosition() < (pointTrajektorie2.getLane().getLanePosition() + 25)){
						conflicts.add(pointTrajektorie1);
					}
				}
			}
		}

		return conflicts;
	}

	@Override
	public void onMessageReceived(ReceivedV2xMessage receivedV2xMessage) {
		if (receivedV2xMessage.getMessage() instanceof Cam){
			Cam camMessage = (Cam) receivedV2xMessage.getMessage();
			double xPosSelf = getOperatingSystem().getVehicleData().getPosition().toCartesian().getX();
			double YPosSelf = getOperatingSystem().getVehicleData().getPosition().toCartesian().getY();
			double xPosRecived = camMessage.getPosition().toCartesian().getX();
			double yPosRecived = camMessage.getPosition().toCartesian().getY();
			double xDistance = xPosRecived - xPosSelf;
			double yDistance = yPosRecived - YPosSelf;
			double distance = Math.sqrt(xDistance * xDistance + yDistance * yDistance);
			if (distance <= 50) {
				VehicleAwarenessData vehicleAwarenessData = (VehicleAwarenessData) camMessage.getAwarenessData();
				double vehicleSpeed = vehicleAwarenessData.getSpeed();
				//Berechnung der Entfernung von Fahrzeug, welches die CAM ausgesendet hat, zum ego-Vehicle
				GeoPoint camPosition = camMessage.getPosition();
				GeoPoint egoPosition = getOperatingSystem().getPosition();

				//negative distance -> ego vehicle in front of other vehicle -> needs to be checked for conflicts
				double distanceToEgo;
				if (xPosSelf >= xPosRecived){
					distanceToEgo = -1 * distance;
				}else {
					distanceToEgo = distance;
				}
				//put the key and some general info into the other Vehicle dictionary
				otherVehicleInfo.put(camMessage.getUnitID(), new VehicleInfo(vehicleSpeed, camPosition, distanceToEgo));
			}
		}

		if (receivedV2xMessage.getMessage() instanceof Mcm){
			Mcm mcmMessage = (Mcm) receivedV2xMessage.getMessage();
//			mcmMessage.getRouting().getSource().getSourceName(); -> id des Fahrzeugs aus Mcms
//			getOperatingSystem().getAdHocModule().sendV2xMessage();

			//filetring for mcm with request Trajektories, or answer to reques trajektorie (has more than one Trajektorie)
			if (mcmMessage.getContent().getVehicleManeuverContainer().getMcmTrajectories().size() > 1){
				if (mcmMessage.getContent().getVehicleManeuverContainer().getMcmTrajectories().get(1).getMcmCategoryType() == McmCategoryType.COOPERATION_OFFER){
					//if vehicle is already in cooperation -> decline cooperation, else set vehicleRole to in cooperation and proceed to check for Conflicts
					if (vehicleRole.equals("none")){
						System.out.println("vehicle set to target");
						//do cooperation stuff
						vehicleRole = "target";
						Trajectory requestedTrajectory = mcmMessage.getContent().getVehicleManeuverContainer().getMcmTrajectories().get(1).getTrajectory();
						//List<IntermediatePointLane> conflicts = findPotentialConflicts(requestedTrajectory.getIntermediatePoints());
						List<IntermediatePointLane> conflicts = findConflictsBetweenTwoTrajectories(assembleCurrentTrajectory().getTrajectory(), requestedTrajectory);
						//if there are conflicts -> slow down
						if (!conflicts.isEmpty()){
							getOperatingSystem().slowDown(13,10);
						}

						//to accept Trajectory -> send mcm Message with same Trajectories, but with McmCategoryType COOPERATION_ACCEPT (subject vehicle saves requested trajectories)
						List<McmTrajectory> messageTrajectories = mcmMessage.getContent().getVehicleManeuverContainer().getMcmTrajectories();
						List<McmTrajectory> responseTrajectories = new ArrayList<McmTrajectory>();

						responseTrajectories.add(new McmTrajectory(1, messageTrajectories.get(0).getTrajectory(), McmCategoryType.NONE, new CooperationCost(0.0f)));
						responseTrajectories.add(new McmTrajectory(1, messageTrajectories.get(1).getTrajectory(), McmCategoryType.COOPERATION_ACCEPTANCE, new CooperationCost(0.0f)));

						VehicleManeuverContainer vmcResponse = new VehicleManeuverContainer(getOperatingSystem().getPosition(), responseTrajectories);
						Set<Maneuver> maneuvers = new HashSet<Maneuver>();
						ManeuverAdviceContainer macResponse = new ManeuverAdviceContainer(maneuvers);

						McmContent responseContent = new McmContent(getOperatingSystem().getSimulationTime(), vmcResponse, macResponse);
						//Mcm response = new Mcm(getOperatingSystem().getAdHocModule().createMessageRouting().topoCast()topoBroadCast(), responseContent, 200); <-- für directes nachrichten senden an fahrzeug mit bestimmter id
						Mcm response = new Mcm(getOperatingSystem().getAdHocModule().createMessageRouting().topoBroadCast(), responseContent, 200);
						getOperatingSystem().getAdHocModule().sendV2xMessage(response);

					}else {
						//send the decline of the cooperation -> send mcm Message with same trajectories but with category Type cooperation decline

						List<McmTrajectory> messageTrajectories = mcmMessage.getContent().getVehicleManeuverContainer().getMcmTrajectories();
						List<McmTrajectory> responseTrajectories = new ArrayList<McmTrajectory>();

						responseTrajectories.add(new McmTrajectory(1, messageTrajectories.get(0).getTrajectory(), McmCategoryType.NONE, new CooperationCost(0.0f)));
						responseTrajectories.add(new McmTrajectory(1, messageTrajectories.get(1).getTrajectory(), McmCategoryType.COOPERATION_DECLINE, new CooperationCost(0.0f)));

						VehicleManeuverContainer vmcResponse = new VehicleManeuverContainer(getOperatingSystem().getPosition(), responseTrajectories);
						Set<Maneuver> maneuvers = new HashSet<Maneuver>();
						ManeuverAdviceContainer macResponse = new ManeuverAdviceContainer(maneuvers);

						McmContent responseContent = new McmContent(getOperatingSystem().getSimulationTime(), vmcResponse, macResponse);
						Mcm response = new Mcm(getOperatingSystem().getAdHocModule().createMessageRouting().topoBroadCast(), responseContent, 200);
						getOperatingSystem().getAdHocModule().sendV2xMessage(response);
					}
				}
				//if vehicle has send cooperation request -> waits for response
				if (vehicleRole.equals("subject")){
					List<McmTrajectory> trajectories = mcmMessage.getContent().getVehicleManeuverContainer().getMcmTrajectories();
					//check if the current recived message was send by the vehicle with the closest negative distance to the ego vehicle
					if (mcmMessage.getRouting().getSource().getSourceName().equals(getSortedOtherVehicleInfo().getFirst().getKey()))
						if (trajectories.get(1).getTrajectory().equals(targetTrajectory.getTrajectory())){
							//if vehicle has accepted cooperation -> change lane
							if (trajectories.get(1).getMcmCategoryType() == McmCategoryType.COOPERATION_ACCEPTANCE){
								getOperatingSystem().changeLane(1, 1000);
								System.out.println("MCM lanechange happened");
							}
					}
				}
			}

			//put received reference Trajectory in vehicle info for vehicles from which a cam message was recieved beforehand
			Trajectory referenceTrajectory = mcmMessage.getContent().getVehicleManeuverContainer().getMcmTrajectories().getFirst().getTrajectory();
			VehicleInfo otherVehicle = otherVehicleInfo.get(mcmMessage.getRouting().getSource().getSourceName());
			if (otherVehicle != null){
				otherVehicle.setTrajectory(referenceTrajectory);
			}
		}
	}

	@Override
	public void processEvent(Event event) throws Exception {
		System.out.println("event happened");
		shareStatus();

		// Request lane change for all connected vehicles driving on rightmost lane to the lane to the left
		int laneIndex = getOperatingSystem().getNavigationModule().getRoadPosition().getLaneIndex();
		//double lanePosition = getOperatingSystem().getRoadPosition().getLateralLanePosition();
		double lanePosition = getOperatingSystem().getNavigationModule().getRoadPosition().getLateralLanePosition();


//		double startLaneChangeArea = Math.random() * (150 - 25) + 25;		//vielleicht als Klassenvariablen damit nicht bei jedem aufruf die Grenzen neu generiert werden
//		double endLaneChangeArea = Math.random() * (300 - 190) + 190;

		double startLaneChangeArea = (150 - 25) + 25;		//vielleicht als Klassenvariablen damit nicht bei jedem aufruf die Grenzen neu generiert werden
		double endLaneChangeArea = (300 - 190) + 190;

		//if (laneIndex == 0 && !laneChangehasHappened && lanePosition > startLaneChangeArea && lanePosition < endLaneChangeArea) {
		//if (laneIndex == 1 && lanePosition > startLaneChangeArea && lanePosition < endLaneChangeArea) {
		if (lanePosition > startLaneChangeArea && lanePosition < endLaneChangeArea) {
			if (this.vehicleRole.equals("none")){
				System.out.println("vehicle set to subject");
				this.vehicleRole = "subject";

				this.laneChangehasHappened = true;
				// TODO construct requested trajectory
				Mcm mcm = constructReferenceTrajectory();

				//creation of the additional target trajectorie
				this.targetTrajectory = new McmTrajectory(1, new Trajectory(new ArrayList<IntermediatePoint>()), McmCategoryType.COOPERATION_OFFER, new CooperationCost(1.0f));
				//copies the reference trajectory, but changes the lane indexes of the lane Points after the lane change (constructs reference trajectory)
				double currentPos = getOperatingSystem().getNavigationModule().getRoadPosition().getLateralLanePosition();
				for (IntermediatePoint ip : mcm.getContent().getVehicleManeuverContainer().getMcmTrajectories().getFirst().getTrajectory().getIntermediatePoints()){
					if (ip instanceof IntermediatePointLane){
						IntermediatePointLane ipLane = (IntermediatePointLane) ip;
						if (ipLane.getLane().getLanePosition() > currentPos){
							ipLane.getLane().setLaneIndex(1);
						}
						this.targetTrajectory.getTrajectory().getIntermediatePoints().add(ipLane);
					}
				}

				mcm.getContent().getVehicleManeuverContainer().getMcmTrajectories().add(this.targetTrajectory);

				getOperatingSystem().getAdHocModule().sendV2xMessage(mcm);
			}
		}

//		// find and resolve conflicts with the reference trajectory
//		findAndResolveConflicts(mcm);
//		// TODO adapt reference trajectory


		getOperatingSystem().getEventManager().addEvent(new Event(getOperatingSystem().getSimulationTime() + TIME.SECOND, this));
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