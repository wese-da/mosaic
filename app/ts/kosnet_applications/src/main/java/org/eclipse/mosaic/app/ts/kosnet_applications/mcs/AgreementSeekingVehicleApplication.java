package org.eclipse.mosaic.app.ts.kosnet_applications.mcs;

import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.CamBuilder;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedAcknowledgement;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedV2xMessage;
import org.eclipse.mosaic.fed.application.app.api.CommunicationApplication;
import org.eclipse.mosaic.interactions.communication.V2xMessageTransmission;
import org.eclipse.mosaic.lib.objects.v2x.V2xMessage;
import org.eclipse.mosaic.lib.objects.v2x.etsi.Mcm;
import org.eclipse.mosaic.lib.objects.v2x.etsi.McmContent;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.McmCategoryType;
import org.eclipse.mosaic.lib.objects.v2x.etsi.mcm.McmTrajectory;
import org.eclipse.mosaic.lib.util.scheduling.Event;
import org.eclipse.mosaic.rti.TIME;

public class AgreementSeekingVehicleApplication extends ManeuverCoordinationService implements CommunicationApplication {
	
	private McmTrajectory targetTrajectory;

	public AgreementSeekingVehicleApplication(boolean useCells) {
		super(useCells);
	}

	@Override
	public void processEvent(Event event) throws Exception {
		shareStatus();
		shareIntent();
		getOperatingSystem().getEventManager().addEvent(new Event(getOperatingSystem().getSimulationTime() + TIME.SECOND, this));
	}

	@Override
	public void onMessageReceived(ReceivedV2xMessage receivedV2xMessage) {
		
		V2xMessage receivedMessage = receivedV2xMessage.getMessage();
		if (receivedMessage instanceof Mcm) {
		}
		
	}

	@Override
	public void onAcknowledgementReceived(ReceivedAcknowledgement acknowledgement) {
		
	}

	@Override
	public void onCamBuilding(CamBuilder camBuilder) {
		
	}

	@Override
	public void onMessageTransmitted(V2xMessageTransmission v2xMessageTransmission) {
		
	}
	
}
