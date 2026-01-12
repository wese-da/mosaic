package org.eclipse.mosaic.app.ts.kosnet_applications.mcs.trajectory;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.mosaic.fed.application.app.api.os.VehicleOperatingSystem;
import org.eclipse.mosaic.lib.geo.GeoPoint;
import org.eclipse.mosaic.lib.geo.MutableGeoPoint;
import org.eclipse.mosaic.lib.objects.road.IRoadPosition;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.FrenetTrajectory;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.IntermediatePoint;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.IntermediatePointReference;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.Lane;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.Polynom;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.TimeOfPos;
import org.eclipse.mosaic.lib.objects.vehicle.VehicleData;
import org.eclipse.mosaic.lib.objects.vehicle.VehicleRoute;
import org.eclipse.mosaic.rti.TIME;

public class TrajectoryGenerator {

    private static final double PREDICTION_HORIZON = 5.0; // seconds
    private static final double SAMPLE_INTERVAL = 0.5;     // seconds
    private static final double DEFAULT_LANE_WIDTH = 3.5;  // meters
	
	public static FrenetTrajectory generateTrajectory(VehicleOperatingSystem os, long currentTime) {
		
		VehicleData vehicleData = os.getVehicleData();
        IRoadPosition roadPos = vehicleData.getRoadPosition();
        double currentSpeed = vehicleData.getSpeed(); // m/s
        double currentHeading = vehicleData.getHeading(); // degrees
        GeoPoint currentPosition = vehicleData.getPosition();
        
        VehicleRoute route = os.getNavigationModule().getCurrentRoute();
        
        // Step 3: Predict future positions
        List<TrajectoryWaypoint> waypoints = predictWaypoints(
            vehicleData, route, currentTime
        );
        
        // Step 4: Convert to Frenet trajectory
        return convertToFrenetTrajectory(waypoints, roadPos, currentPosition, currentHeading, currentTime);
		
	}
	
	public static FrenetTrajectory generateTrajectoryWithAcceleration(VehicleOperatingSystem os, long currentTime, double acceleration) {
        
        VehicleData vehicleData = os.getVehicleData();
        IRoadPosition roadPos = vehicleData.getRoadPosition();
        double currentSpeed = vehicleData.getSpeed();
        double currentHeading = vehicleData.getHeading();
        GeoPoint currentPosition = vehicleData.getPosition();
        
        // Create reference point
        GeoPoint refPos = new MutableGeoPoint(
            currentPosition.getLatitude(), 
            currentPosition.getLongitude()
        );
        
        int laneIndex = roadPos.getLaneIndex();
        Lane lane = new Lane(roadPos.getConnectionId(), laneIndex + 1, 3);
        
        IntermediatePointReference refPoint = new IntermediatePointReference(
            refPos, currentHeading, lane, new TimeOfPos(currentTime)
        );
        
        // Polynomial for motion with constant acceleration
        // x(t) = v0*t + 0.5*a*t²
        double startTimeSeconds = currentTime / (double)TIME.SECOND;
        double endTimeSeconds = startTimeSeconds + PREDICTION_HORIZON;
        
        Polynom longitudinal = Polynom.quadratic(
            0.0,                    // x(0) = 0
            currentSpeed,           // v0
            0.5 * acceleration,     // 0.5*a
            startTimeSeconds,
            endTimeSeconds
        );
        
        // Calculate final distance
        double finalSpeed = currentSpeed + acceleration * PREDICTION_HORIZON;
        if (finalSpeed < 0) finalSpeed = 0; // Can't go backwards
        double avgSpeed = (currentSpeed + finalSpeed) / 2.0;
        double totalDistance = avgSpeed * PREDICTION_HORIZON;
        
        Polynom lateral = Polynom.constant(0.0, 0.0, totalDistance);
        
        // Build trajectory
        return new FrenetTrajectory(
            List.of(refPoint),
            List.of(longitudinal),
            List.of(lateral),
            null
        );
    }
	
	private static List<TrajectoryWaypoint> predictWaypoints(VehicleData vehicleData, VehicleRoute route, long currentTime) {
		
		List<TrajectoryWaypoint> waypoints = new ArrayList<>();
		
		// Current state
        IRoadPosition currentPos = vehicleData.getRoadPosition();
        double speed = vehicleData.getSpeed();
        double positionOnLane = currentPos.getOffset(); // meters from lane start
        
        // Get current and upcoming edges
        String currentEdgeId = currentPos.getConnectionId();
        int currentEdgeIndex = route.getConnectionIds().indexOf(currentEdgeId);
        List<String> edgeIds = route.getConnectionIds();
        
        // Sample future positions
        double timeStep = 0.0;
        double distanceTraveled = 0.0;
        
        while (timeStep <= PREDICTION_HORIZON) {
            // Simplified motion model: constant speed
            // (In production: use SUMO's vehicle following model)
            double distance = speed * timeStep;
            
            // Find which edge this distance corresponds to
            EdgePosition edgePos = findPositionOnRoute(
                currentEdgeIndex, edgeIds, positionOnLane, distance
            );
            
            // Create waypoint
            TrajectoryWaypoint waypoint = new TrajectoryWaypoint();
            waypoint.time = currentTime + (long)(timeStep * TIME.SECOND);
            waypoint.edgeId = edgePos.edgeId;
            waypoint.laneIndex = currentPos.getLaneIndex();
            waypoint.offset = edgePos.offset;
            waypoint.speed = speed;
            waypoint.distanceFromStart = distance;
            
            waypoints.add(waypoint);
            
            timeStep += SAMPLE_INTERVAL;
        }
		
		return waypoints;
		
	}
	
