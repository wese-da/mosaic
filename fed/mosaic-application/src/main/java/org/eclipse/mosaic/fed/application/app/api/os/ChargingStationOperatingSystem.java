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

import org.eclipse.mosaic.fed.application.app.api.os.modules.AdHocCommunicative;
import org.eclipse.mosaic.fed.application.app.api.os.modules.CellCommunicative;
import org.eclipse.mosaic.fed.application.app.api.os.modules.Locatable;
import org.eclipse.mosaic.lib.objects.electricity.ChargingStationData;

import javax.annotation.Nullable;

/**
 * This interface extends the basic {@link OperatingSystem} and
 * is implemented by the {@link org.eclipse.mosaic.fed.application.ambassador.simulation.ChargingStationUnit}.
 */
public interface ChargingStationOperatingSystem
        extends OperatingSystem, Locatable, CellCommunicative, AdHocCommunicative {

    /**
     * Returns the charging station information.
     * Returns {@code null} if no information is given.
     * This could be the case if no charging stations ambassador is running.
     *
     * @return the information or {@code null} if no information is given.
     */
    @Nullable
    ChargingStationData getChargingStationData();
}
