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

import org.eclipse.mosaic.lib.enums.SensorType;
import org.eclipse.mosaic.lib.objects.ToDataOutput;

public class SensorInformation implements ToDataOutput, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -823322070356042026L;
	
	private SensorType type;

	@Override
	public void toDataOutput(DataOutput dataOutput) throws IOException {
		// TODO Auto-generated method stub
		
	}

}
