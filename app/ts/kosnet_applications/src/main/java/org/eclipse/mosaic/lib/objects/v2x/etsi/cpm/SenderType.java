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


public enum SenderType {
	
	VEHICLE(0),
	RSU(1);
	
	public final int id;
	
	SenderType(int id) {
		this.id = id;
	}

    /**
     * Returns the enum mapped from an integer.
     *
     * @param id identifying integer
     * @return the enum mapped from an integer.
     */
    public static SenderType fromId(int id) {
        for (SenderType type : SenderType.values()) {
            if (type.id == id) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown SenderType id " + id);
    }

}
