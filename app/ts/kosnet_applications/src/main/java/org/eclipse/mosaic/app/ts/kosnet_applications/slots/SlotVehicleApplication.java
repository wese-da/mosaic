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

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.AdHocModuleConfiguration;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.CamBuilder;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedAcknowledgement;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedV2xMessage;
import org.eclipse.mosaic.fed.application.app.AbstractApplication;
import org.eclipse.mosaic.fed.application.app.api.CommunicationApplication;
import org.eclipse.mosaic.fed.application.app.api.os.VehicleOperatingSystem;
import org.eclipse.mosaic.interactions.communication.V2xMessageTransmission;
import org.eclipse.mosaic.lib.enums.AdHocChannel;
import org.eclipse.mosaic.lib.enums.DestinationType;
import org.eclipse.mosaic.lib.enums.ProtocolType;
import org.eclipse.mosaic.lib.enums.SensorType;
import org.eclipse.mosaic.lib.geo.CartesianPoint;
import org.eclipse.mosaic.lib.geo.GeoPoint;
import org.eclipse.mosaic.lib.geo.GeoPolygon;
import org.eclipse.mosaic.lib.geo.MutableCartesianPoint;
import org.eclipse.mosaic.lib.objects.addressing.DestinationAddressContainer;
import org.eclipse.mosaic.lib.objects.addressing.NetworkAddress;
import org.eclipse.mosaic.lib.objects.addressing.SourceAddressContainer;
import org.eclipse.mosaic.lib.objects.v2x.MessageRouting;
import org.eclipse.mosaic.lib.objects.v2x.etsi.Denm;
import org.eclipse.mosaic.lib.objects.v2x.etsi.DenmContent;
import org.eclipse.mosaic.lib.transform.Wgs84Projection;
import org.eclipse.mosaic.lib.util.scheduling.Event;

public class SlotVehicleApplication extends AbstractApplication<VehicleOperatingSystem> implements CommunicationApplication {

	@Override
	public void onShutdown() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStartup() {
		
		getLog().infoSimTime(this, "Initialize application");
		if (useCellNetwork()) {
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
		
		getOperatingSystem().requestVehicleParametersUpdate()
			.changeColor(Color.RED)
			.apply();
		constructDenm();
	}

	@Override
	public void processEvent(Event arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onAcknowledgementReceived(ReceivedAcknowledgement arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onCamBuilding(CamBuilder arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onMessageReceived(ReceivedV2xMessage arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onMessageTransmitted(V2xMessageTransmission arg0) {
		// TODO Auto-generated method stub
		
	}
	
	protected boolean useCellNetwork() {
		return false;
	}
	
	private Denm constructDenm() {
		GeoPoint egoPosition = getOperatingSystem().getPosition();
		
		GeoPolygon area = createSlotGeometry(egoPosition);
		
		DestinationAddressContainer destination = new DestinationAddressContainer(DestinationType.AD_HOC_GEOCAST, new NetworkAddress(NetworkAddress.BROADCAST_ADDRESS),
				AdHocChannel.SCH1, 1, area, ProtocolType.UDP);
		SourceAddressContainer source = new SourceAddressContainer(
				getOs().getAdHocModule().getSourceAddress(), "", egoPosition);
		MessageRouting messageRouting = new MessageRouting(destination, source);

		DenmContent denmContent = new DenmContent(
				0L, egoPosition, "roadID", SensorType.POSITION, 1,
				13.89f, 0.0f);
		Denm denm = new Denm(messageRouting, denmContent, 5L);
		return denm;
	}
	
	private GeoPolygon createSlotGeometry(final GeoPoint egoPosition) {
		SumoData sd = getVehicleData();
		
		double heading = sd.heading;
		double length = sd.length;
		double width = sd.width;

		Wgs84Projection proj = new Wgs84Projection(egoPosition);
		CartesianPoint origin = proj.geographicToCartesian(egoPosition);
		
		CartesianPoint[] points = {new MutableCartesianPoint(origin.getX() + Math.cos(heading) * width, origin.getY() + Math.sin(heading) * width, 0),
				new MutableCartesianPoint(origin.getX() + Math.cos(heading) * width, origin.getY() - Math.sin(heading) * width, 0),
				new MutableCartesianPoint(origin.getX() - length * Math.cos(heading), origin.getY() - Math.sin(heading) * width, 0),
				new MutableCartesianPoint(origin.getX() - length * Math.cos(heading), origin.getY() + Math.sin(heading) * width, 0)
				};
		
		
		GeoPoint[] geoPoints = {null, null, null, null};
		for (int i = 0; i < points.length; i++) {
			geoPoints[i] = proj.cartesianToGeographic(points[i]);
			getLog().infoSimTime(this, geoPoints[i].toString());
		}
		
		return new GeoPolygon(geoPoints);
	}
	
	private SumoData getVehicleData() {
		
		SumoData sd = new SumoData();
		
		sd.length = getVehicleLength();
		sd.heading = getVehicleHeading();
		sd.width = getVehicleWidth();
		
		return sd;
		
	}
	
	private double getVehicleLength() {
		byte traciVehicleLength = (byte) 0x40;
		String res = getOs().sendSumoTraciRequest(new byte[] {traciVehicleLength});
		
		getLog().infoSimTime(this, res);
		
		byte[] buf = res.getBytes();
		DataInputStream dis = new DataInputStream(new ByteArrayInputStream(buf));
		try {
			double length = dis.readDouble();
			//getLog().infoSimTime(this, "veh length=" + Double.toString(length));
			return length;
		} catch (IOException e) {
			getLog().error(e.getMessage());
		}
		
		return 0.0;
	}
	
	private double getVehicleWidth() {
		byte traciVehicleWidth = (byte) 0x4d;
		String res = getOs().sendSumoTraciRequest(new byte[] {traciVehicleWidth});
		
		getLog().infoSimTime(this, res);
		
		byte[] buf = res.getBytes();
		DataInputStream dis = new DataInputStream(new ByteArrayInputStream(buf));
		try {
			double width = dis.readDouble();
			//getLog().infoSimTime(this, "veh length=" + Double.toString(width));
			return width;
		} catch (IOException e) {
			getLog().error(e.getMessage());
		}
		
		return 0.0;
	}
	
	private int getVehicleHeading() {

		byte traciVehicleHeading = (byte) 0x43;
		String res = getOs().sendSumoTraciRequest(new byte[] {traciVehicleHeading});
		
		getLog().infoSimTime(this, res);
		
		byte[] buf = res.getBytes();
		DataInputStream dis = new DataInputStream(new ByteArrayInputStream(buf));
		try {
			int heading = dis.readInt();
			//getLog().infoSimTime(this, "veh length=" + Integer.toString(heading));
			return heading;
		} catch (IOException e) {
			getLog().error(e.getMessage());
		}
		
		return 0;
	}

    private String readString(final DataInputStream in) throws IOException {
        final int length = in.readInt();
        final byte[] bytes = readBytes(in, length);

        return new String(bytes, StandardCharsets.UTF_8);
    }

    private byte[] readBytes(final DataInputStream in, final int bytes) throws IOException {
        final byte[] result = new byte[bytes];

        in.read(result, 0, bytes);

        return result;
    }

    class SumoData {
    	
    	int heading;
    	double length;
    	double width;
    	
    }
    
}
