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
import org.eclipse.mosaic.lib.objects.v2x.etsi.CamContent;
import org.eclipse.mosaic.lib.objects.v2x.etsi.Denm;
import org.eclipse.mosaic.lib.objects.vehicle.VehicleData;
import org.eclipse.mosaic.lib.util.scheduling.Event;
import org.eclipse.mosaic.rti.TIME;

public class SlotVehicleApplication extends AbstractApplication<VehicleOperatingSystem> implements CommunicationApplication, VehicleApplication {

	@Override
	public void onShutdown() {
		// TODO Auto-generated method stub
		
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
		
		getOperatingSystem().requestVehicleParametersUpdate()
			.changeColor(Color.RED)
			.apply();

        getOs().getEventManager().addEvent(getOs().getSimulationTime() + TIME.SECOND, this);
	}

	private boolean useCellNetwork() {
		return false;
	}

	@Override
	public void processEvent(Event arg0) throws Exception {
        sendCam();

        getOs().getEventManager().addEvent(getOs().getSimulationTime() + TIME.SECOND, this);
	}

    private void sendCam() {
        getLog().infoSimTime(this, "Sending CAM");
        getOs().getAdHocModule().sendCam();
    }

	@Override
	public void onVehicleUpdated(VehicleData previousVehicleData, VehicleData updatedVehicleData) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onMessageReceived(ReceivedV2xMessage receivedV2xMessage) {
		if (receivedV2xMessage.getMessage() instanceof Denm) {
			getLog().infoSimTime(this, "Received DENM");
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
