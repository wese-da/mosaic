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
import java.io.Serializable;

import org.eclipse.mosaic.lib.objects.ToDataOutput;

public class IntermediatePointLane implements IntermediatePoint {

	/**
	 * 
	 */
	private static final long serialVersionUID = 9196694938285049538L;
	private Lane lane;
	private Reason reason;
	private TimeOfPos timeOfPos;
	
	public IntermediatePointLane(Lane lane, Reason reason, TimeOfPos timeOfPos) {
		this.lane = lane;
		this.reason = reason;
		this.timeOfPos = timeOfPos;
	}
	
	public IntermediatePointLane(DataInput in) throws IOException {
		lane = new Lane(in);
		reason = Reason.fromId(in.readByte());
		timeOfPos = new TimeOfPos(in);
	}

	public Lane getLane() {
		return lane;
	}

	public Reason getReason() {
		return reason;
	}

	public TimeOfPos getTimeOfPos() {
		return timeOfPos;
	}

	@Override
	public void toDataOutput(DataOutput dataOutput) throws IOException {
		lane.toDataOutput(dataOutput);
		dataOutput.writeByte(reason.id);
		timeOfPos.toDataOutput(dataOutput);
	}
	
}