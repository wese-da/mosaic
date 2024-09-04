/*
 * Copyright (c) 2022 Fraunhofer FOKUS and others. All rights reserved.
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

package org.eclipse.mosaic.app.ts.kosnet_applications.ivim;

import java.awt.Color;

import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.AdHocModuleConfiguration;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.CamBuilder;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedAcknowledgement;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedV2xMessage;
import org.eclipse.mosaic.fed.application.app.AbstractApplication;
import org.eclipse.mosaic.fed.application.app.api.CommunicationApplication;
import org.eclipse.mosaic.fed.application.app.api.os.VehicleOperatingSystem;
import org.eclipse.mosaic.interactions.communication.V2xMessageTransmission;
import org.eclipse.mosaic.lib.enums.AdHocChannel;
import org.eclipse.mosaic.lib.objects.v2x.V2xMessage;
import org.eclipse.mosaic.lib.objects.v2x.etsi.Ivim;
import org.eclipse.mosaic.lib.objects.v2x.etsi.ivim.Advice;
import org.eclipse.mosaic.lib.objects.v2x.etsi.ivim.Segment;
import org.eclipse.mosaic.lib.util.scheduling.Event;
import org.eclipse.mosaic.rti.TIME;

public class IvimVehicleApplication extends AbstractApplication<VehicleOperatingSystem> implements CommunicationApplication {
	
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
		
	}

	@Override
	public void onShutdown() {
		getLog().infoSimTime(this, "Shutdown application");
	}

	@Override
	public void processEvent(Event event) throws Exception {
		getOperatingSystem().resetSpeed();
	}

	@Override
	public void onMessageReceived(ReceivedV2xMessage receivedV2xMessage) {
		
		final V2xMessage msg = receivedV2xMessage.getMessage();

		getLog().infoSimTime(this, "Incoming message of type {}", msg.getSimpleClassName());
		
		if (!(msg instanceof Ivim)) {
            getLog().infoSimTime(this, "Ignoring message of type: {}", msg.getSimpleClassName());
            return;
		}
		
		final Ivim ivim = (Ivim) msg;
		
		reactOnInformativeData(ivim);
		
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

	private void reactOnInformativeData(final Ivim ivim) {
		
		for (Segment s : ivim.getIvimContent().getSegments()) {
			getLog().infoSimTime(this, "Parsing segment.");
			for (Advice a : s.getAdvices()) {
				getLog().infoSimTime(this, "Parsing advice.");
				// okay here since we only have one advice
				// TODO more generic solution
				getOs().changeSpeedWithInterval(a.getSpeedAdvice(), TIME.SECOND);
				getLog().infoSimTime(this, String.format("Applying speed advice %.2f "
						+ "(decelerating from %.2f)."
						, a.getSpeedAdvice(), getOs().getVehicleData().getSpeed()));
				getOperatingSystem().requestVehicleParametersUpdate()
				.changeColor(Color.GREEN)
				.apply();
			}
		}
		
	}
	
	protected boolean useCellNetwork() {
		return false;
	}
	
}