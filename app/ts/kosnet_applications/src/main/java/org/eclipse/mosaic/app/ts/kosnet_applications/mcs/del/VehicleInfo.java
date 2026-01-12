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

package org.eclipse.mosaic.app.ts.kosnet_applications.mcs.del;

import org.eclipse.mosaic.lib.geo.GeoPoint;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.TimeOfPos;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.Trajectory;

public class VehicleInfo {
    private double speed;
    private Trajectory trajectory;
    private GeoPoint positon;
    private double distanceToEgo;

    VehicleInfo(double speed, GeoPoint positon, double distanceToEgo){
        this.speed = speed;
        this.positon = positon;
        this.distanceToEgo = distanceToEgo;
    }

    public double getSpeed() {
        return speed;
    }

    public Trajectory getTrajectory() {
        return trajectory;
    }

    public GeoPoint getPositon() {
        return positon;
    }

    public double getDistanceToEgo() {
        return distanceToEgo;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public void setTrajectory(Trajectory trajectory) {
        this.trajectory = trajectory;
    }

    public void setPositon(GeoPoint positon) {
        this.positon = positon;
    }
}
