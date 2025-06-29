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

package org.eclipse.mosaic.lib.objects.kosnet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;

import org.eclipse.mosaic.lib.objects.ToDataOutput;

public class SmmContent implements ToDataOutput, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3509944530831220015L;
	
	private final long generationTime;
	
	// can either be a request message (true) or an acknowledgement message (false)
	private final boolean request;
	
	private final String senderId;

	public SmmContent(long generationTime, String senderId, boolean request) {
		this.generationTime = generationTime;
		this.senderId = senderId;
		this.request = request;
	}
	
	public SmmContent(DataInput di) throws IOException {
		di.readInt(); // version
		this.generationTime = di.readLong();
		this.senderId = di.readUTF();
		this.request = di.readBoolean();
	}
	
	@Override
	public void toDataOutput(DataOutput dataOutput) throws IOException {
		dataOutput.writeInt(0);
		dataOutput.writeLong(generationTime);
		dataOutput.writeUTF(senderId);
		dataOutput.writeBoolean(request);
	}
	
	public long getGenerationTime() {
		return generationTime;
	}
	
	public boolean isRequest() {
		return this.request;
	}
	
	public String getSenderId() {
		return senderId;
	}
	
}
