package org.eclipse.mosaic.app.ts.kosnet_applications.etsi.uc.hazloc.sc2;

import java.awt.Color;
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
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.AdHocModuleConfiguration;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.CamBuilder;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedAcknowledgement;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedV2xMessage;
import org.eclipse.mosaic.fed.application.app.AbstractApplication;
import org.eclipse.mosaic.fed.application.app.api.CommunicationApplication;
import org.eclipse.mosaic.fed.application.app.api.os.VehicleOperatingSystem;
import org.eclipse.mosaic.interactions.communication.V2xMessageTransmission;
import org.eclipse.mosaic.interactions.vehicle.VehicleLaneChange;
import org.eclipse.mosaic.lib.enums.AdHocChannel;
import org.eclipse.mosaic.lib.geo.GeoPoint;
import org.eclipse.mosaic.lib.objects.v2x.etsi.Denm;
import org.eclipse.mosaic.lib.routing.RoutingParameters;
import org.eclipse.mosaic.lib.util.scheduling.Event;
import org.eclipse.mosaic.rti.TIME;

/**
 * ETSI use case 5.2 ...
 * 
 */
public class HazardousLocationNotificationVehicleApplication extends AbstractApplication<VehicleOperatingSystem> implements CommunicationApplication {

	@Override
	public void processEvent(Event event) throws Exception {
		
		getOperatingSystem().getAdHocModule().sendCam();
		getOperatingSystem().getEventManager().addEvent(new Event(getOs().getSimulationTime() + TIME.MILLI_SECOND * 100, this));
		
	}

	@Override
	public void onStartup() {
		getLog().infoSimTime(this, "Initialize application");
		getOperatingSystem().getAdHocModule().enable(new AdHocModuleConfiguration()
				.addRadio()
				.channel(AdHocChannel.CCH)
				.power(50.)
				.create());
		getLog().infoSimTime(this, "AdHoc module enabled");
		
		getOperatingSystem().requestVehicleParametersUpdate()
			.changeColor(Color.RED)
			.apply();

        getOs().getEventManager().addEvent(getOs().getSimulationTime() + TIME.SECOND, this);
	}

	@Override
	public void onShutdown() {
		
	}

	@Override
	public void onMessageReceived(ReceivedV2xMessage receivedV2xMessage) {
		
		if(receivedV2xMessage.getMessage() instanceof Denm) {
			
			Denm denm = (Denm) receivedV2xMessage.getMessage();
			String roadId = denm.getEventRoadId();
			
			// only relevant if the vehicle is driving on the same edge
			if (roadId.equals(getOperatingSystem().getNavigationModule().getRoadPosition().getConnection().getId())) {
				getOperatingSystem().changeLane(getOperatingSystem().getRoadPosition().getLaneIndex()+1, 100000000000L);
//				GeoPoint hazardPosition = denm.getSenderPosition();
				
//				double egoSpeed = getOperatingSystem().getVehicleData().getSpeed();
//				double distanceToHazard = hazardPosition.distanceTo(getOperatingSystem().getPosition());
				
			}
			
		}
		
	}

	@Override
	public void onAcknowledgementReceived(ReceivedAcknowledgement acknowledgement) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onCamBuilding(CamBuilder camBuilder) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onMessageTransmitted(V2xMessageTransmission v2xMessageTransmission) {
		// TODO Auto-generated method stub
		
	}

}