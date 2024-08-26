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
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.mosaic.app.ts.kosnet_applications.supplement.HeadingResponse;
import org.eclipse.mosaic.app.ts.kosnet_applications.supplement.Response;
import org.eclipse.mosaic.app.ts.kosnet_applications.supplement.SpeedResponse;
import org.eclipse.mosaic.app.ts.kosnet_applications.supplement.TraciConstants;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.AdHocModuleConfiguration;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.CamBuilder;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedAcknowledgement;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedV2xMessage;
import org.eclipse.mosaic.fed.application.app.AbstractApplication;
import org.eclipse.mosaic.fed.application.app.api.CommunicationApplication;
import org.eclipse.mosaic.fed.application.app.api.MosaicApplication;
import org.eclipse.mosaic.fed.application.app.api.VehicleApplication;
import org.eclipse.mosaic.fed.application.app.api.os.VehicleOperatingSystem;
import org.eclipse.mosaic.interactions.application.ApplicationInteraction;
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
import org.eclipse.mosaic.lib.objects.traffic.SumoTraciResult;
import org.eclipse.mosaic.lib.objects.v2x.MessageRouting;
import org.eclipse.mosaic.lib.objects.v2x.etsi.Denm;
import org.eclipse.mosaic.lib.objects.v2x.etsi.DenmContent;
import org.eclipse.mosaic.lib.objects.vehicle.VehicleData;
import org.eclipse.mosaic.lib.transform.Wgs84Projection;
import org.eclipse.mosaic.lib.util.scheduling.Event;

public class SlotVehicleApplication extends AbstractApplication<VehicleOperatingSystem> implements CommunicationApplication, VehicleApplication, MosaicApplication {

    private String lastSentMsgId;
    private SumoData currentData = new SumoData();
    
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
		
		MessageRouting messageRouting = getOs().getAdHocModule().createMessageRouting().viaChannel(AdHocChannel.SCH1).topoBroadCast();

