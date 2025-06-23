package org.eclipse.mosaic.lib.objects.v2x.etsi.mcm;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;

import org.eclipse.mosaic.lib.objects.ToDataOutput;

public class IntermediatePointLane implements IntermediatePoint {

	/**
	 * 
	 */
	private static final long serialVersionUID = 9196694938285049538L;
	private Lane lane;
	private Reason reason;
	private TimeOfPos timeOfPos;
	
	public IntermediatePointLane(Lane lane, Reason reason, TimeOfPos timeOfPos) {
		this.lane = lane;
		this.reason = reason;
		this.timeOfPos = timeOfPos;
	}
	
	public IntermediatePointLane(DataInput in) throws IOException {
		lane = new Lane(in);
		reason = Reason.fromId(in.readByte());
		timeOfPos = new TimeOfPos(in);
	}

	public Lane getLane() {
		return lane;
	}

	public Reason getReason() {
		return reason;
	}

	public TimeOfPos getTimeOfPos() {
		return timeOfPos;
	}

	@Override
	public void toDataOutput(DataOutput dataOutput) throws IOException {
		lane.toDataOutput(dataOutput);
		dataOutput.writeByte(reason.id);
		timeOfPos.toDataOutput(dataOutput);
	}
	
}