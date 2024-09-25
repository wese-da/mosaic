/*
 * Copyright (c) 2022 Fraunhofer FOKUS and others. All rights reserved.
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

import org.eclipse.mosaic.lib.objects.v2x.EncodedPayload;
import org.eclipse.mosaic.lib.objects.v2x.MessageRouting;
import org.eclipse.mosaic.lib.objects.v2x.V2xMessage;
import org.eclipse.mosaic.lib.objects.v2x.etsi.cpm.SenderInformation;

public class Cpm extends V2xMessage {

	/**
     * The encoded message.
     */
    private final EncodedPayload payLoad;

	private static final long serialVersionUID = -2642419273764058771L;
	
	private CpmContent cpmContent;
	
    public Cpm(final MessageRouting routing, final CpmContent cpmContent, long minimalPayloadLength) {
		super(routing);
		
		this.cpmContent = cpmContent;

        if (EtsiPayloadConfiguration.getPayloadConfiguration().encodePayloads) {
            payLoad = new EncodedPayload(cpmContent, minimalPayloadLength);
        } else {
            payLoad = new EncodedPayload(0, minimalPayloadLength);
        }
	}
    
    public SenderInformation getSenderInfo() {
    	return cpmContent.getSenderInfo();
    }
	
	@Override
	public EncodedPayload getPayload() {
		return payLoad;
	}

}