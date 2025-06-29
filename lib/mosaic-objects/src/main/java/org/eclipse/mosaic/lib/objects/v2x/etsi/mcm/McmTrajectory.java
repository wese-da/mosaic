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

package org.eclipse.mosaic.lib.objects.v2x.etsi.mcm;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;

import javax.annotation.Nonnull;

import org.eclipse.mosaic.lib.objects.ToDataOutput;

/**
 * 
 * As proposed by ETSI TR 103 578.<br>
 * Trajectories represent possible driving paths of the ego vehicle with a time horizon about seconds in the future.
 * Costs indicate how favourable the trajectory is for the ego vehicle.
 * 
 */
public class McmTrajectory implements ToDataOutput, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6506376438321460228L;
	private int trajectoryId;
	private Trajectory trajectory;
	private McmCategoryType mcmCateogoryType;
	private CooperationCost cooperationCost;
	
	public McmTrajectory(@Nonnull int trajectoryId,
			@Nonnull Trajectory trajectory,
			@Nonnull McmCategoryType mcmCategoryType,
			@Nonnull CooperationCost cooperationCost) {
		this.trajectoryId = trajectoryId;
		this.trajectory = trajectory;
		this.mcmCateogoryType = mcmCategoryType;
		this.cooperationCost = cooperationCost;
	}
	
	public McmTrajectory(DataInput in) throws IOException {
		trajectoryId = in.readInt();
		trajectory = new Trajectory(in);
		mcmCateogoryType = McmCategoryType.fromId(in.readByte());
		cooperationCost = new CooperationCost(in);
	}
	
	public int getTrajectoryId() {
		return trajectoryId;
	}
	
	public Trajectory getTrajectory() {
		return trajectory;
	}
	
	public McmCategoryType getMcmCategoryType() {
		return mcmCateogoryType;
	}
	
	public CooperationCost getCooperationCost() {
		return cooperationCost;
	}

	@Override
	public void toDataOutput(DataOutput dataOutput) throws IOException {
		dataOutput.writeInt(trajectoryId);
		trajectory.toDataOutput(dataOutput);
		dataOutput.writeByte(mcmCateogoryType.id);
		cooperationCost.toDataOutput(dataOutput);
	}
	
}