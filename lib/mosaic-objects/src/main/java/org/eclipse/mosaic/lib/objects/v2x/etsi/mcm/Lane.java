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

public class Lane implements ToDataOutput, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3923398733977493373L;
	private double lanePosition;
	private int laneCount;
	// deviation from std
	private int laneIndex;
	
	public Lane(double lanePosition, int laneCount, int laneIndex) {
		this.lanePosition = lanePosition;
		
		if (laneCount > 16 || laneCount < 1) {
			laneCount = 0; // TODO handle invalid values
		}
		
		this.laneCount = laneCount;
		this.laneIndex = laneIndex;
	}
	
	public Lane(double lanePosition, int laneCount) {
		this(lanePosition, laneCount, 0);
	}
	
	public Lane(DataInput in) throws IOException {
		this.lanePosition = in.readDouble();
		this.laneCount = in.readInt();
		this.laneIndex = in.readInt();
	}
	
	public double getLanePosition() {
		return lanePosition;
	}
	
	public int getLaneCount() {
		return laneCount;
	}

	@Override
	public void toDataOutput(DataOutput dataOutput) throws IOException {
		dataOutput.writeDouble(lanePosition);
		dataOutput.writeInt(laneCount);
		dataOutput.writeInt(laneIndex);
	}

	public int getLaneIndex(){return laneIndex;}

	public String toString(){
		return "||" + this.laneIndex + "|" + this.lanePosition + "||";

	}
	
}