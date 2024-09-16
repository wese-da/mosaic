package org.eclipse.mosaic.app.ts.kosnet_applications.etsi_uc.sc1;

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
import java.awt.Color;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.AdHocModuleConfiguration;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.CamBuilder;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedAcknowledgement;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedV2xMessage;
import org.eclipse.mosaic.fed.application.ambassador.simulation.navigation.INavigationModule;
import org.eclipse.mosaic.fed.application.app.AbstractApplication;
import org.eclipse.mosaic.fed.application.app.api.CommunicationApplication;
import org.eclipse.mosaic.fed.application.app.api.VehicleApplication;
import org.eclipse.mosaic.fed.application.app.api.os.VehicleOperatingSystem;
import org.eclipse.mosaic.interactions.communication.V2xMessageTransmission;
import org.eclipse.mosaic.lib.enums.AdHocChannel;
import org.eclipse.mosaic.lib.enums.SensorType;
import org.eclipse.mosaic.lib.geo.GeoCircle;
import org.eclipse.mosaic.lib.geo.GeoPoint;
import org.eclipse.mosaic.lib.geo.GeoPolygon;
import org.eclipse.mosaic.lib.objects.v2x.MessageRouting;
import org.eclipse.mosaic.lib.objects.v2x.etsi.Denm;
import org.eclipse.mosaic.lib.objects.v2x.etsi.DenmContent;
import org.eclipse.mosaic.lib.objects.vehicle.VehicleData;
import org.eclipse.mosaic.lib.routing.CandidateRoute;
import org.eclipse.mosaic.lib.routing.RoutingParameters;
import org.eclipse.mosaic.lib.routing.RoutingPosition;
import org.eclipse.mosaic.lib.routing.RoutingResponse;
import org.eclipse.mosaic.lib.routing.util.ReRouteSpecificConnectionsCostFunction;
import org.eclipse.mosaic.lib.util.scheduling.Event;
import org.eclipse.mosaic.rti.TIME;

/**
 * ETSI use case 5.2.2
 * This implementation represents scenario S1.<br>
 * 
 * Architecture:
 * <ul>
 * <li>Only vehicles participate in the use case</li>
 * <li>Transmitting vehicle triggers transmission of DENMs after detection of a hazardous situation and continuously updates the DENM content to describe possible evolution of the hazardous situation or its contextualization to the driving environment (e.g. lane, zone ,road configuration)</li>
 * <li>One or multiple receiving vehicles approach the hazard location</li>
 * </ul>
 * 
 * ITS-S services:
 * <ul>
 * <li>Decentralized Environmental Notification Service (DENS) which signals the road hazard to approaching cooperative vehicles</li>
 * </ul>
 * 
 */
public class HazardousLocationNotificationVehicleApplication extends AbstractApplication<VehicleOperatingSystem> implements CommunicationApplication, VehicleApplication {

	private boolean routeAdapted = false;
	
	private static final GeoPolygon HAZARD_POLY = new GeoPolygon(GeoPoint.latLon(52.231848, 11.884322), GeoPoint.latLon(52.230087, 11.869919));

	@Override
	public void processEvent(Event event) throws Exception {

		// Send CAM
		getOperatingSystem().getAdHocModule().sendCam();
		
		// Send DENM
		GeoPoint center = getOperatingSystem().getPosition();
		MessageRouting routing = getOs().getAdHocModule().createMessageRouting()
				.geoBroadCast(new GeoCircle(center, 500.));
		String roadId = getOperatingSystem().getNavigationModule().getRoadPosition().getConnection().getId();
		DenmContent content = new DenmContent(getOs().getSimulationTime(), center, roadId,
				SensorType.OBSTACLE, 1, 0.0f, 0.0f);
		Denm denm = new Denm(routing, content, 0);
		getOperatingSystem().getAdHocModule().sendV2xMessage(denm);
		getLog().infoSimTime(this, "Sent DENM hazard warning.");
		
		// Rinse and repeat
		getOperatingSystem().getEventManager().addEvent(new Event(getOs().getSimulationTime() + TIME.SECOND , this));			
		
	}

