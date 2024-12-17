package org.eclipse.mosaic.lib.objects.v2x.etsi.mcm;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;

import org.eclipse.mosaic.lib.objects.ToDataOutput;

/**
 * As proposed by ETSI TR 103 578.<br>
 * The cost value reflects the vehicle's necessity for cooperation (when C > 0) or its willingness to cooperate with other vehicles (C < 0).
 * A cost c = 0 indicates that the vehicle neither is looking for cooperation nor is it willing to cooperate with other vehicles.
 */
public class CooperationCost implements ToDataOutput, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8940682863073763127L;
	private float value;
	
	public CooperationCost(float value) {
		this.value = value;
	}
	
	public CooperationCost(DataInput in) throws IOException {
		this.value = in.readFloat();
	}
	
	public float getValue() {
		return this.value;
	}

	@Override
	public void toDataOutput(DataOutput dataOutput) throws IOException {
		dataOutput.writeFloat(value);
	}
	
}