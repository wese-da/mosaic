package org.eclipse.mosaic.lib.objects.v2x.etsi.mcm;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.mosaic.lib.objects.ToDataOutput;

/**
 * Analog to ETSI TR 103 578.<br>
 * This class contains IntermediatePointLane as sole intermediate point data element for practical reasons, since SUMO can best work with lane position data
 * rather than geographical positions.
 */
public class Trajectory implements ToDataOutput, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7178950715700996682L;
	
	private List<IntermediatePoint> intermediatePoints;

	public Trajectory(List<IntermediatePoint> intermediatePoints) {
		this.intermediatePoints = intermediatePoints;
	}
	
	public Trajectory(DataInput in) throws IOException {
		intermediatePoints = new ArrayList<IntermediatePoint>();
		int size = in.readInt();
		for (int i = 0; i < size; i++) {
			intermediatePoints.add(new IntermediatePointLane(in));
		}
	}
	
	public List<IntermediatePoint> getIntermediatePoints() {
		return intermediatePoints;
	}

	@Override
	public void toDataOutput(DataOutput dataOutput) throws IOException {
		dataOutput.writeInt(intermediatePoints.size());
		for (IntermediatePoint ipl : intermediatePoints) {
			ipl.toDataOutput(dataOutput);
		}
	}
	
}