		DenmContent denmContent = new DenmContent(
				0L, egoPosition, "roadID", SensorType.POSITION, 1,
				13.89f, 0.0f);
		Denm denm = new Denm(messageRouting, denmContent, 5L);
		return denm;
	}
	
	private GeoPolygon createSlotGeometry(final GeoPoint egoPosition) {
		
		double heading = getOperatingSystem().getVehicleData().getHeading();
		double length = currentData.length;
		double width = currentData.width;

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
	
	private byte[] assembleTraciCommand(String vehicleId, byte command) {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final DataOutputStream dos = new DataOutputStream(baos);

        try {
            dos.writeByte(TraciConstants.TRACI_VEHICLE);
            dos.writeByte(command);
            dos.writeInt(vehicleId.length());
            dos.write(vehicleId.getBytes(StandardCharsets.UTF_8)); // Vehicle Identifier
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return baos.toByteArray();
    }
	
	private double getVehicleLength() {
//		byte traciVehicleLength = (byte) 0x40;
//		String vehicleId = getOs().getId();
//		lastSentMsgId = getOs().sendSumoTraciRequest(assembleTraciCommand(vehicleId, traciVehicleLength));
//		
//		getLog().infoSimTime(this, res);
//		
//		byte[] buf = res.getBytes();
//		DataInputStream dis = new DataInputStream(new ByteArrayInputStream(buf));
//		try {
//			double length = dis.readDouble();
//			//getLog().infoSimTime(this, "veh length=" + Double.toString(length));
//			return length;
//		} catch (IOException e) {
//			getLog().error(e.getMessage());
//		}
		
		return 0.0;
	}
	
	private double getVehicleWidth() {
//		byte traciVehicleWidth = (byte) 0x4d;
//		String res = getOs().sendSumoTraciRequest(new byte[] {traciVehicleWidth});
//		
//		getLog().infoSimTime(this, res);
//		
//		byte[] buf = res.getBytes();
//		DataInputStream dis = new DataInputStream(new ByteArrayInputStream(buf));
//		try {
//			double width = dis.readDouble();
//			//getLog().infoSimTime(this, "veh length=" + Double.toString(width));
//			return width;
//		} catch (IOException e) {
//			getLog().error(e.getMessage());
//		}
		
		return 0.0;
	}
	
	private int getVehicleHeading() {

//		byte traciVehicleHeading = (byte) 0x43;
//		String res = getOs().sendSumoTraciRequest(new byte[] {traciVehicleHeading});
//		
//		getLog().infoSimTime(this, res);
//		
//		byte[] buf = res.getBytes();
//		DataInputStream dis = new DataInputStream(new ByteArrayInputStream(buf));
//		try {
//			int heading = dis.readInt();
//			//getLog().infoSimTime(this, "veh length=" + Integer.toString(heading));
//			return heading;
//		} catch (IOException e) {
//			getLog().error(e.getMessage());
//		}
		
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
    	
    	double heading;
    	double length;
    	double width;
    	double speed;
    	
    	@Override
    	public String toString() {
    		return "h=" + heading + " l=" + length + " w=" + width + " v=" + speed;
    	}
    	
    }

	@Override
	public void onSumoTraciResponded(SumoTraciResult sumoTraciResult) {
        if (sumoTraciResult.getRequestCommandId().equals(lastSentMsgId)) {
        	final Response response = decodeResponse(sumoTraciResult.getTraciCommandResult());
        	getLog().infoSimTime(this, "response: {}", response);
        	if (response != null) {
        		getLog().infoSimTime(
                        this,
                        "Received TraCI message from Sumo. {} of vehicle {} is {}",
                        response.getType(),
                        response.getVehicleId(),
                        response.getValue()
                );
        	}
        	getLog().infoSimTime(this, "veh {} curr {}", getOs().getId(), currentData);
        }		
	}
	
	private Response decodeResponse(final byte[] msg) {
        final ByteArrayInputStream bais = new ByteArrayInputStream(msg);
        final DataInputStream dis = new DataInputStream(bais);

        try {
            byte response = dis.readByte(); // should be 0xb4 for response vehicle variable
            getLog().infoSimTime(this, "response: {} expected: {} / {}", response, TraciConstants.TRACI_VEHICLE, 0xa4);
            if (response == TraciConstants.TRACI_VEHICLE_RESPONSE) {
            	int command = dis.readUnsignedByte(); 
                getLog().infoSimTime(this, "command: {}", command);
                String vehicleId = readString(dis); // vehicle for which the response is
                getLog().infoSimTime(this, "vehicle id: {}", vehicleId);
                int variableType = dis.readUnsignedByte(); // type of response, should be 0x0b for double
                getLog().infoSimTime(this, "variable type: {}", variableType);
                if (command == TraciConstants.TRACI_VEHICLE_SPEED) {
                    double speed = dis.readDouble(); // the actual value, speed in m/s here
                	currentData.speed = speed;
                    return new SpeedResponse(vehicleId, speed);
                } else if (command == TraciConstants.TRACI_VEHICLE_HEADING) {
                	double heading = dis.readDouble(); // the actual value, speed in m/s here
                	currentData.heading = heading;
                    return new HeadingResponse(vehicleId, heading);
                } else if (command == TraciConstants.TRACI_VEHICLE_LENGTH) {
                	double length = dis.readDouble();
                	currentData.length = length;
                	return null;
                } else if (command == TraciConstants.TRACI_VEHICLE_WIDTH) {
                	double width = dis.readDouble();
                	currentData.width = width;
                	return null;
                }
                else {
                	return null;
                }
            } else {
            	return null;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

	@Override
	public void onInteractionReceived(ApplicationInteraction applicationInteraction) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onVehicleUpdated(VehicleData previousVehicleData, VehicleData updatedVehicleData) {
		final byte[] traciMsg = assembleTraciCommand(getOs().getId(), TraciConstants.TRACI_VEHICLE_LENGTH); // assemble the TraCI msg for sumo
 
		getLog().infoSimTime(this, "traci vehicle: {}", TraciConstants.TRACI_VEHICLE);
		getLog().infoSimTime(this, "msg for vehicle {}: {}", getOs().getId(), traciMsg);
		
        lastSentMsgId = getOs().sendSumoTraciRequest(traciMsg);
	}
	
}
