package org.eclipse.mosaic.app.ts.kosnet_applications.glosa;

import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.AdHocModuleConfiguration;
import org.eclipse.mosaic.fed.application.app.AbstractApplication;
import org.eclipse.mosaic.fed.application.app.api.os.RoadSideUnitOperatingSystem;
import org.eclipse.mosaic.lib.enums.AdHocChannel;
import org.eclipse.mosaic.lib.objects.v2x.etsi.Spatm;
import org.eclipse.mosaic.lib.objects.v2x.etsi.SpatmContent;
import org.eclipse.mosaic.lib.util.scheduling.Event;
import org.eclipse.mosaic.rti.TIME;

public class GlosaRsuApplication extends AbstractApplication<RoadSideUnitOperatingSystem> {

	@Override
	public void processEvent(Event event) throws Exception {

		Spatm message = assembleSpatm();
		
	}
	
	private Spatm assembleSpatm() {
		
		SpatmContent content = new SpatmContent(0, null, isTornDown(), isValidStateAndLog(), isTornDown(), 0, 0, 0, 0);
		
		Spatm spatm = new Spatm(
				getOperatingSystem().getAdHocModule().createMessageRouting().topoBroadCast(),
				content, 200L);
		
		return spatm;
		
	}

	@Override
	public void onStartup() {

		// enable ad-hoc communication
		getOperatingSystem().getAdHocModule().enable(new AdHocModuleConfiguration()
				.addRadio()
				.channel(AdHocChannel.CCH)
				.power(50.)
				.create());
		getLog().infoSimTime(this, "AdHoc module enabled");
		
		getOperatingSystem().getEventManager().addEvent(new Event(getOperatingSystem().getSimulationTime() + TIME.SECOND, this));
		
	}

	@Override
	public void onShutdown() {
		// TODO Auto-generated method stub
		
	}

}