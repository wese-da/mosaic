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

package org.eclipse.mosaic.app.ts.kosnet_applications;

import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.AdHocModuleConfiguration;
import org.eclipse.mosaic.fed.application.app.AbstractApplication;
import org.eclipse.mosaic.fed.application.app.api.os.ServerOperatingSystem;
import org.eclipse.mosaic.lib.enums.AdHocChannel;
import org.eclipse.mosaic.lib.geo.GeoCircle;
import org.eclipse.mosaic.lib.geo.GeoPoint;
import org.eclipse.mosaic.lib.objects.v2x.MessageRouting;
import org.eclipse.mosaic.lib.objects.v2x.etsi.Ivim;
import org.eclipse.mosaic.lib.objects.v2x.etsi.IvimContent;
import org.eclipse.mosaic.lib.objects.v2x.etsi.ivim.Advice;
import org.eclipse.mosaic.lib.objects.v2x.etsi.ivim.Segment;
import org.eclipse.mosaic.lib.objects.v2x.etsi.ivim.SegmentPosition;
import org.eclipse.mosaic.lib.util.scheduling.Event;
import org.eclipse.mosaic.rti.TIME;

public class IvimServerApplication extends AbstractApplication<ServerOperatingSystem>  {

	private static final GeoPoint CENTER = GeoPoint.latLon(52.633047, 13.565314);
	private static final long INTERVAL = 1 * TIME.SECOND;
	
	@Override
	public void processEvent(Event event) throws Exception {
		sample();
	}

	@Override
	public void onStartup() {
		getLog().infoSimTime(this, "Initialize IVIM service");
		getOperatingSystem().getCellModule().enable();
		sample();
	}

	@Override
	public void onShutdown() {
		getLog().infoSimTime(this, "Shutdown IVIM service");
	}
	
	private void sample() {
		final Ivim ivim = constructIvim();
		getOperatingSystem().getCellModule().sendV2xMessage(ivim);
		
		getLog().infoSimTime(this, "Sent IVIM");
		
		getOs().getEventManager().addEvent(new Event(getOs().getSimulationTime() + INTERVAL, this));
	}
	
	private Ivim constructIvim() {
		final GeoCircle geoCircle  = new GeoCircle(CENTER, 3000);
		final MessageRouting routing = getOperatingSystem().getCellModule().createMessageRouting().geoBroadcastBasedOnUnicast(geoCircle);
		
		return new Ivim(routing, new IvimContent(getOperatingSystem().getSimulationTime()).addSegment(
				new Segment("speed advice")
					.setStartPosition(new SegmentPosition()
							.setEdgePosition("284243530_2879911873_1313885463", 0)
					.setGeoPosition(GeoPoint.latLon(52.627410, 13.563967, 0), 0))
					.setEndPosition(new SegmentPosition()
							.setEdgePosition("385101817_1313885548_5841713763", 0)
					.setGeoPosition(GeoPoint.latLon(52.612722, 13.548835, 0), 0))
					.putAdvice(0, new Advice().setSpeedAdvice(30/3.6f))), 0);
	}

}