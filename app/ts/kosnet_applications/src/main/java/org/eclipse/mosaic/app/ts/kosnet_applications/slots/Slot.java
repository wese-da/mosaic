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

import org.eclipse.mosaic.lib.geo.GeoPoint;
import org.eclipse.mosaic.lib.geo.GeoPolygon;
import org.eclipse.mosaic.lib.objects.v2x.etsi.cam.VehicleAwarenessData;

public class Slot {

	private final String vehId;
	private GeoPolygon geometry;
	
	public Slot(String vehId, GeoPoint vehiclePosition, VehicleAwarenessData aData) {
		this.vehId = vehId;
		this.geometry = buildSlotGeometry(vehiclePosition, aData);
	}
	
	private GeoPolygon buildSlotGeometry(GeoPoint vehiclePosition, VehicleAwarenessData data) {
		double heading = data.getHeading();
		double speed = data.getSpeed();
		
		return new GeoPolygon();
	}
	
	public String getVehicleId() {
		return this.vehId;
	}
	
	public GeoPolygon getSlotGeometry() {
		return this.geometry;
	}
	
}