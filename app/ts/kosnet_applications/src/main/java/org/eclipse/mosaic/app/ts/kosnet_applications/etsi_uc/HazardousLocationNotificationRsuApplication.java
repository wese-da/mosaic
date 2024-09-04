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
package org.eclipse.mosaic.app.ts.kosnet_applications.etsi_uc;

import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.AdHocModuleConfiguration;
import org.eclipse.mosaic.fed.application.app.AbstractApplication;
import org.eclipse.mosaic.fed.application.app.api.VehicleApplication;
import org.eclipse.mosaic.fed.application.app.api.os.VehicleOperatingSystem;
import org.eclipse.mosaic.lib.enums.AdHocChannel;
import org.eclipse.mosaic.lib.enums.SensorType;
import org.eclipse.mosaic.lib.enums.VehicleStopMode;
import org.eclipse.mosaic.lib.geo.GeoCircle;
import org.eclipse.mosaic.lib.geo.GeoPoint;
import org.eclipse.mosaic.lib.objects.v2x.MessageRouting;
import org.eclipse.mosaic.lib.objects.v2x.etsi.Denm;
import org.eclipse.mosaic.lib.objects.v2x.etsi.DenmContent;
import org.eclipse.mosaic.lib.objects.vehicle.VehicleData;
import org.eclipse.mosaic.lib.util.scheduling.Event;
import org.eclipse.mosaic.rti.TIME;

/**
 * ETSI use case 5.2 ...
 * This implementation should represent scenario 2 (patroller RSU and DENMs).
 * 
 */
public class HazardousLocationNotificationRsuApplication extends AbstractApplication<VehicleOperatingSystem> implements VehicleApplication {

	@Override
	public void processEvent(Event event) throws Exception {
		
		GeoPoint center = getOperatingSystem().getPosition();
		MessageRouting routing = getOs().getAdHocModule().createMessageRouting()
				.geoBroadCast(new GeoCircle(center, 500.));
		String roadId = getOperatingSystem().getNavigationModule().getRoadPosition().getConnection().getId();
		DenmContent content = new DenmContent(getOs().getSimulationTime(), center, roadId,
				SensorType.OBSTACLE, 0, 0.0f, 0);
		getOperatingSystem().getAdHocModule().sendV2xMessage(new Denm(routing, content, 0));
		getLog().infoSimTime(this, "Sent DENM hazard warning.");
		
		getOperatingSystem().getEventManager().addEvent(new Event(getOperatingSystem().getSimulationTime() + TIME.SECOND, this));
		
	}

	@Override
	public void onStartup() {
		
		getOperatingSystem().getAdHocModule().enable(new AdHocModuleConfiguration()
				.addRadio()
				.channel(AdHocChannel.CCH)
				.power(50.)
				.create());
		getLog().infoSimTime(this, "AdHoc module enabled");
		
	}

	@Override
	public void onShutdown() {
		
	}

	@Override
	public void onVehicleUpdated(VehicleData previousVehicleData, VehicleData updatedVehicleData) {
		
		boolean hazardDetected = getOperatingSystem().getStateOfEnvironmentSensor(SensorType.OBSTACLE) > 0;
		
		if (hazardDetected) {
//			String roadId = getOperatingSystem().getNavigationModule().getRoadPosition().getConnection().getId();
//			int laneIndex = getOperatingSystem().getNavigationModule().getRoadPosition().getLaneIndex();
//			ChangeLaneState cls = getOperatingSystem().changeLaneState(roadId, laneIndex+1);
//			cls.closeForAll();
			getOperatingSystem().changeSpeedWithForcedAcceleration(0.0, 5.0);
			long dur = (long) (1000 * Math.pow(10, 9));
			getOperatingSystem().stopNow(VehicleStopMode.STOP, dur);
			getOperatingSystem().getEventManager().addEvent(new Event(getOperatingSystem().getSimulationTime() + TIME.SECOND, this));
		}
		
	}

}
