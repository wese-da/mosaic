package org.eclipse.mosaic.app.ts.kosnet_applications.mcs;

import org.eclipse.mosaic.lib.util.scheduling.Event;
import org.eclipse.mosaic.rti.TIME;

public class IntentSharingVehicleApplication extends ManeuverCoordinationService {

	public IntentSharingVehicleApplication(boolean useCells) {
		super(useCells);
	}

	@Override
	public void processEvent(Event event) throws Exception {
		shareStatus();
		shareIntent();
		getOperatingSystem().getEventManager().addEvent(new Event(getOperatingSystem().getSimulationTime() + TIME.SECOND, this));
	}

}
