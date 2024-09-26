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

package org.eclipse.mosaic.lib.objects.v2x.etsi.cpm;

import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;

import org.eclipse.mosaic.lib.objects.ToDataOutput;

public class SenderInformation implements ToDataOutput, Serializable {

	private static final long serialVersionUID = -5428464520887215041L;

	private final SenderType type;
	// only applicable for senders of type vehicle
	private double orientation = 0; // the actual orientation of the vehicle
	private double heading = 0; // the orientation of the provided velocity vector
	
	public SenderInformation(SenderType type) {
		this(type, 0, 0);
	}
	
	public SenderInformation(SenderType type, double orientation, double heading) {
		this.type = type;
		this.orientation = orientation;
		this.heading = heading;
	}
	
	public double getOrientation() {
		return orientation;
	}
	
	public double getHeading() {
		return heading;
	}
	
	@Override
	public void toDataOutput(DataOutput dataOutput) throws IOException {
		dataOutput.write(type.id);
		dataOutput.writeDouble(orientation);
		dataOutput.writeDouble(heading);
	}

}