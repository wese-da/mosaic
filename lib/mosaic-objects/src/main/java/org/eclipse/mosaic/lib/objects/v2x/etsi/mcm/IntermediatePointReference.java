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

package org.eclipse.mosaic.lib.objects.v2x.etsi.mcm;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.eclipse.mosaic.lib.geo.GeoPoint;
import org.eclipse.mosaic.lib.util.SerializationUtils;

public class IntermediatePointReference implements IntermediatePoint {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6150767794161940652L;
	
	private GeoPoint referencePosition;
	private double referenceHeading;
	private Lane lane;
	private TimeOfPos timeOfPos;
	
	public IntermediatePointReference(GeoPoint referencePosition, double referenceHeading, Lane lane, TimeOfPos timeOfPos) {
		this.referencePosition = referencePosition;
		this.referenceHeading = referenceHeading;
		this.lane = lane;
		this.timeOfPos = timeOfPos;
	}
	
	public IntermediatePointReference(DataInput in) throws IOException {
		this.referencePosition = SerializationUtils.decodeGeoPoint(in);
		this.referenceHeading = in.readDouble();
		this.lane = new Lane(in);
		this.timeOfPos = new TimeOfPos(in);
	}

	@Override
	public void toDataOutput(DataOutput dataOutput) throws IOException {
		SerializationUtils.encodeGeoPoint(dataOutput, this.referencePosition);
		dataOutput.writeDouble(this.referenceHeading);
		this.lane.toDataOutput(dataOutput);
		this.timeOfPos.toDataOutput(dataOutput);
	}
	
	public GeoPoint getReferencePosition() {
		return this.referencePosition;
	}
	
	public double getReferenceHeading() {
		return this.referenceHeading;
	}
	
	public Lane getLane() {
		return this.lane;
	}
	
	public TimeOfPos getTimeOfPos() {
		return this.timeOfPos;
	}

}
