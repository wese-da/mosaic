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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import org.eclipse.mosaic.lib.geo.GeoPoint;
import org.eclipse.mosaic.lib.junit.EtsiPayloadConfigurationRule;
import org.eclipse.mosaic.lib.objects.v2x.EncodedPayload;
import org.eclipse.mosaic.lib.objects.v2x.MessageRouting;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.CooperationCost;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.IntermediatePointLane;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.Lane;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.Maneuver;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.ManeuverAdviceContainer;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.McmAutomationState;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.McmCategoryType;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.McmTrajectory;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.Reason;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.TimeOfPos;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.Trajectory;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.VehicleManeuverContainer;
import org.eclipse.mosaic.rti.TIME;

import org.junit.Rule;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;

public class McmContentTest {

    @Rule
    public EtsiPayloadConfigurationRule messageConf = new EtsiPayloadConfigurationRule();

    @Test
    public void decodeMessage() {

        //SETUP
        GeoPoint position = GeoPoint.latLon(52.5, 13.3);

        List<McmTrajectory> mcmTrajectories = List.of(
        		new McmTrajectory(0, new Trajectory(List.of(
        				new IntermediatePointLane(new Lane(10.5, 1), Reason.NONE, new TimeOfPos(15.0))))
        				, McmCategoryType.NONE, new CooperationCost(0)));
        McmAutomationState mas = new McmAutomationState(false, false);
        VehicleManeuverContainer vmc = new VehicleManeuverContainer(position, mcmTrajectories, mas);
        
        ManeuverAdviceContainer mac = new ManeuverAdviceContainer(new HashSet<Maneuver>());
        
        Mcm mcm = new Mcm(mock(MessageRouting.class), new McmContent(4 * TIME.SECOND, vmc, mac), 200);
        EncodedPayload encodedMessage = mcm.getPayload();

        //PRE-ASSERT
        assertNotNull(encodedMessage.getBytes());
        assertTrue(encodedMessage.getBytes().length > 0);

        //RUN
        McmContent decodedMcm = encodedMessage.decodePayload();

        //ASSERT
        assertNotNull(decodedMcm);
        assertEquals(mcm.getGenerationTime(), decodedMcm.getGenerationTime());
        assertEquals(mcm.getPosition(), decodedMcm.getPosition());

        assertTrue(decodedMcm.getVehicleManeuverContainer() instanceof VehicleManeuverContainer);
        VehicleManeuverContainer maneuverContainer = (VehicleManeuverContainer) decodedMcm.getVehicleManeuverContainer();
        assertEquals(maneuverContainer.getAutomationState().isLongitudinalAutomated(),
        		decodedMcm.getVehicleManeuverContainer().getAutomationState().isLongitudinalAutomated());
        assertEquals(maneuverContainer.getAutomationState().isLateralAutomated(), decodedMcm.getVehicleManeuverContainer().getAutomationState().isLateralAutomated());

//        VehicleAwarenessData decodedAwarenessData = (VehicleAwarenessData) decodedCam.getAwarenessData();
//        assertEquals(awarenessData.getDirection(), decodedAwarenessData.getDirection());
//        assertEquals(awarenessData.getLaneIndex(), decodedAwarenessData.getLaneIndex());
//        assertEquals(awarenessData.getHeading(), decodedAwarenessData.getHeading(), 0.0001d);
//        assertEquals(awarenessData.getLength(), decodedAwarenessData.getLength(), 0.0001d);
//        assertEquals(awarenessData.getWidth(), decodedAwarenessData.getWidth(), 0.0001d);
//        assertEquals(awarenessData.getSpeed(), decodedAwarenessData.getSpeed(), 0.0001d);
//        assertEquals(awarenessData.getLongitudinalAcceleration(), decodedAwarenessData.getLongitudinalAcceleration(), 0.01);
//        assertEquals(awarenessData.getVehicleClass(), decodedAwarenessData.getVehicleClass());
    }

}
