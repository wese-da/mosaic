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

package org.eclipse.mosaic.app.ts.kosnet_applications.cp;

import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.AdHocModuleConfiguration;
import org.eclipse.mosaic.fed.application.ambassador.simulation.perception.SimplePerceptionConfiguration;
import org.eclipse.mosaic.fed.application.app.AbstractApplication;
import org.eclipse.mosaic.fed.application.app.api.os.VehicleOperatingSystem;
import org.eclipse.mosaic.lib.enums.AdHocChannel;
import org.eclipse.mosaic.lib.objects.v2x.etsi.Cpm;
import org.eclipse.mosaic.lib.objects.v2x.etsi.CpmContent;
import org.eclipse.mosaic.lib.objects.v2x.etsi.cpm.PerceivedObjectsData;
import org.eclipse.mosaic.lib.objects.v2x.etsi.cpm.SenderInformation;
import org.eclipse.mosaic.lib.objects.v2x.etsi.cpm.SenderType;
import org.eclipse.mosaic.lib.util.scheduling.Event;
import org.eclipse.mosaic.rti.TIME;

public class CollectivePerceptionVehicleApplication extends AbstractApplication<VehicleOperatingSystem> {

	private boolean useCells = false;
	
	public CollectivePerceptionVehicleApplication(final boolean useCells) {
		this.useCells = useCells;
	}
	
	@Override
	public void processEvent(Event event) throws Exception {
		
		getOperatingSystem().getAdHocModule().sendV2xMessage(generateCpm());
		getLog().infoSimTime(this, "Collective Perception Message sent.");
		getOperatingSystem().getEventManager().addEvent(new Event(getOperatingSystem().getSimulationTime() + TIME.SECOND, this));
		
		
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
		
		getOperatingSystem().getPerceptionModule().enable(new SimplePerceptionConfiguration.Builder(120, 100).build());
		getOperatingSystem().getEventManager().addEvent(new Event(getOperatingSystem().getSimulationTime() + TIME.SECOND, this));
		
	}
	
	private Cpm generateCpm() {
		
		PerceivedObjectsData pod = new PerceivedObjectsData(getOperatingSystem().getId(), getOperatingSystem().getPerceptionModule().getPerceivedVehicles());
		CpmContent content = new CpmContent(getOperatingSystem().getSimulationTime(), new SenderInformation(SenderType.VEHICLE), pod);
		Cpm cpm = new Cpm(getOperatingSystem().getAdHocModule().createMessageRouting().topoBroadCast(), content, 200);
		
		return cpm;
		
	}

	@Override
	public void onShutdown() {
		
	}
	
}