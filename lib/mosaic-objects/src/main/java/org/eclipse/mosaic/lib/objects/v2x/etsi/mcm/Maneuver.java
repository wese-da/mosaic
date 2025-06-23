package org.eclipse.mosaic.lib.objects.v2x.etsi.mcm;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;

import javax.annotation.Nonnull;

import org.eclipse.mosaic.lib.geo.GeoPoint;
import org.eclipse.mosaic.lib.objects.ToDataOutput;
import org.eclipse.mosaic.lib.util.SerializationUtils;

public class Maneuver implements ToDataOutput, Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -8750960533039517857L;
	private int maneuverId;
	private String executantId;
	private GeoPoint executantPosition;
	private double executantHeading;
	private Trajectory trajectory;
	private McmAutomationState automationAdvice;
	
	public Maneuver(@Nonnull int maneuverId,
			@Nonnull String stationId,
			@Nonnull GeoPoint executantPosition,
			@Nonnull double executantHeading,
			@Nonnull Trajectory trajectory,
			McmAutomationState automationAdvice) {
		this.maneuverId = maneuverId;
		this.executantId = stationId;
		this.executantPosition = executantPosition;
		this.executantHeading = executantHeading;
		this.trajectory = trajectory;
		this.automationAdvice = automationAdvice;
	}
	
	public Maneuver(DataInput in) throws IOException {
		this.maneuverId = in.readInt();
		this.executantId = in.readUTF();
		this.executantPosition = SerializationUtils.decodeGeoPoint(in);
		this.executantHeading = in.readDouble();
		this.trajectory = new Trajectory(in);
		if (in.readBoolean()) {
			this.automationAdvice = new McmAutomationState(in);
		}
	}
	
	public int getManeuverId() {
		return maneuverId;
	}
	
	public String getExecutantId() {
		return executantId;
	}
	
	public GeoPoint getExecutantPosition() {
		return executantPosition;
	}
	
	public double getExecutantHeading() {
		return executantHeading;
	}
	
	public Trajectory getTrajectory() {
		return trajectory;
	}
	
	public McmAutomationState getAutomationAdvice() {
		return automationAdvice;
	}
	
	@Override
	public void toDataOutput(DataOutput dataOutput) throws IOException {
		dataOutput.writeInt(maneuverId);
		dataOutput.writeUTF(executantId);
		SerializationUtils.encodeGeoPoint(dataOutput, executantPosition);
		dataOutput.writeDouble(executantHeading);
		trajectory.toDataOutput(dataOutput);
		boolean automationAdvicePresent = automationAdvice != null;
		dataOutput.writeBoolean(automationAdvicePresent);
		if (automationAdvicePresent) {
			automationAdvice.toDataOutput(dataOutput);
		}
	}
	
}