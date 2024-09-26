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

package org.eclipse.mosaic.lib.objects.v2x.etsi;

import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;
import java.util.Objects;

import javax.annotation.Nonnull;

import org.eclipse.mosaic.lib.objects.ToDataOutput;
import org.eclipse.mosaic.lib.objects.v2x.etsi.cpm.PerceivedObjectsData;
import org.eclipse.mosaic.lib.objects.v2x.etsi.cpm.SenderInformation;

public class CpmContent implements ToDataOutput, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 9177024789065166904L;
	
    private final long generationTime;
    
    private final SenderInformation senderInfo;
    // private final SensorInformation sensorInfo;
    // private final PerceptionRegionData percRegInfo;
     private final PerceivedObjectsData percObj;
    
    public CpmContent(long generationTime,
    		@Nonnull SenderInformation senderInfo,
    		@Nonnull PerceivedObjectsData percObj) {
    	this.generationTime = generationTime;
    	this.senderInfo = Objects.requireNonNull(senderInfo);
    	this.percObj = Objects.requireNonNull(percObj);
    }
    
    public long getGenerationTime() {
    	return generationTime;
    }

    public SenderInformation getSenderInfo() {
    	return senderInfo;
    }
    
    public PerceivedObjectsData getPerceivedObjectsData() {
    	return percObj;
    }
    
	@Override
	public void toDataOutput(DataOutput dataOutput) throws IOException {
		dataOutput.writeLong(generationTime);
		senderInfo.toDataOutput(dataOutput);
		percObj.toDataOutput(dataOutput);
	}

}
