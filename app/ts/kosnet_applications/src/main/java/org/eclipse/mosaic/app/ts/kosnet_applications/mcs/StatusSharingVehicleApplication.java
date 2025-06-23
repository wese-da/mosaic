package org.eclipse.mosaic.app.ts.kosnet_applications.mcs;

import org.eclipse.mosaic.lib.util.scheduling.Event;
import org.eclipse.mosaic.rti.TIME;

public class StatusSharingVehicleApplication extends ManeuverCoordinationService {

	public StatusSharingVehicleApplication(boolean useCells) {
		super(useCells);
	}

	@Override
	public void processEvent(Event event) throws Exception {
		shareStatus();
		getOperatingSystem().getEventManager().addEvent(new Event(getOperatingSystem().getSimulationTime() + TIME.SECOND, this));
	}

}