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

import org.eclipse.mosaic.lib.objects.v2x.EncodedPayload;
import org.eclipse.mosaic.lib.objects.v2x.MessageRouting;
import org.eclipse.mosaic.lib.objects.v2x.V2xMessage;
import org.eclipse.mosaic.lib.objects.v2x.etsi.EtsiPayloadConfiguration;

public class SlotManagementMessage extends V2xMessage {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7764831746385259076L;

	/**
     * The encoded message.
     */
    private final EncodedPayload payLoad;
    
    private SmmContent content;

	public SlotManagementMessage(MessageRouting routing, SmmContent content, long minimalPayloadLength) {
		super(routing);
		this.content = content;
		if (EtsiPayloadConfiguration.getPayloadConfiguration().encodePayloads) {
            payLoad = new EncodedPayload(content, minimalPayloadLength);
        } else {
            payLoad = new EncodedPayload(0, minimalPayloadLength);
        }
	}

	@Override
	public EncodedPayload getPayload() {
		return payLoad;
	}
	
	public boolean isRequestMessage() {
		return content.isRequest();
	}
	
	public long getGenerationTime() {
		return content.getGenerationTime();
	}
	
	public String getSenderId() {
		return content.getSenderId();
	}

}
