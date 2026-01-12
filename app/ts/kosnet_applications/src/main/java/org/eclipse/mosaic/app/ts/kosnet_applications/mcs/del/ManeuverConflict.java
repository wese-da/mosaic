package org.eclipse.mosaic.app.ts.kosnet_applications.mcs.del;

import java.util.Collections;
import java.util.Set;

import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.IntermediatePoint;

public class ManeuverConflict {

	private String subjectVehicleId;
	private Set<String> targetVehicleIds;
	private IntermediatePoint location;
	
	public ManeuverConflict(String subjectVehicleId, Set<String> targetVehicleIds, IntermediatePoint location) {
		this.subjectVehicleId = subjectVehicleId;
		this.targetVehicleIds = targetVehicleIds;
		this.location = location;
	}
	
	public String getSubjectVehicleId() {
		return subjectVehicleId;
	}
	
	public Set<String> getTargetVehicleIds() {
		return Collections.unmodifiableSet(targetVehicleIds);
	}
	
	public IntermediatePoint getLocation() {
		return this.location;
	}
	
}