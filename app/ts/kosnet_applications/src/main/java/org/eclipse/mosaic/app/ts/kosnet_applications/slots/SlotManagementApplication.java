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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.mosaic.fed.application.ambassador.simulation.tmc.InductionLoop;
import org.eclipse.mosaic.fed.application.ambassador.simulation.tmc.LaneAreaDetector;
import org.eclipse.mosaic.fed.application.app.AbstractApplication;
import org.eclipse.mosaic.fed.application.app.api.TrafficManagementCenterApplication;
import org.eclipse.mosaic.fed.application.app.api.os.TrafficManagementCenterOperatingSystem;
import org.eclipse.mosaic.lib.util.scheduling.Event;

public class SlotManagementApplication extends AbstractApplication<TrafficManagementCenterOperatingSystem> implements TrafficManagementCenterApplication {

	private List<String> vehicleIdList;
	
	@Override
	public void onStartup() {
		this.vehicleIdList = new ArrayList<String>();
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