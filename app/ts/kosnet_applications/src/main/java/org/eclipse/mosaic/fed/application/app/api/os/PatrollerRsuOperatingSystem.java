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
package org.eclipse.mosaic.fed.application.app.api.os;

/**
 * This interface extends the {@link TrafficManagementCenterOperatingSystem} and {@link VehicleOperatingSystem}.
 * It should represent a mobile RSU (Patroller) that for instance detects road hazards and warns connected vehicles via DENMs,
 * as well as supports driving maneuver coordination via MCMs.
 */
public interface PatrollerRsuOperatingSystem extends TrafficManagementCenterOperatingSystem, VehicleOperatingSystem {

}
