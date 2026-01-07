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

import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.CamBuilder;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedAcknowledgement;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedV2xMessage;
import org.eclipse.mosaic.interactions.communication.V2xMessageTransmission;
import org.eclipse.mosaic.lib.objects.v2x.etsi.Mcm;
import org.eclipse.mosaic.lib.objects.v2x.etsi.McmContent;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.CooperationCost;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.IntermediatePoint;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.IntermediatePointLane;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.Maneuver;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.ManeuverAdviceContainer;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.McmCategoryType;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.McmTrajectory;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.Trajectory;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.VehicleManeuverContainer;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.VehicleRole;
import org.eclipse.mosaic.lib.util.scheduling.Event;
import org.eclipse.mosaic.rti.TIME;

public class AgreementSeekingVehicleApplication extends ManeuverCoordinationService {

	public AgreementSeekingVehicleApplication(boolean useCells) {
		super(useCells);
	}

	@Override
	public void processEvent(Event event) throws Exception {
		shareStatus();
		
		generateAndSendMcm();
		
		getOperatingSystem().getEventManager().addEvent(new Event(getOperatingSystem().getSimulationTime() + 200 * TIME.MILLI_SECOND, this));
	}

	@Override
	public void onMessageReceived(ReceivedV2xMessage receivedV2xMessage) {
		
		super.onMessageReceived(receivedV2xMessage);
		
		handleIncomingMcm(receivedV2xMessage);
		
	}
	
