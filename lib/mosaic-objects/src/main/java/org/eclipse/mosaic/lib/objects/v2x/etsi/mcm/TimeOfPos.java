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

public class TimeOfPos implements ToDataOutput, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private double value;
	
	public TimeOfPos(double v) {
		this.value = v;
	}
	
	public TimeOfPos(DataInput in) throws IOException {
		this.value = in.readDouble();
	}
	
	public double getValue() {
		return this.value;
	}

	@Override
	public void toDataOutput(DataOutput dataOutput) throws IOException {
		dataOutput.writeDouble(value);
	}
	
}