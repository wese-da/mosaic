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

package org.eclipse.mosaic.app.ts.kosnet_applications.slots;

import java.awt.Color;

import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.AdHocModuleConfiguration;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.CamBuilder;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedAcknowledgement;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedV2xMessage;
import org.eclipse.mosaic.fed.application.app.AbstractApplication;
import org.eclipse.mosaic.fed.application.app.api.CommunicationApplication;
import org.eclipse.mosaic.fed.application.app.api.VehicleApplication;
import org.eclipse.mosaic.fed.application.app.api.os.VehicleOperatingSystem;
import org.eclipse.mosaic.interactions.communication.V2xMessageTransmission;
import org.eclipse.mosaic.lib.enums.AdHocChannel;
import org.eclipse.mosaic.lib.enums.SensorType;
import org.eclipse.mosaic.lib.geo.GeoPolygon;
import org.eclipse.mosaic.lib.objects.kosnet.SlotManagementMessage;
import org.eclipse.mosaic.lib.objects.kosnet.SmmContent;
import org.eclipse.mosaic.lib.objects.v2x.V2xMessage;
import org.eclipse.mosaic.lib.objects.v2x.etsi.Denm;
import org.eclipse.mosaic.lib.objects.v2x.etsi.DenmContent;
import org.eclipse.mosaic.lib.objects.vehicle.VehicleData;
import org.eclipse.mosaic.lib.util.scheduling.Event;
import org.eclipse.mosaic.rti.TIME;

public class SlotVehicleApplication extends AbstractApplication<VehicleOperatingSystem> implements CommunicationApplication, VehicleApplication {

	private boolean hasSlot = false;
	
	@Override
	public void onShutdown() {
		
	}

	@Override
	public void onStartup() {
		
		getLog().infoSimTime(this, "Initialize application");
		if (useCellNetwork()) {
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
		
		getOperatingSystem().getAdHocModule().sendV2xMessage(generateRequestMessage());
		
        getOs().getEventManager().addEvent(getOs().getSimulationTime() + TIME.SECOND, this);
	}

	private boolean useCellNetwork() {
		return false;
	}

	@Override
	public void processEvent(Event arg0) throws Exception {
		// send CAM
		getOs().getAdHocModule().sendCam();
		
		if (!hasSlot) {
			getOperatingSystem().getAdHocModule().sendV2xMessage(generateRequestMessage());
		} else {
			// change color scheme
			getOperatingSystem().requestVehicleParametersUpdate()
			.changeColor(Color.BLUE)
			.apply();

			// send DENM with slot
			String extendedContainer = "slot";
			GeoPolygon eventArea = new GeoPolygon(getOperatingSystem().getPosition()); //TODO ?
			DenmContent content = new DenmContent(getOperatingSystem().getSimulationTime(), getOperatingSystem().getPosition(),
					getOperatingSystem().getNavigationModule().getRoadPosition().getConnectionId(), SensorType.POSITION, 0,
					(float) getOperatingSystem().getVehicleData().getSpeed(), getOperatingSystem().getVehicleData().getThrottle().floatValue(),
					getOperatingSystem().getPosition(), eventArea, extendedContainer);
			Denm denm = new Denm(getOperatingSystem().getAdHocModule().createMessageRouting().topoBroadCast(), content, 200);
			getOperatingSystem().getAdHocModule().sendV2xMessage(denm);
		}

        getOs().getEventManager().addEvent(getOs().getSimulationTime() + TIME.SECOND, this);
	}
	
	private SlotManagementMessage generateRequestMessage() {
		SlotManagementMessage smm = new SlotManagementMessage(getOperatingSystem().getAdHocModule().createMessageRouting().topoBroadCast(),
				new SmmContent(getOperatingSystem().getSimulationTime(), getOperatingSystem().getId(), true), 200);
		return smm;
	}

	@Override
	public void onVehicleUpdated(VehicleData previousVehicleData, VehicleData updatedVehicleData) {
		
	}

	@Override
	public void onMessageReceived(ReceivedV2xMessage receivedV2xMessage) {
		V2xMessage msg = receivedV2xMessage.getMessage();
		if (msg instanceof SlotManagementMessage) {
			if (!((SlotManagementMessage) msg).isRequestMessage()) {
				hasSlot = true;
			}
		} else if (msg instanceof Denm) {
			getLog().infoSimTime(this, "Received DENM");
		}
		
	}

	@Override
	public void onCamBuilding(CamBuilder camBuilder) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onMessageTransmitted(V2xMessageTransmission v2xMessageTransmission) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onAcknowledgementReceived(ReceivedAcknowledgement acknowledgement) {
		// TODO Auto-generated method stub
		
	}
	
}
