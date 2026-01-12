package org.eclipse.mosaic.app.ts.kosnet_applications.mcs;

import org.eclipse.mosaic.app.ts.kosnet_applications.mcs.trajectory.TrajectoryGenerator;
import org.eclipse.mosaic.fed.application.app.AbstractApplication;
import org.eclipse.mosaic.fed.application.app.api.Application;
import org.eclipse.mosaic.fed.application.app.api.VehicleApplication;
import org.eclipse.mosaic.fed.application.app.api.os.VehicleOperatingSystem;
import org.eclipse.mosaic.lib.geo.GeoPoint;
import org.eclipse.mosaic.lib.objects.road.IRoadPosition;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.FrenetTrajectory;
import org.eclipse.mosaic.lib.objects.vehicle.VehicleData;
import org.eclipse.mosaic.lib.util.scheduling.Event;
import org.eclipse.mosaic.rti.TIME;

public class TrajectoryGenerationService extends AbstractApplication<VehicleOperatingSystem> implements VehicleApplication {

	private enum GenerationMode {
		SIMPLE,
		NETWORK_BASED
	}
	
	private GenerationMode generationMode = GenerationMode.SIMPLE;
	
	
	@Override
	public void onStartup() {
		
		getLog().infoSimTime(this, "Trajectory generation service started on {}", getOs().getId());
		
		getOs().getEventManager().addEvent(new Event(getOs().getSimulationTime() + 200 * TIME.MILLI_SECOND, this));
		
	}

	@Override
	public void onShutdown() {
		
        getLog().infoSimTime(this, "Trajectory generation service stopped");
		
	}

	@Override
	public void processEvent(Event event) throws Exception {
		
		VehicleData vehicleData = getOs().getVehicleData();
		IRoadPosition roadPosition = vehicleData.getRoadPosition();
		GeoPoint position = vehicleData.getPosition();
		double speed = vehicleData.getSpeed();
		double heading = vehicleData.getHeading();
		long currentTime = getOs().getSimulationTime();
		
		// Log current state
        getLog().infoSimTime(this, 
            "Vehicle state: pos=[{},{}], speed={} m/s, heading={}°, edge={}, lane={}",
            position.getLatitude(), position.getLongitude(),
            speed, heading,
            roadPosition.getConnectionId(), roadPosition.getLaneIndex()
        );
        
        try {
        	
        	FrenetTrajectory trajectory = generateSimpleTrajectory(currentTime);
        	logTrajectory(trajectory);
        	
        	for(Application app : getOs().getApplications()) {
            	if (app instanceof AgreementSeekingVehicleApplication ) {
            		this.getOs().getEventManager().newEvent(this.getOs().getSimulationTime() + 1 * TIME.MILLI_SECOND,
            				((AgreementSeekingVehicleApplication) app)::handleNewCurrentTrajectory)
                    .withResource(trajectory).schedule();
            	}
            }
        	
        } catch (Exception e) {
        	getLog().error("Failed to generate trajectory: {}", 
                    e.getMessage());
        	e.printStackTrace();
        }

		getOs().getEventManager().addEvent(new Event(getOs().getSimulationTime() + 200 * TIME.MILLI_SECOND, this));
		
	}

	private FrenetTrajectory generateSimpleTrajectory(long currentTime) {
		
		getLog().infoSimTime(this, "Generating simple trajectory");
		
		return TrajectoryGenerator.generateTrajectory(getOperatingSystem(), currentTime);
		
	}
	
	private void logTrajectory(FrenetTrajectory trajectory) {
        getLog().infoSimTime(this, "Generated trajectory: {}", trajectory);
        
        // Sample trajectory at specific times
        double[] sampleTimes = {0.0, 1.0, 2.0, 3.0, 4.0, 5.0};
        
        getLog().infoSimTime(this, "Trajectory samples:");
        for (double t : sampleTimes) {
            double absTime = trajectory.getStartTime() + t;
            
            try {
                var state = trajectory.evaluateAt(absTime);
                getLog().infoSimTime(this, 
                    "  t={:.1f}s: x={:.1f}m, y={:.2f}m, v={:.1f}m/s",
                    t, state.getX(), state.getY(), state.getVelocity()
                );
            } catch (IllegalArgumentException e) {
                getLog().debugSimTime(this, "  t={:.1f}s: outside trajectory", t);
            }
        }
    }

	@Override
	public void onVehicleUpdated(VehicleData previousVehicleData, VehicleData updatedVehicleData) {
		
	}

}