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

package org.eclipse.mosaic.app.ts.kosnet_applications.supplement;

public class SpeedResponse implements Response {
	
	protected final String vehicleId;
    protected final double speed;

    public SpeedResponse(String vehicleId, double speed) {
        this.vehicleId = vehicleId;
        this.speed = speed;
    }

	@Override
	public String getVehicleId() {
		return this.vehicleId;
	}
    
    public double getSpeed() {
    	return this.speed;
    }

	@Override
	public String getType() {
		return "SPEED";
	}

	@Override
	public String getValue() {
		return Double.toString(speed);
	}
    
}