	private static EdgePosition findPositionOnRoute(
            int startEdgeIndex, 
            List<String> edgeIds,
            double startOffset,
            double distance) {
        
        EdgePosition result = new EdgePosition();
        
        // Simplified: assume each edge is 100m long
        // In reality: query SUMO network for actual edge length
        double edgeLength = 100.0;
        
        double remainingDistance = distance;
        double currentOffset = startOffset;
        int currentEdgeIndex = startEdgeIndex;
        
        // Walk along edges until distance is consumed
        while (remainingDistance > 0 && currentEdgeIndex < edgeIds.size()) {
            double availableOnEdge = edgeLength - currentOffset;
            
            if (remainingDistance <= availableOnEdge) {
                // Distance fits on current edge
                result.edgeId = edgeIds.get(currentEdgeIndex);
                result.offset = currentOffset + remainingDistance;
                result.edgeIndex = currentEdgeIndex;
                break;
            } else {
                // Need to move to next edge
                remainingDistance -= availableOnEdge;
                currentEdgeIndex++;
                currentOffset = 0.0; // Start at beginning of next edge
            }
        }
        
        // If we ran out of edges, use last position
        if (currentEdgeIndex >= edgeIds.size()) {
            result.edgeId = edgeIds.get(edgeIds.size() - 1);
            result.offset = edgeLength; // End of last edge
            result.edgeIndex = edgeIds.size() - 1;
        }
        
        return result;
    }
	
	private static FrenetTrajectory convertToFrenetTrajectory(
            List<TrajectoryWaypoint> waypoints,
            IRoadPosition roadPos,
            GeoPoint startPosition,
            double startHeading,
            long startTime) {
        
        // Create reference point (current position)
        GeoPoint refPos = new MutableGeoPoint(
            startPosition.getLatitude(), 
            startPosition.getLongitude()
        );
        
        // Lane information
        int laneIndex = roadPos.getLaneIndex();
        // Assume 3 lanes (should query from SUMO network)
        Lane lane = new Lane(roadPos.getConnectionId(), laneIndex + 1, 3); // +1 because ASN.1 is 1-indexed
        
        IntermediatePointReference refPoint = new IntermediatePointReference(
            refPos, startHeading, lane, new TimeOfPos(0) // timeOfPos = 0 (start time)
        );
        
        // Create polynomials for longitudinal position x(t)
        // Simplified: linear motion x(t) = v*t
        double speed = waypoints.isEmpty() ? 0.0 : waypoints.get(0).speed;
        double startTimeSeconds = startTime / (double)TIME.SECOND;
        double endTimeSeconds = startTimeSeconds + PREDICTION_HORIZON;
        
        Polynom longitudinal = Polynom.linear(
            0.0,    // x(0) = 0 (start at reference)
            speed,  // dx/dt = v (constant velocity)
            startTimeSeconds,
            endTimeSeconds
        );
        
        // Create polynomial for lateral position y(x)
        // No lane change: y = 0 (stay in lane center)
        double totalDistance = speed * PREDICTION_HORIZON;
        Polynom lateral = Polynom.constant(
            0.0,    // y = 0 (lane center)
            0.0,
            totalDistance
        );
        
        // Build trajectory
        List<IntermediatePoint> points = new ArrayList<>();
        points.add(refPoint);
        
        List<Polynom> longitudinalList = new ArrayList<>();
        longitudinalList.add(longitudinal);
        
        List<Polynom> lateralList = new ArrayList<>();
        lateralList.add(lateral);
        
        return new FrenetTrajectory(points, longitudinalList, lateralList, new ArrayList<Polynom>());
    }
	
}

/**
 * Waypoint in trajectory prediction
 */
class TrajectoryWaypoint {
    long time;                  // Simulation time
    String edgeId;              // SUMO edge ID
    int laneIndex;              // Lane index on edge
    double offset;              // Position on lane (meters)
    double speed;               // Speed at waypoint
    double distanceFromStart;   // Total distance from start
}

/**
 * Position on a specific edge
 */
class EdgePosition {
    String edgeId;
    int edgeIndex;
    double offset;
}