	@Override
	public void onStartup() {
		getLog().infoSimTime(this, "Initialize application");
		getOperatingSystem().getAdHocModule().enable(new AdHocModuleConfiguration()
				.addRadio()
				.channel(AdHocChannel.CCH)
				.power(50.)
				.create());
		getLog().infoSimTime(this, "AdHoc module enabled");
		
		getOperatingSystem().requestVehicleParametersUpdate()
			.changeColor(Color.RED)
			.apply();

        getOs().getEventManager().addEvent(getOs().getSimulationTime() + TIME.SECOND, this);
	}

	@Override
	public void onShutdown() {
		
	}

	@Override
	public void onMessageReceived(ReceivedV2xMessage receivedV2xMessage) {
		
		if(receivedV2xMessage.getMessage() instanceof Denm) {
			
			Denm denm = (Denm) receivedV2xMessage.getMessage();
			String roadId = denm.getEventRoadId();
			
			// only relevant if the vehicle is driving on the same edge
			if (roadId.equals(getOperatingSystem().getNavigationModule().getRoadPosition().getConnection().getId())) {
				if(routeAdapted) {
					getLog().infoSimTime(this, "Route already changed.");
				} else {
					reroute(denm);
				} 
//				getOperatingSystem().changeLane(getOperatingSystem().getRoadPosition().getLaneIndex()+1, 100000000000L);
//				GeoPoint hazardPosition = denm.getSenderPosition();
				
//				double egoSpeed = getOperatingSystem().getVehicleData().getSpeed();
//				double distanceToHazard = hazardPosition.distanceTo(getOperatingSystem().getPosition());
				
			}
			
		}
		
	}
	
	private void reroute(final Denm denm) {
		ReRouteSpecificConnectionsCostFunction myCostFunction = new ReRouteSpecificConnectionsCostFunction();
        myCostFunction.setConnectionSpeedMS(denm.getEventRoadId(), denm.getCausedSpeed());

        /*
         * The vehicle on which this application has been deployed has a navigation module
         * that we need to retrieve in order to switch routes.
         */
        INavigationModule navigationModule = getOs().getNavigationModule();

        /*
         * Routing parameters are used for route calculation. In our case, we want a specific cost function
         * to be used for getting the best route.
         */
        RoutingParameters routingParameters = new RoutingParameters().costFunction(myCostFunction);

        /*
         * To let the navigation module calculate a new route, we need a target position and routing parameters.
         * For the target position, we keep the one our navigation module currently has, i.e. the position our vehicle
         * is currently navigating to. This means that we do not want to change our destination, only calculate
         * a new route to circumvent the obstacle.
         */
        RoutingResponse response = navigationModule.calculateRoutes(new RoutingPosition(navigationModule.getTargetPosition()), routingParameters);

        /*
         * The navigation module has calculated a number of possible routes, of which we want to retrieve the best one
         * according to our specifically assigned cost function. If a best route exists, we call the navigation module
         * to switch to it.
         */
        CandidateRoute newRoute = response.getBestRoute();
        if (newRoute != null) {
            getLog().infoSimTime(this, "Sending Change Route Command at position: {}", denm.getSenderPosition());
            navigationModule.switchRoute(newRoute);
            routeAdapted = true;
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

	@Override
	public void onVehicleUpdated(VehicleData previousVehicleData, VehicleData updatedVehicleData) {
		boolean hazardDetected = getOperatingSystem().getStateOfEnvironmentSensor(SensorType.OBSTACLE) > 0;
		
		if (hazardDetected) {
			getOperatingSystem().getEventManager().addEvent(new Event(getOperatingSystem().getSimulationTime() + TIME.SECOND, this));
		}

		getLog().infoSimTime(this, "Driving on edge {}, lane {}", getOperatingSystem().getNavigationModule().getRoadPosition().getConnectionId(),
				getOperatingSystem().getNavigationModule().getRoadPosition().getLaneIndex());
	}

}