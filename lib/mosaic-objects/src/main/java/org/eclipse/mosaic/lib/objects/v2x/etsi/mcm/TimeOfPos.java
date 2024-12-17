package org.eclipse.mosaic.lib.objects.v2x.etsi.mcm;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;

import org.eclipse.mosaic.lib.objects.ToDataOutput;

public class TimeOfPos implements ToDataOutput, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private double value;
	
	public TimeOfPos(double v) {
		this.value = v;
	}
	
	public TimeOfPos(DataInput in) throws IOException {
		this.value = in.readDouble();
	}
	
	public double getValue() {
		return this.value;
	}

	@Override
	public void toDataOutput(DataOutput dataOutput) throws IOException {
		dataOutput.writeDouble(value);
	}
	
}