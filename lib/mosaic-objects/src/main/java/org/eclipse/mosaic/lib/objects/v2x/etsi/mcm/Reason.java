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

/**
 * Analog to ETSI TR 103 578<br>
 * 
 * 0: none,<br>
 * 1: lane opening,<br>
 * 2: lane closing,<br>
 * 3: lane change
 */
public enum Reason {
	NONE(0),
	LANE_OPENING(1),
	LANE_CLOSING(2),
	LANE_CHANGE(3);

	public final int id;
	
	Reason(int id) {
		this.id = id;
	}
	
	public static Reason fromId(int id) {
		for (Reason type : Reason.values()) {
			if (type.id == id) {
				return type;
			}
		}
		throw new IllegalArgumentException("Unknown Reason with id " + id);
	}
	
}