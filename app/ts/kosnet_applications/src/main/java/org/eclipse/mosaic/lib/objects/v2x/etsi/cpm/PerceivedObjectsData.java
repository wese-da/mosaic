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
import java.util.ArrayList;
import java.util.List;

import org.eclipse.mosaic.fed.application.ambassador.simulation.perception.index.objects.SpatialObject;
import org.eclipse.mosaic.lib.objects.ToDataOutput;

public class PerceivedObjectsData implements ToDataOutput, Serializable {

	private static final long serialVersionUID = 8383938606513263329L;
	
	private String sensorId;
	private List<SpatialObject<?>> perceivedObjects;
	
	public PerceivedObjectsData(String sensorId) {
		this.sensorId = sensorId;
		this.perceivedObjects = new ArrayList<SpatialObject<?>>();
	}
	
	public PerceivedObjectsData(String sensorId, List<SpatialObject<?>> list) {
		this.sensorId = sensorId;
		this.perceivedObjects = list;
	}

	@Override
	public void toDataOutput(DataOutput dataOutput) throws IOException {
		dataOutput.writeUTF(sensorId);
		for (SpatialObject<?> obj : perceivedObjects) {
			dataOutput.writeUTF(obj.toString()); // TODO correct?
		}
	}
	
	public String getSensorId() {
		return sensorId;
	}
	
	public void addPerceivedObjects(final List<SpatialObject<?>> objects) {
		this.perceivedObjects.addAll(objects);
	}
	
	public List<SpatialObject<?>> getPerceivedObjects() {
		return perceivedObjects;
	}

}