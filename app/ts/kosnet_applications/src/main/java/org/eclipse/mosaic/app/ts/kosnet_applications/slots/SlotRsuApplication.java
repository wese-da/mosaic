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

import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.AdHocModuleConfiguration;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.CamBuilder;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedAcknowledgement;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedV2xMessage;
import org.eclipse.mosaic.fed.application.app.AbstractApplication;
import org.eclipse.mosaic.fed.application.app.api.CommunicationApplication;
import org.eclipse.mosaic.fed.application.app.api.os.RoadSideUnitOperatingSystem;
import org.eclipse.mosaic.interactions.communication.V2xMessageTransmission;
import org.eclipse.mosaic.lib.enums.AdHocChannel;
import org.eclipse.mosaic.lib.enums.SensorType;
import org.eclipse.mosaic.lib.geo.GeoCircle;
import org.eclipse.mosaic.lib.geo.GeoPoint;
import org.eclipse.mosaic.lib.objects.v2x.MessageRouting;
import org.eclipse.mosaic.lib.objects.v2x.etsi.Cam;
import org.eclipse.mosaic.lib.objects.v2x.etsi.Denm;
import org.eclipse.mosaic.lib.objects.v2x.etsi.DenmContent;
import org.eclipse.mosaic.lib.util.scheduling.Event;
import org.eclipse.mosaic.rti.TIME;

public class SlotRsuApplication extends AbstractApplication<RoadSideUnitOperatingSystem> implements CommunicationApplication {

	@Override
	public void processEvent(Event event) throws Exception {
		
		getOs().getAdHocModule().sendCam();
		getOs().getEventManager().addEvent(getOs().getSimulationTime() + TIME.SECOND, this);
		
	}

	@Override
	public void onStartup() {
		
		getLog().infoSimTime(this, "Initialize slot RSU application.");
		getOperatingSystem().getAdHocModule().enable(new AdHocModuleConfiguration()
				.addRadio()
				.channel(AdHocChannel.CCH)
				.power(50.)
				.create());
		getLog().infoSimTime(this, "AdHoc module enabled");

        getOs().getEventManager().addEvent(getOs().getSimulationTime() + TIME.SECOND, this);
	}

	@Override
	public void onShutdown() {
		
	}

	@Override
	public void onMessageReceived(ReceivedV2xMessage receivedV2xMessage) {
		
		if (receivedV2xMessage.getMessage() instanceof Cam) {
			Cam message = (Cam) receivedV2xMessage.getMessage();
			String vehId = message.getUnitID();
			GeoPoint position = message.getPosition();
			getLog().infoSimTime(this, "Received CAM: {}, pos={}", vehId, position);
			
			Denm denm = prepareDenm(message);
			getOs().getAdHocModule().sendV2xMessage(denm);
			getLog().infoSimTime(this, "Sent DENM.");
		}
		
	}
	
	private Denm prepareDenm(Cam receivedMessage) {
		GeoPoint center = getOs().getPosition();
		MessageRouting routing = getOs().getAdHocModule().createMessageRouting()
				.geoBroadCast(new GeoCircle(center, 500.));
		DenmContent content = new DenmContent(getOs().getSimulationTime(), center, null, SensorType.POSITION, 0, 13.89f, 0);
		return new Denm(routing, content, 0);
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

}