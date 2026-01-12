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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.AdHocModuleConfiguration;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.CamBuilder;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedAcknowledgement;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedV2xMessage;
import org.eclipse.mosaic.fed.application.app.AbstractApplication;
import org.eclipse.mosaic.fed.application.app.api.CommunicationApplication;
import org.eclipse.mosaic.fed.application.app.api.os.RoadSideUnitOperatingSystem;
import org.eclipse.mosaic.interactions.communication.V2xMessageTransmission;
import org.eclipse.mosaic.lib.enums.AdHocChannel;
import org.eclipse.mosaic.lib.geo.CartesianPoint;
import org.eclipse.mosaic.lib.objects.road.IConnection;
import org.eclipse.mosaic.lib.objects.v2x.V2xMessage;
import org.eclipse.mosaic.lib.objects.v2x.etsi.Cam;
import org.eclipse.mosaic.lib.objects.v2x.etsi.Mcm;
import org.eclipse.mosaic.lib.objects.v2x.etsi.McmContent;
import org.eclipse.mosaic.lib.objects.v2x.etsi.cam.VehicleAwarenessData;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.CooperationCost;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.IntermediatePoint;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.IntermediatePointLane;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.Lane;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.Maneuver;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.ManeuverAdviceContainer;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.McmCategoryType;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.McmTrajectory;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.Reason;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.TimeOfPos;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.Trajectory;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.VehicleManeuverContainer;
import org.eclipse.mosaic.lib.util.scheduling.Event;
import org.eclipse.mosaic.rti.TIME;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.index.strtree.STRtree;

/**
 * 
 * Refers to TR 103 578 section 5.5 (concept B2)
 * 
 * Use of a roadside equipment for acting to facilitate the maneuver coordination of one or several subject vehicles for satisfying a global objective (e.g. safety, traffic management, vehicle interception by relevant authority etc.).<br>
 * 
 * Traffic management: either local by RSU or global by traffic management center connected to the RSU.
 * 
 */
public class PrescriptiveManeuverCoordinationService extends AbstractApplication<RoadSideUnitOperatingSystem> implements CommunicationApplication {

	
	private boolean useCells = false;
	
	private Map<String, Cam> receivedCams = new HashMap<String, Cam>();
	private Map<String, Mcm> receivedMcms = new HashMap<String, Mcm>();
	
	public PrescriptiveManeuverCoordinationService(boolean useCells) {
		this.useCells = useCells;
	}
	
	@Override
	public void processEvent(Event event) throws Exception {
		
		getOperatingSystem().getAdHocModule().sendCam();
		
		STRtree spatialIndex = updateSpatialIndex();
		
		for (Mcm mcm : receivedMcms.values()) {
			
			List<McmTrajectory> trajectories =  mcm.getContent().getVehicleManeuverContainer().getMcmTrajectories();
			int index = 0;
			if (trajectories.size() > 1) {
				index = 1;
			}
			Trajectory ref = trajectories.get(index).getTrajectory();
			
			List<Mcm> candidates = spatialIndex.query(TrajectoryUtils.getTrajectoryBoundingBox(ref));
			System.out.println();
		}
		
		for (Entry<String, Cam> cams : receivedCams.entrySet()) {
			// if maneuver coordination needed
			List<McmTrajectory> mcmTrajectories = new ArrayList<McmTrajectory>();
			VehicleManeuverContainer vmc = new VehicleManeuverContainer(getOperatingSystem().getPosition(), mcmTrajectories);
			Set<Maneuver> maneuvers = new HashSet<Maneuver>();
			ManeuverAdviceContainer mac = new ManeuverAdviceContainer(maneuvers);
			McmContent content = new McmContent(getOperatingSystem().getSimulationTime(), vmc, mac);
			Mcm mcm = new Mcm(getOperatingSystem().getAdHocModule().createMessageRouting().topoBroadCast(),
					content, 200);
			
			getOperatingSystem().getAdHocModule().sendV2xMessage(mcm);
		}

		receivedMcms.clear();
		
		getOperatingSystem().getEventManager().addEvent(new Event(getOperatingSystem().getSimulationTime() + 200 * TIME.MILLI_SECOND, this));
		
	}
	
	private STRtree updateSpatialIndex() {
		
		STRtree spatialIndex = new STRtree();
		// intent sharing
		// calculate maneuvers for all vehicles that have cooperation needs
		for (Mcm mcm : receivedMcms.values()) {
			
			// generate bounding box for trajectory
			Envelope bounds = new Envelope();
			
			List<McmTrajectory> trajectories = mcm.getContent().getVehicleManeuverContainer().getMcmTrajectories();
			
			int index = 0;
			if (trajectories.size() > 1) {
				index = 1;
			}
			Trajectory subject = trajectories.get(index).getTrajectory();
			
			for (IntermediatePoint ip : subject.getIntermediatePoints()) {
				
				if (ip instanceof IntermediatePointLane) {
					
					IntermediatePointLane ipl = (IntermediatePointLane) ip;
					Lane l = ipl.getLane();
					
					IConnection c = getOperatingSystem().getRoutingModule().getConnection(l.getLaneId());
					CartesianPoint cp =  c.getStartNode().getPosition().toCartesian();
					double x = cp.getX() + l.getLanePosition();
					bounds.expandToInclude(x, cp.getY());
					
				}
				
			}
			
			spatialIndex.insert(bounds, mcm);
			
		}
		
		return spatialIndex;
		
	}
	
	@Override
	public void onStartup() {
		
		getLog().infoSimTime(this, "Initialize application");
		if (this.useCells) {
			getOperatingSystem().getCellModule().enable();
			getLog().infoSimTime(this, "Cell module enabled");
		} else {
			getOperatingSystem().getAdHocModule().enable(new AdHocModuleConfiguration()
					.addRadio()
					.channel(AdHocChannel.CCH)
					.power(50.)
					.create());
			getLog().infoSimTime(this, "AdHoc module enabled");
		}
		
		// trigger processEvent
		getOperatingSystem().getEventManager().addEvent(new Event(getOperatingSystem().getSimulationTime() + 200 * TIME.MILLI_SECOND, this));
	}

	@Override
	public void onShutdown() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onMessageReceived(ReceivedV2xMessage receivedV2xMessage) {
		V2xMessage message = receivedV2xMessage.getMessage();
		if (message instanceof Cam) {
			Cam cam = (Cam) message;
			this.receivedCams.put(cam.getRouting().getSource().getSourceName(), cam);
		} else if (message instanceof Mcm) {
			Mcm mcm = (Mcm) message;
			this.receivedMcms.put(mcm.getRouting().getSource().getSourceName(), mcm);
		}
	}

	@Override
	public void onAcknowledgementReceived(ReceivedAcknowledgement acknowledgement) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onCamBuilding(CamBuilder camBuilder) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onMessageTransmitted(V2xMessageTransmission v2xMessageTransmission) {
		// TODO Auto-generated method stub
		
	}

}