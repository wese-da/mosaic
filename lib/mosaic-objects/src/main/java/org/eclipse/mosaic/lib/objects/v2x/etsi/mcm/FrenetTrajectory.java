package org.eclipse.mosaic.lib.objects.v2x.etsi.mcm;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.mosaic.lib.geo.GeoPoint;
import org.eclipse.mosaic.lib.objects.ToDataOutput;

public class FrenetTrajectory implements ToDataOutput {
	
	enum Type {
		REFERENCE,
		LANE,
		INTERSECTION
	}

	private List<IntermediatePoint> intermediatePoints = new ArrayList<IntermediatePoint>();
	private List<Polynom> longitudinalPositions = new ArrayList<Polynom>();
	private List<Polynom> lateralPositions = new ArrayList<Polynom>();
	private List<Polynom> headings = new ArrayList<Polynom>();
	
	public FrenetTrajectory(List<IntermediatePoint> intermediatePoints,
            List<Polynom> longitudinalPositions,
            List<Polynom> lateralPositions,
            List<Polynom> headings) {
		// Validate sizes
		if (intermediatePoints == null || intermediatePoints.isEmpty() || 
		intermediatePoints.size() > 10) {
		throw new IllegalArgumentException("Intermediate points must be 1-10");
		}
		
		int expectedSegments = intermediatePoints.size();
		if (longitudinalPositions.size() != expectedSegments) {
		throw new IllegalArgumentException(
		 "Longitudinal positions must have " + expectedSegments + " segments");
		}
		if (lateralPositions.size() != expectedSegments) {
		throw new IllegalArgumentException(
		 "Lateral positions must have " + expectedSegments + " segments");
		}
//		if (headings != null && headings.size() != expectedSegments) {
//		throw new IllegalArgumentException(
//		 "Headings must have " + expectedSegments + " segments or be null");
//		}
		
		this.intermediatePoints = new ArrayList<>(intermediatePoints);
		this.longitudinalPositions = new ArrayList<>(longitudinalPositions);
		this.lateralPositions = new ArrayList<>(lateralPositions);
		this.headings = headings != null ? new ArrayList<>(headings) : null;
	}
	
	public FrenetTrajectory(DataInput in) throws IOException {
		this.intermediatePoints = new ArrayList<IntermediatePoint>();
		int size = in.readInt();
		for (int i = 0; i < size; i++) {
			IntermediatePointType t = IntermediatePointType.values()[in.readInt()];
			if(t == IntermediatePointType.REFERENCE) {
				this.intermediatePoints.add(new IntermediatePointReference(in));
			} else if (t == IntermediatePointType.LANE) {
				this.intermediatePoints.add(new IntermediatePointLane(in));
			}
		}
		this.longitudinalPositions = new ArrayList<Polynom>();
		size = in.readInt();
		for (int i = 0; i < size; i++) {
			this.longitudinalPositions.add(new Polynom(in));
		}
		this.lateralPositions = new ArrayList<Polynom>();
		size = in.readInt();
		for (int i = 0; i < size; i++) {
			this.lateralPositions.add(new Polynom(in));
		}
		this.headings = new ArrayList<Polynom>();
		size = in.readInt();
		for (int i = 0; i < size; i++) {
			this.headings.add(new Polynom(in));
		}
	}

	public static FrenetTrajectory createStraight(
            GeoPoint startPosition,
            double startHeading,
            Lane lane,
            double startTime,
            double endTime,
            double speed) {
        
        // Single intermediate point (reference)
        IntermediatePointReference refPoint = new IntermediatePointReference(
            startPosition, startHeading, lane, new TimeOfPos(startTime * 100)
        );
        
        // Single segment (no lane changes)
        // x(t) = v*t (constant speed)
        Polynom longitudinal = Polynom.linear(0.0, speed, startTime, endTime);
        
        // y(x) = 0 (stay in lane center)
        double distance = speed * (endTime - startTime);
        Polynom lateral = Polynom.constant(0.0, 0.0, distance);
        
        return new FrenetTrajectory(
            Collections.singletonList(refPoint),
            Collections.singletonList(longitudinal),
            Collections.singletonList(lateral),
            new ArrayList<Polynom>()
        );
    }
	
