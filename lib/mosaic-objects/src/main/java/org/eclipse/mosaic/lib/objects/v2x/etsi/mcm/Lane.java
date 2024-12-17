package org.eclipse.mosaic.lib.objects.v2x.etsi.mcm;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;

import org.eclipse.mosaic.lib.objects.ToDataOutput;

public class Lane implements ToDataOutput, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3923398733977493373L;
	private double lanePosition;
	private int laneCount;
	
	public Lane(double lanePosition, int laneCount) {
		this.lanePosition = lanePosition;
		
		if (laneCount > 16 || laneCount < 1) {
			laneCount = 0; // TODO handle invalid values
		}
		
		this.laneCount = laneCount;
	}
	
	public Lane(DataInput in) throws IOException {
		this.lanePosition = in.readDouble();
		this.laneCount = in.readInt();
	}
	
	public double getLanePosition() {
		return lanePosition;
	}
	
	public int getLaneCount() {
		return laneCount;
	}

	@Override
	public void toDataOutput(DataOutput dataOutput) throws IOException {
		dataOutput.writeDouble(lanePosition);
		dataOutput.writeInt(laneCount);
	}
	
}