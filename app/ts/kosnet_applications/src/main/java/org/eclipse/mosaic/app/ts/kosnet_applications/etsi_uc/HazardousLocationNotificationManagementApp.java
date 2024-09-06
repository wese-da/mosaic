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

import java.util.Collection;

import org.eclipse.mosaic.fed.application.ambassador.simulation.tmc.InductionLoop;
import org.eclipse.mosaic.fed.application.ambassador.simulation.tmc.LaneAreaDetector;
import org.eclipse.mosaic.fed.application.app.AbstractApplication;
import org.eclipse.mosaic.fed.application.app.api.TrafficManagementCenterApplication;
import org.eclipse.mosaic.fed.application.app.api.os.TrafficManagementCenterOperatingSystem;
import org.eclipse.mosaic.lib.util.scheduling.Event;

public class HazardousLocationNotificationManagementApp extends AbstractApplication<TrafficManagementCenterOperatingSystem> implements TrafficManagementCenterApplication {

	@Override
	public void onStartup() {
		
        getOs().changeLaneState("192548826_2030999713_3257049721", 0).closeForAll();
        getOs().changeLaneState("192548826_2030999713_3257049721", 1).closeForAll();
        
	}

	@Override
	public void onShutdown() {
		
	}

	@Override
	public void processEvent(Event event) throws Exception {
		
	}

	@Override
	public void onInductionLoopUpdated(Collection<InductionLoop> updatedInductionLoops) {
		
	}

	@Override
	public void onLaneAreaDetectorUpdated(Collection<LaneAreaDetector> updatedLaneAreaDetectors) {
		
	}

}
