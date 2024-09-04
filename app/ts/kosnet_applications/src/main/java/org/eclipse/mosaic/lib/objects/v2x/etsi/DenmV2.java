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

import org.eclipse.mosaic.lib.objects.v2x.MessageRouting;

public class DenmV2 extends Denm {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4811440435715441368L;

	public DenmV2(MessageRouting routing, Denm denm, long minimalPayloadLength) {
		super(routing, denm, minimalPayloadLength);
	}
	
}
