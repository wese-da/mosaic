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
 * 1: emergency,<br>
 * 2: cooperation offer,<br>
 * 3: cooperation decline,<br>
 * 4: cooperation acceptance
 */
public enum McmCategoryType {
	NONE(0),
	EMERGENCY(1),
	COOPERATION_OFFER(2),
	COOPERATION_DECLINE(3),
	COOPERATION_ACCEPTANCE(4);
	
	public final int id;
	
	McmCategoryType(int id) {
		this.id = id;
	}
	
	public static McmCategoryType fromId(int id) {
		for (McmCategoryType type : McmCategoryType.values()) {
			if (type.id == id) {
				return type;
			}
		}
		throw new IllegalArgumentException("Unknown CooperationType id " + id);
	}

}