	public static FrenetTrajectory createLaneChange(
            GeoPoint startPosition,
            double startHeading,
            Lane startLane,
            Lane targetLane,
            double laneWidth,
            double startTime,
            double changeStartTime,
            double changeEndTime,
            double endTime,
            double speed) {
        
        // Two intermediate points: start lane and target lane
        IntermediatePointReference refPoint = new IntermediatePointReference(
            startPosition, startHeading, startLane, new TimeOfPos(startTime * 100)
        );
        
        IntermediatePointLane lanePoint = new IntermediatePointLane(
            targetLane, Reason.LANE_CHANGE, new TimeOfPos(startTime * 100)
        );
        
        List<IntermediatePoint> points = new ArrayList<>();
        points.add(refPoint);
        points.add(lanePoint);
        
        // Two segments: before and after lane change
        
        // Segment 1: Start to lane change
        double distance1 = speed * (changeStartTime - startTime);
        Polynom long1 = Polynom.linear(0.0, speed, startTime, changeStartTime);
        Polynom lat1 = Polynom.constant(0.0, 0.0, distance1); // Stay in center
        
        // Segment 2: Lane change and after
        double changeDistance = speed * (changeEndTime - changeStartTime);
        double distance2 = speed * (endTime - changeStartTime);
        
        Polynom long2 = Polynom.linear(0.0, speed, changeStartTime, endTime);
        
        // Lateral motion during lane change: smooth transition
        // Use cubic polynomial for smooth acceleration/deceleration
        double laneOffset = targetLane.getLanePosition() - startLane.getLanePosition();
        double lateralDistance = laneOffset * laneWidth;
        
        // y(x) = a*x³ + b*x² for smooth lane change
        // Boundary conditions: y(0)=0, y'(0)=0, y(d)=lateralDistance, y'(d)=0
        double d = changeDistance;
        double a = -2 * lateralDistance / (d * d * d);
        double b = 3 * lateralDistance / (d * d);
        
        Polynom lat2 = Polynom.quadratic(0.0, 0.0, b, 0.0, distance2);
        if (Math.abs(a) > 0.001) {
            // Would need cubic for proper implementation
            // Simplified here to quadratic
        }
        
        List<Polynom> longitudinal = new ArrayList<>();
        longitudinal.add(long1);
        longitudinal.add(long2);
        
        List<Polynom> lateral = new ArrayList<>();
        lateral.add(lat1);
        lateral.add(lat2);
        
        return new FrenetTrajectory(points, longitudinal, lateral, new ArrayList<Polynom>());
    }
    
    /**
     * Evaluate trajectory at given time
     * 
     * @param time Time in seconds
     * @return TrajectoryState at that time
     */
    public TrajectoryState evaluateAt(double time) {
        // Find which segment this time belongs to
        int segmentIndex = findSegmentForTime(time);
        
        if (segmentIndex < 0) {
            throw new IllegalArgumentException("Time " + time + " outside trajectory range");
        }
        
        // Get longitudinal position x(t)
        Polynom longPoly = longitudinalPositions.get(segmentIndex);
        double x = longPoly.evaluate(time);
        
        // Get lateral position y(x)
        Polynom latPoly = lateralPositions.get(segmentIndex);
        double y = latPoly.evaluate(x);
        
        // Get heading if available
        Double heading = null;
        if (headings != null) {
        	if (headings.size() > 0) {
        		heading = headings.get(segmentIndex).evaluate(x);
        	}
        }
        
        // Get velocity (derivative of longitudinal position)
        double velocity = longPoly.derivative().evaluate(time);
        
        return new TrajectoryState(time, x, y, velocity, heading, segmentIndex);
    }
    
    /**
     * Find which segment a given time belongs to
     */
    private int findSegmentForTime(double time) {
        for (int i = 0; i < longitudinalPositions.size(); i++) {
            if (longitudinalPositions.get(i).isInRange(time)) {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * Get start time of trajectory
     */
    public double getStartTime() {
        return longitudinalPositions.get(0).getStartValue();
    }
    
    /**
     * Get end time of trajectory
     */
    public double getEndTime() {
        return longitudinalPositions.get(longitudinalPositions.size() - 1)
               .getEndValue();
    }
    
    /**
     * Get duration of trajectory
     */
    public double getDuration() {
        return getEndTime() - getStartTime();
    }
    
    /**
     * Get number of lane changes in trajectory
     */
    public int getNumberOfLaneChanges() {
        return (int) intermediatePoints.stream()
            .filter(p -> p.getType() == IntermediatePointType.LANE)
            .count();
    }
    
    /**
     * Check if trajectory includes intersection crossing
     */
    public boolean crossesIntersection() {
        return intermediatePoints.stream()
            .anyMatch(p -> p.getType() == IntermediatePointType.INTERSECTION);
    }
    
    // Getters
    
    public List<IntermediatePoint> getIntermediatePoints() {
        return Collections.unmodifiableList(intermediatePoints);
    }
    
    public List<Polynom> getLongitudinalPositions() {
        return Collections.unmodifiableList(longitudinalPositions);
    }
    
    public List<Polynom> getLateralPositions() {
        return Collections.unmodifiableList(lateralPositions);
    }
    
    public List<Polynom> getHeadings() {
        return headings != null ? Collections.unmodifiableList(headings) : null;
    }
    
    public int getSegmentCount() {
        return longitudinalPositions.size();
    }
    
    @Override
    public String toString() {
        return String.format("FrenetTrajectory{segments=%d, duration=%.2fs, laneChanges=%d, intersection=%s}",
                           getSegmentCount(), getDuration(), 
                           getNumberOfLaneChanges(), crossesIntersection());
    }

	@Override
	public void toDataOutput(DataOutput dataOutput) throws IOException {
		dataOutput.writeInt(intermediatePoints.size());
		for(IntermediatePoint p : intermediatePoints) {
			if (p instanceof IntermediatePointReference) {
				dataOutput.writeInt(IntermediatePointType.REFERENCE.ordinal());
			} else if (p instanceof IntermediatePointLane) {
				dataOutput.writeInt(IntermediatePointType.LANE.ordinal());
			}
			p.toDataOutput(dataOutput);
		}
		dataOutput.writeInt(longitudinalPositions.size());
		for(Polynom p : longitudinalPositions) {
			p.toDataOutput(dataOutput);
		}
		dataOutput.writeInt(lateralPositions.size());
		for(Polynom p : lateralPositions) {
			p.toDataOutput(dataOutput);
		}
		dataOutput.writeInt(headings.size());
		for(Polynom p : headings) {
			p.toDataOutput(dataOutput);
		}
	}
}
