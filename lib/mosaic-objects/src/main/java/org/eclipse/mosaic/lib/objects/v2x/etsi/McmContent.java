package org.eclipse.mosaic.lib.objects.v2x.etsi;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;

import javax.annotation.Nonnull;

import org.eclipse.mosaic.lib.geo.GeoPoint;
import org.eclipse.mosaic.lib.objects.ToDataOutput;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.ManeuverAdviceContainer;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.VehicleManeuverContainer;

public class McmContent implements ToDataOutput, Serializable {

	private static final long serialVersionUID = 4880783254521565146L;
	
	private final long generationTime;
	
	private final VehicleManeuverContainer vehicleManeuverContainer;
	private final ManeuverAdviceContainer maneuverAdviceContainer;

	public McmContent(
			final long generationTime,
			@Nonnull  VehicleManeuverContainer vehicleManueverContainer,
			@Nonnull ManeuverAdviceContainer maneuverAdviceContainer) {
		this.generationTime = generationTime;
		this.vehicleManeuverContainer = vehicleManueverContainer;
		this.maneuverAdviceContainer = maneuverAdviceContainer;
	}
	
	public McmContent(DataInput in) throws IOException {
		in.readInt(); // version
		this.generationTime = in.readLong();
		vehicleManeuverContainer = new VehicleManeuverContainer(in);
		maneuverAdviceContainer = new ManeuverAdviceContainer(in);
	}
	
	@Override
	public void toDataOutput(DataOutput dataOutput) throws IOException {
		dataOutput.writeInt(0);
		dataOutput.writeLong(generationTime);
		vehicleManeuverContainer.toDataOutput(dataOutput);
		maneuverAdviceContainer.toDataOutput(dataOutput);
	}
	
	public long getGenerationTime() {
		return generationTime;
	}
	
	public GeoPoint getPosition() {
		return vehicleManeuverContainer.getCurrentPoint();
	}
	
	public VehicleManeuverContainer getVehicleManeuverContainer() {
		return vehicleManeuverContainer;
	}

}