	private void handleIncomingMcm(ReceivedV2xMessage receivedV2xMessage) {
		if (receivedV2xMessage.getMessage() instanceof Mcm){
			Mcm mcmMessage = (Mcm) receivedV2xMessage.getMessage();

			//filetring for mcm with request Trajektories, or answer to reques trajektorie (has more than one Trajektorie)
			if (mcmMessage.getContent().getVehicleManeuverContainer().getMcmTrajectories().size() > 1){
				if (mcmMessage.getContent().getVehicleManeuverContainer().getMcmTrajectories().get(1).getMcmCategoryType() == McmCategoryType.COOPERATION_OFFER){
					//if vehicle is already in cooperation -> decline cooperation, else set vehicleRole to in cooperation and proceed to check for Conflicts
				    // check distance between subject vehicle and target vehicle to decide whether cooperation is necessary
					if (vehicleRole.equals(VehicleRole.NONE)){
						getLog().infoSimTime(this, "vehicle role set to target");
						vehicleRole = VehicleRole.TARGET;
						Trajectory requestedTrajectory = mcmMessage.getContent().getVehicleManeuverContainer().getMcmTrajectories().get(1).getTrajectory();
						List<IntermediatePointLane> conflicts = findConflictsBetweenTwoTrajectories(assembleCurrentTrajectory().getTrajectory(), requestedTrajectory);
						//if there are conflicts -> slow down (maybe constant value, maybe value relative to current speed)
						if (!conflicts.isEmpty()){
							getOperatingSystem().changeSpeedWithPleasantAcceleration(getOperatingSystem().getVehicleData().getSpeed() - 2);
//							getOperatingSystem().slowDown((float) this.getOperatingSystem().getVehicleData().getSpeed()-1, 10);
						}

						//to accept Trajectory -> send mcm Message with same Trajectories, but with McmCategoryType COOPERATION_ACCEPT (subject vehicle saves requested trajectories)
						List<McmTrajectory> messageTrajectories = mcmMessage.getContent().getVehicleManeuverContainer().getMcmTrajectories();
						List<McmTrajectory> responseTrajectories = new ArrayList<>();

						responseTrajectories.add(new McmTrajectory(1, messageTrajectories.get(0).getTrajectory(), McmCategoryType.NONE, new CooperationCost(0.0f)));
						responseTrajectories.add(new McmTrajectory(1, messageTrajectories.get(1).getTrajectory(), McmCategoryType.COOPERATION_ACCEPTANCE, new CooperationCost(0.0f)));

						VehicleManeuverContainer vmcResponse = new VehicleManeuverContainer(getOperatingSystem().getPosition(), responseTrajectories);
						Set<Maneuver> maneuvers = new HashSet<>();
						ManeuverAdviceContainer macResponse = new ManeuverAdviceContainer(maneuvers);

						McmContent responseContent = new McmContent(getOperatingSystem().getSimulationTime(), vmcResponse, macResponse);
						Mcm response = new Mcm(getOperatingSystem().getAdHocModule().createMessageRouting().topoBroadCast(), responseContent, 200);
						getOperatingSystem().getAdHocModule().sendV2xMessage(response);

					} else {
						//send the decline of the cooperation -> send mcm Message with same trajectories but with category Type cooperation decline

						List<McmTrajectory> messageTrajectories = mcmMessage.getContent().getVehicleManeuverContainer().getMcmTrajectories();
						List<McmTrajectory> responseTrajectories = new ArrayList<>();

						responseTrajectories.add(new McmTrajectory(1, messageTrajectories.get(0).getTrajectory(), McmCategoryType.NONE, new CooperationCost(0.0f)));
						responseTrajectories.add(new McmTrajectory(1, messageTrajectories.get(1).getTrajectory(), McmCategoryType.COOPERATION_DECLINE, new CooperationCost(0.0f)));

						VehicleManeuverContainer vmcResponse = new VehicleManeuverContainer(getOperatingSystem().getPosition(), responseTrajectories);
						Set<Maneuver> maneuvers = new HashSet<>();
						ManeuverAdviceContainer macResponse = new ManeuverAdviceContainer(maneuvers);

						McmContent responseContent = new McmContent(getOperatingSystem().getSimulationTime(), vmcResponse, macResponse);
						Mcm response = new Mcm(getOperatingSystem().getAdHocModule().createMessageRouting().topoBroadCast(), responseContent, 200);
						getOperatingSystem().getAdHocModule().sendV2xMessage(response);
						this.vehicleRole = VehicleRole.NONE;
					}
				}
				//if vehicle has sent cooperation request -> waits for response
				if (vehicleRole.equals(VehicleRole.SUBJECT)){
					List<McmTrajectory> trajectories = mcmMessage.getContent().getVehicleManeuverContainer().getMcmTrajectories();
					//check if the current recived message was send by the vehicle with the closest negative distance to the ego vehicle
					if (this.vehicleInfo.isEmpty()){
						this.laneChangePerformed = false;
						this.vehicleRole = VehicleRole.NONE;
//						getOperatingSystem().changeSpeedWithPleasantAcceleration(getOperatingSystem().getVehicleParameters().getMaxSpeed());
					}
					else if (mcmMessage.getRouting().getSource().getSourceName().equals(getSortedOtherVehicleInfo().getFirst().getKey())) { //<-- hier error, wenn keine Elemente vorhanden sind
						if (trajectories.get(1).getTrajectory().equals(targetTrajectory.getTrajectory())){
							//if vehicle has accepted cooperation -> change lane
							if (trajectories.get(1).getMcmCategoryType() == McmCategoryType.COOPERATION_ACCEPTANCE){
								getOperatingSystem().changeLane(1, 1000);
								getLog().infoSimTime(this, "MCM lanechange happened");
							}
						}
					}
				}
			}

			//put received reference Trajectory in vehicle info for vehicles from which a cam message was recieved beforehand
			Trajectory referenceTrajectory = mcmMessage.getContent().getVehicleManeuverContainer().getMcmTrajectories().getFirst().getTrajectory();
			VehicleInfo otherVehicle = vehicleInfo.get(mcmMessage.getRouting().getSource().getSourceName());
			if (otherVehicle != null){
				otherVehicle.setTrajectory(referenceTrajectory);
			}
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
	
	protected void generateAndSendMcm() {
		// Request lane change for all connected vehicles driving on rightmost lane to the lane to the left		
		int laneIndex = getOperatingSystem().getNavigationModule().getRoadPosition().getLaneIndex();
		//double lanePosition = getOperatingSystem().getRoadPosition().getLateralLanePosition();
		double lanePosition = getOperatingSystem().getVehicleData().getDistanceDriven();//getOperatingSystem().getNavigationModule().getRoadPosition().getOffset(); //replace getLateralLanePosition with getOffset
		
		if (laneIndex == 0) { // && !laneChangehasHappened && lanePosition > startLaneChangeArea && lanePosition < endLaneChangeArea) {

			double startLaneChangeArea = Math.random() * (150 - 25) + 25;		//vielleicht als Klassenvariablen damit nicht bei jedem aufruf die Grenzen neu generiert werden
//			double endLaneChangeArea = Math.random() * (300 - 190) + 190;
			
			if (!laneChangePerformed && lanePosition > startLaneChangeArea) {// && lanePosition < endLaneChangeArea) {
			
				getLog().infoSimTime(this, "trying to init lane change at position {}", startLaneChangeArea);
				if (this.vehicleRole.equals(VehicleRole.NONE)){
					getLog().infoSimTime(this, "vehicle role set to subject");
					this.vehicleRole = VehicleRole.SUBJECT;
	
					this.laneChangePerformed = true;
					// TODO construct requested trajectory
					Mcm mcm = constructReferenceTrajectory();
	
					//creation of the additional target trajectorie
					this.targetTrajectory = new McmTrajectory(1, new Trajectory(new ArrayList<>()), McmCategoryType.COOPERATION_OFFER, new CooperationCost(1.0f));
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
					
					getLog().infoSimTime(this, "generated trajectory with {} intermediate points", this.targetTrajectory.getTrajectory().getIntermediatePoints().size());
	
					mcm.getContent().getVehicleManeuverContainer().getMcmTrajectories().add(this.targetTrajectory);
	
					getOperatingSystem().getAdHocModule().sendV2xMessage(mcm);
				} else {
					getLog().infoSimTime(this, "vehicle involved in another lane change coordination as {}", this.vehicleRole);
				}
			} else {
				
				this.getOperatingSystem().changeLane(1, 3600);
				
			}
		}
	}
	
}
