/*
 * Copyright (c) 2023 Fraunhofer FOKUS and others. All rights reserved.
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

package org.eclipse.mosaic.fed.sumo.bridge.libsumo;

import org.eclipse.mosaic.fed.sumo.bridge.Bridge;
import org.eclipse.mosaic.fed.sumo.bridge.CommandException;
import org.eclipse.mosaic.rti.api.InternalFederateException;

import org.eclipse.sumo.libsumo.Vehicle;

import java.util.List;

public class VehicleGetTeleportingList implements org.eclipse.mosaic.fed.sumo.bridge.api.VehicleGetTeleportingList {
    @Override
    public List<String> execute(Bridge bridge) throws CommandException, InternalFederateException {
        return Vehicle.getTeleportingIDList().stream()
                .map(Bridge.VEHICLE_ID_TRANSFORMER::fromExternalId).toList();
    }
}
