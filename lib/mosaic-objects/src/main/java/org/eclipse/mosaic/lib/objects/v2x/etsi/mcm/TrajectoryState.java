package org.eclipse.mosaic.lib.objects.v2x.etsi.mcm;

/**
 * Trajectory state at a specific time
 */
public class TrajectoryState {
    private double time;           // Time (seconds)
    private double x;              // Longitudinal position (meters)
    private double y;              // Lateral position (meters)
    private double velocity;       // Longitudinal velocity (m/s)
    private Double heading;        // Optional heading (radians)
    private int segmentIndex;      // Which segment this state is in
    
    public TrajectoryState(double time, double x, double y, double velocity,
                          Double heading, int segmentIndex) {
        this.time = time;
        this.x = x;
        this.y = y;
        this.velocity = velocity;
        this.heading = heading;
        this.segmentIndex = segmentIndex;
    }
    
    public double getTime() { return time; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getVelocity() { return velocity; }
    public Double getHeading() { return heading; }
    public int getSegmentIndex() { return segmentIndex; }
    
    @Override
    public String toString() {
        return String.format("State{t=%.2fs, x=%.2fm, y=%.2fm, v=%.2fm/s, segment=%d}",
                           time, x, y, velocity, segmentIndex);
    }
}