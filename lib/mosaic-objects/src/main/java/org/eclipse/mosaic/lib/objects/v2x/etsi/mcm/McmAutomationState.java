package org.eclipse.mosaic.lib.objects.v2x.etsi.mcm;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;

import javax.annotation.Nonnull;

import org.eclipse.mosaic.lib.objects.ToDataOutput;

public class McmAutomationState implements ToDataOutput, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1672619109462356257L;
	private boolean longitudinalAutomated;
	private boolean lateralAutomated;
	
	public McmAutomationState(
			@Nonnull boolean longitudinalAutomated,
			@Nonnull boolean lateralAutomated) {
		this.longitudinalAutomated = longitudinalAutomated;
		this.lateralAutomated = lateralAutomated;
	}
	
	public McmAutomationState(DataInput in) throws IOException {
		longitudinalAutomated = in.readBoolean();
		lateralAutomated = in.readBoolean();
	}
	
	public boolean isLongitudinalAutomated() {
		return longitudinalAutomated;
	}
	
	public boolean isLateralAutomated() {
		return lateralAutomated;
	}

	@Override
	public void toDataOutput(DataOutput dataOutput) throws IOException {
		dataOutput.writeBoolean(longitudinalAutomated);
		dataOutput.writeBoolean(lateralAutomated);
	}
	
}