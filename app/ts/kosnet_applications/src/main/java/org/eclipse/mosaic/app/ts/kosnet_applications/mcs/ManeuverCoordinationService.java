package org.eclipse.mosaic.app.ts.kosnet_applications.mcs;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.AdHocModuleConfiguration;
import org.eclipse.mosaic.fed.application.app.AbstractApplication;
import org.eclipse.mosaic.fed.application.app.api.os.VehicleOperatingSystem;
import org.eclipse.mosaic.lib.enums.AdHocChannel;
import org.eclipse.mosaic.lib.objects.road.IConnection;
import org.eclipse.mosaic.lib.objects.v2x.etsi.Mcm;
import org.eclipse.mosaic.lib.objects.v2x.etsi.McmContent;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.CooperationCost;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.IntermediatePoint;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.IntermediatePointLane;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.Lane;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.Maneuver;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.ManeuverAdviceContainer;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.McmCategoryType;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.McmTrajectory;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.Reason;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.TimeOfPos;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.Trajectory;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.VehicleManeuverContainer;
import org.eclipse.mosaic.lib.util.scheduling.Event;
import org.eclipse.mosaic.rti.TIME;

public abstract class ManeuverCoordinationService extends AbstractApplication<VehicleOperatingSystem> {

	private final boolean useCells;
	
	public ManeuverCoordinationService(boolean useCells) {
		this.useCells = useCells;
	}
	
	@Override
	public void onStartup() {
		getLog().infoSimTime(this, "Initialize application");
		if (this.useCells) {
			getOperatingSystem().getCellModule().enable();
			getLog().infoSimTime(this, "Cell module enabled");
		} else {
			getOperatingSystem().getAdHocModule().enable(new AdHocModuleConfiguration()
					.addRadio()
					.channel(AdHocChannel.CCH)
					.power(50.)
					.create());
			getLog().infoSimTime(this, "AdHoc module enabled");
		}
		
		// trigger processEvent
		getOperatingSystem().getEventManager().addEvent(new Event(getOperatingSystem().getSimulationTime() + TIME.SECOND, this));
	}
	
	/**
	 * provide status of vehicular objects via CAMs
	 * provide event notifications via DENMs
	 * provide data about VRUs via VAMs
	 * provide status of perceived unconnected objects via CPMs
	 */
	protected void shareStatus() {
		getOperatingSystem().getAdHocModule().sendCam();
	}
	
	protected void shareIntent() {
		Mcm message = assembleMessage();
		getOperatingSystem().getAdHocModule().sendV2xMessage(message);
	}

	protected Mcm assembleMessage() {
		// vehicle maneuver container
		List<McmTrajectory> mcmTrajectories = new ArrayList<McmTrajectory>();
		mcmTrajectories.add(assembleCurrentTrajectory());
		VehicleManeuverContainer vmc = new VehicleManeuverContainer(getOperatingSystem().getPosition(), mcmTrajectories);
		// maneuver advice container
		Set<Maneuver> maneuvers = new HashSet<Maneuver>();
		ManeuverAdviceContainer mac = new ManeuverAdviceContainer(maneuvers);
		// content
		McmContent content = new McmContent(getOperatingSystem().getSimulationTime(), vmc, mac);
		Mcm mcm = new Mcm(getOperatingSystem().getAdHocModule().createMessageRouting().topoBroadCast(), content, 200);
		return mcm;
	}
	
	private McmTrajectory assembleCurrentTrajectory() {
		
		double posFromCurrentEdgeStart = getOperatingSystem().getNavigationModule().getRoadPosition().getOffset();
		List<String> edgeIds = getOperatingSystem().getNavigationModule().getCurrentRoute().getConnectionIds();
		int startIdx = edgeIds.indexOf(getOperatingSystem().getNavigationModule().getRoadPosition().getConnectionId());
		
		double currentSpeed = getOperatingSystem().getVehicleData().getSpeed();
		double accelerationTime = Math.max(13.89 - currentSpeed, accelTime(13.89, currentSpeed)); // TODO
		
		double length = -posFromCurrentEdgeStart;
		double currentPos = posFromCurrentEdgeStart;
		List<IntermediatePoint> ipl = new ArrayList<IntermediatePoint>();
		// parse the list from the current edge to the end of the route
		for (String edgeId : edgeIds.subList(startIdx, edgeIds.size())) {
			IConnection current = getOperatingSystem().getNavigationModule().getConnection(edgeId);
			ipl.add(new IntermediatePointLane(new Lane(currentPos, current.getLanes()), Reason.NONE,
					new TimeOfPos(getOperatingSystem().getSimulationTime())));
			length += current.getLength();
			if (length / currentSpeed >= Mcm.MCM_HORIZON) {
				break;
			}
			currentPos = 0;
		}
		
		Trajectory trajectory = new Trajectory(ipl);
		CooperationCost cost = new CooperationCost(0.0f);
		McmTrajectory current = new McmTrajectory(0, trajectory, McmCategoryType.NONE, cost);
		return current;
	}
	
	private double accelTime(double maxSpeed, double currentSpeed) {
		
		double a = getOperatingSystem().getVehicleParameters().getMaxAcceleration();
		double t = (maxSpeed - currentSpeed) / a;
		return t;
		
	}
	
	@Override
	public void onShutdown() {
		
	}
	
}