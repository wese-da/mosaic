package org.eclipse.mosaic.app.ts.kosnet_applications.mcs;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedV2xMessage;
import org.eclipse.mosaic.lib.objects.v2x.etsi.Mcm;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.IntermediatePoint;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.IntermediatePointLane;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.IntermediatePointReference;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.Trajectory;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;

public class TrajectoryUtils {

	public static Envelope getTrajectoryBoundingBox(final Trajectory trajectory) {
		
		Envelope result = new Envelope();
		
		for (IntermediatePoint p : trajectory.getIntermediatePoints()) {
			if (p instanceof IntermediatePointReference) {
				
				IntermediatePointReference pr = (IntermediatePointReference) p;
				Coordinate c = new Coordinate(pr.getReferencePosition().getLatitude(), pr.getReferencePosition().getLongitude());
				result.expandToInclude(c);
			}
		}
		
		return result;
		
	}
	
	public List<ManeuverConflict> findConflictsBetweenTrajectories(List<ReceivedV2xMessage> messages){
		
		List<ManeuverConflict> conflicts = new ArrayList<>();
		
		for (ReceivedV2xMessage m : messages) {

			Trajectory t = ((Mcm) m.getMessage()).getContent().getVehicleManeuverContainer().getMcmTrajectories().get(0).getTrajectory();
			
			for (ReceivedV2xMessage other : messages) {
				
				if (m != other) {
					
					Trajectory tOther = ((Mcm) other.getMessage()).getContent().getVehicleManeuverContainer().getMcmTrajectories().get(0).getTrajectory();
					
					if (TrajectoryUtils.getTrajectoryBoundingBox(t).intersects(TrajectoryUtils.getTrajectoryBoundingBox(tOther))) {
						
						conflicts.add(new ManeuverConflict(null, null, null));
						
					}
					
				}
				
			}
			
		}

		return conflicts;
	}
	
}