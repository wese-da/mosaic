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

package org.eclipse.mosaic.app.ts.kosnet_applications.etsi.uc.cacc.sc1;

import java.awt.Color;

import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.AdHocModuleConfiguration;
import org.eclipse.mosaic.fed.application.app.AbstractApplication;
import org.eclipse.mosaic.fed.application.app.api.os.VehicleOperatingSystem;
import org.eclipse.mosaic.lib.enums.AdHocChannel;
import org.eclipse.mosaic.lib.util.scheduling.Event;
import org.eclipse.mosaic.rti.TIME;

/**
 * ETSI use case 5.2.3
 * This implementation represents scenario S1: Target vehicle broadcasts CAMs.<br>
 * 
 * Architecture:
 * <ul>
 * <li> V2V cooperation</li>
 * </ul>
 * 
 * ITS-S services:
 * <ul>
 * <li>Cooperative Awareness Service (CAS) by the target vehicle</li>
 * </ul>
 */
public class CaccVehicleApplication extends AbstractApplication<VehicleOperatingSystem> {

	@Override
	public void processEvent(Event event) throws Exception {

		Integer camId = getOperatingSystem().getAdHocModule().sendCam();
		getLog().infoSimTime(this, "Sent CAM with id {}", camId);
		getOperatingSystem().getEventManager().addEvent(getOs().getSimulationTime() + TIME.SECOND, this);
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

        getOperatingSystem().getEventManager().addEvent(getOperatingSystem().getSimulationTime() + TIME.SECOND, this);
        
	}

	@Override
	public void onShutdown() {
        
	}

}