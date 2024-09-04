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

package org.eclipse.mosaic.app.ts.kosnet_applications.slots;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.AdHocModuleConfiguration;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.CamBuilder;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedAcknowledgement;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedV2xMessage;
import org.eclipse.mosaic.fed.application.app.AbstractApplication;
import org.eclipse.mosaic.fed.application.app.api.CommunicationApplication;
import org.eclipse.mosaic.fed.application.app.api.os.RoadSideUnitOperatingSystem;
import org.eclipse.mosaic.interactions.communication.V2xMessageTransmission;
import org.eclipse.mosaic.lib.enums.AdHocChannel;
import org.eclipse.mosaic.lib.enums.SensorType;
import org.eclipse.mosaic.lib.geo.CartesianPoint;
import org.eclipse.mosaic.lib.geo.GeoCircle;
import org.eclipse.mosaic.lib.geo.GeoPoint;
import org.eclipse.mosaic.lib.geo.MutableCartesianPoint;
import org.eclipse.mosaic.lib.geo.MutableGeoPoint;
import org.eclipse.mosaic.lib.objects.v2x.MessageRouting;
import org.eclipse.mosaic.lib.objects.v2x.etsi.Cam;
import org.eclipse.mosaic.lib.objects.v2x.etsi.Denm;
import org.eclipse.mosaic.lib.objects.v2x.etsi.DenmContent;
import org.eclipse.mosaic.lib.objects.v2x.etsi.cam.VehicleAwarenessData;
import org.eclipse.mosaic.lib.util.scheduling.Event;
import org.eclipse.mosaic.rti.TIME;

import a.MessageId;
import a.enums.Encoding;
import a.messages.Payload;
import de.dlr.ts.v2x.commons.translators.MessagesApp;
import de.dlr.ts.v2x.wind.denm_v2_23.DENM_PDU_Description.DENM;
import de.dlr.ts.v2x.wind.denm_v2_23.DENM_PDU_Description.LocationContainer;
import de.dlr.ts.v2x.wind.its_container_v4.ETSI_ITS_CDD.PathPointPredicted;
import de.dlr.ts.v2x.wind.its_container_v4.ETSI_ITS_CDD.ReferencePosition;

public class SlotRsuApplication extends AbstractApplication<RoadSideUnitOperatingSystem> implements CommunicationApplication {

	private Queue<Cam> currentCams = new ConcurrentLinkedQueue<Cam>();
	
	@Override
	public void processEvent(Event event) throws Exception {
		
		
		Denm denm = prepareDenm();
		getOs().getAdHocModule().sendV2xMessage(denm);
		getLog().infoSimTime(this, "Sent DENM.");
		
		getOs().getAdHocModule().sendCam();
		getOs().getEventManager().addEvent(getOs().getSimulationTime() + TIME.SECOND, this);
		
	}

	@Override
	public void onStartup() {
		
		
		getLog().infoSimTime(this, "Initialize slot RSU application.");
		getOperatingSystem().getAdHocModule().enable(new AdHocModuleConfiguration()
				.addRadio()
				.channel(AdHocChannel.CCH)
				.power(50.)
				.create());
		getLog().infoSimTime(this, "AdHoc module enabled");

        getOs().getEventManager().addEvent(getOs().getSimulationTime() + TIME.SECOND, this);
	}

	@Override
	public void onShutdown() {
		
	}

	@Override
	public void onMessageReceived(ReceivedV2xMessage receivedV2xMessage) {
		
		if (receivedV2xMessage.getMessage() instanceof Cam) {
			Cam message = (Cam) receivedV2xMessage.getMessage();
			currentCams.add(message);
			String vehId = message.getUnitID();
			GeoPoint position = message.getPosition();
			getLog().infoSimTime(this, "Received CAM: {}, pos={}", vehId, position);
		}
		
	}
	
	private Denm prepareDenm() {
		createDenm();
		GeoPoint center = getOs().getPosition();
		MessageRouting routing = getOs().getAdHocModule().createMessageRouting()
				.geoBroadCast(new GeoCircle(center, 500.));
		DenmContent content = new DenmContent(getOs().getSimulationTime(), center, null, SensorType.POSITION, 0, 13.89f, 0);
		return new Denm(routing, content, 0);
	}
	
	private void createDenm() {
		
		try {
			DENM denm = (DENM) MessagesApp.getInstance().createEmptyMessage(MessageId.DENM_V2);
			// set reference position
			GeoPoint egoPosition = getOs().getPosition();
			ReferencePosition rp = denm.getDenm().getManagement().getEventPosition();
			rp.getLatitude().setValue(egoPosition.getLatitude());
			rp.getLongitude().setValue(egoPosition.getLongitude());
			// location container
			LocationContainer lc = denm.getDenm().getLocation();
			// for all detected vehicles
			Cam message;
			int currentElementIndex = 0;
			while ((message = currentCams.poll()) != null) {
				VehicleAwarenessData data = (VehicleAwarenessData) message.getAwarenessData();
				GeoPoint pos = message.getPosition();
				double vehLength = data.getLength();
				double heading = data.getHeading();
				// first point
				PathPointPredicted ppp = lc.getPredictedPaths().getElement(currentElementIndex).getPathPredicted().getElement(0);
				ppp.getDeltaLatitude().setValue(egoPosition.getLatitude() - pos.getLatitude());
				ppp.getDeltaLongitude().setValue(egoPosition.getLongitude() - pos.getLongitude());
				ppp.getSymmetricAreaOffset().setValue(1.75f);
				// second point
				PathPointPredicted ppp2 = lc.getPredictedPaths().getElement(currentElementIndex).getPathPredicted().getElement(1);
				
				CartesianPoint pp = new MutableGeoPoint(pos.getLatitude(), pos.getLongitude()).toCartesian();
				GeoPoint pp_temp = new MutableCartesianPoint(pp.getX() - vehLength * Math.sin(heading), pp.getY() - vehLength * Math.cos(heading), 0).toGeo();
				
				ppp2.getDeltaLatitude().setValue(egoPosition.getLatitude() - pp_temp.getLatitude()); // TODO
				ppp2.getDeltaLongitude().setValue(egoPosition.getLongitude() - pp_temp.getLongitude()); // TODO
				ppp2.getSymmetricAreaOffset().setValue(1.75f);
				currentElementIndex++;
			}
			Payload p = MessagesApp.getInstance().encode(denm, Encoding.UPER);
			int length = p.getLength();
			getLog().infoSimTime(this, "Created DENM with payload length {}", length);
		} catch (Exception e) {
			getLog().error(e.getMessage());
		}
		
	}

	@Override
	public void onAcknowledgementReceived(ReceivedAcknowledgement acknowledgement) {
		
	}

	@Override
	public void onCamBuilding(CamBuilder camBuilder) {
		
	}

	@Override
	public void onMessageTransmitted(V2xMessageTransmission v2xMessageTransmission) {
		
	}

}