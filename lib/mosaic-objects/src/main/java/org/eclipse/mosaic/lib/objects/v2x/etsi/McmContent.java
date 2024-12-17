package org.eclipse.mosaic.lib.objects.v2x.etsi;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;

import javax.annotation.Nonnull;

import org.eclipse.mosaic.lib.objects.ToDataOutput;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.VehicleManeuverContainer;

public class McmContent implements ToDataOutput, Serializable {

	private static final long serialVersionUID = 4880783254521565146L;
	
	private final long generationTime;
	
	private final VehicleManeuverContainer vehicleManeuverContainer;

	public McmContent(
			final long generationTime,
			@Nonnull  VehicleManeuverContainer vehicleManueverContainer) {
		this.generationTime = generationTime;
		this.vehicleManeuverContainer = vehicleManueverContainer;
	}
	
	public McmContent(DataInput in) throws IOException {
		in.readInt(); // version
		this.generationTime = in.readLong();
		vehicleManeuverContainer = new VehicleManeuverContainer(in);
	}
	
	@Override
	public void toDataOutput(DataOutput dataOutput) throws IOException {
		dataOutput.writeInt(0);
		dataOutput.writeLong(generationTime);
		vehicleManeuverContainer.toDataOutput(dataOutput);
	}
	
	public long getGenerationTime() {
		return generationTime;
	}
	
	public VehicleManeuverContainer getVehicleManeuverContainer() {
		return vehicleManeuverContainer;
	}

}
