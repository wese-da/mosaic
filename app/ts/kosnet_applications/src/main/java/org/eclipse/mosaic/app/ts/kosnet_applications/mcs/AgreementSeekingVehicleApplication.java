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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.CamBuilder;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedAcknowledgement;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedV2xMessage;
import org.eclipse.mosaic.fed.application.app.api.CommunicationApplication;
import org.eclipse.mosaic.interactions.communication.V2xMessageTransmission;
import org.eclipse.mosaic.lib.objects.v2x.V2xMessage;
import org.eclipse.mosaic.lib.objects.v2x.etsi.Mcm;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.CooperationCost;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.IntermediatePoint;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.IntermediatePointLane;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.McmCategoryType;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.McmTrajectory;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.Trajectory;
import org.eclipse.mosaic.lib.util.scheduling.Event;
import org.eclipse.mosaic.rti.TIME;

public class AgreementSeekingVehicleApplication extends ManeuverCoordinationService implements CommunicationApplication {

	private Map<String, Mcm> reveivedMcms = new HashMap<String, Mcm>();
	
	private McmTrajectory targetTrajectory;

	private String vehicleRole; // "subject" or "target" or "none"

	public AgreementSeekingVehicleApplication(boolean useCells) {
		super(useCells);
	}

	@Override
	public void processEvent(Event event) throws Exception {
		shareStatus();
		
		// Request lane change for all connected vehicles driving on rightmost lane to the lane to the left
		int laneIndex = getOperatingSystem().getNavigationModule().getRoadPosition().getLaneIndex();
		//if (laneIndex == 0) {
			Mcm mcm = constructReferenceTrajectory();
			// TODO construct requested trajectory
			this.targetTrajectory = new McmTrajectory(1, new Trajectory(new ArrayList<IntermediatePoint>()), McmCategoryType.COOPERATION_OFFER, new CooperationCost(1.0f));
			mcm.getContent().getVehicleManeuverContainer().getMcmTrajectories().add(this.targetTrajectory);
		//} else {
			// Is there a nearby vehicle that requests cooperation?
		//}

		double distance = getOperatingSystem().getNavigationModule().getNextJunctionNode().getPosition().distanceTo(getOperatingSystem().getPosition());
		double drivingDur = distance / getOperatingSystem().getVehicleData().getSpeed();
		getOperatingSystem().changeLane(laneIndex+1, (long) drivingDur);

		// find and resolve conflicts with the reference trajectory
		findAndResolveConflicts(mcm);
		// TODO adapt reference trajectory
		
		getOperatingSystem().getAdHocModule().sendV2xMessage(mcm);
		getOperatingSystem().getEventManager().addEvent(new Event(getOperatingSystem().getSimulationTime() + TIME.SECOND, this));
	}

	@Override
	public void onMessageReceived(ReceivedV2xMessage receivedV2xMessage) {
		
		V2xMessage receivedMessage = receivedV2xMessage.getMessage();
		if (receivedMessage instanceof Mcm) {
			Mcm mcm = (Mcm) receivedMessage;
			this.reveivedMcms.put(receivedMessage.getRouting().getSource().getSourceName(), mcm);
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
	
	private void findAndResolveConflicts(final Mcm mcm) {
		
		List<IntermediatePoint> egoTrajectory = mcm.getContent().getVehicleManeuverContainer().getMcmTrajectories().get(0).getTrajectory().getIntermediatePoints();
		
		for (Mcm otherMcm : this.reveivedMcms.values()) {
			// Get other vehicle's reference trajectory
			McmTrajectory mcmTrajectory = otherMcm.getContent().getVehicleManeuverContainer().getMcmTrajectories().get(0);
			List<IntermediatePoint> otherTrajectory = mcmTrajectory.getTrajectory().getIntermediatePoints();
			for (int i = 0; i < egoTrajectory.size() - 1; i++) {
				for (int j = 0; j < otherTrajectory.size() - 1; j++) {
					IntermediatePoint intersection = getIntersection(egoTrajectory.subList(i, i+1), otherTrajectory.subList(i, i+1));
					if (intersection != null) {
						// handle conflict, i.e. either vehicle brakes
					}
				}
			}
		}
		
	}
	
	private IntermediatePoint getIntersection(List<IntermediatePoint> ego, List<IntermediatePoint> other) {
		return null;
	}
	
}
