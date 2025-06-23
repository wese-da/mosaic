package org.eclipse.mosaic.lib.objects.v2x.etsi;

import javax.annotation.Nonnull;

import org.eclipse.mosaic.lib.geo.GeoPoint;
import org.eclipse.mosaic.lib.objects.v2x.EncodedPayload;
import org.eclipse.mosaic.lib.objects.v2x.MessageRouting;
import org.eclipse.mosaic.lib.objects.v2x.V2xMessage;
import org.eclipse.mosaic.rti.TIME;


/**
 * Manoeuvre coordination message (MCM) as proposed by ETSI TR 103 578.
 */
public class Mcm extends V2xMessage {

	private static final long serialVersionUID = 7289489801013027657L;
	
	public static final long MCM_HORIZON = 30 * TIME.SECOND;

	/**
     * The encoded message.
     */
    private final EncodedPayload payLoad;

    @Override
    @Nonnull
    public EncodedPayload getPayload() {
        return payLoad;
    }
    
	private final McmContent mcmContent;

	public Mcm(MessageRouting routing, final McmContent content, long minimalPayloadLength) {
		super(routing);
		this.mcmContent = content;
		if (EtsiPayloadConfiguration.getPayloadConfiguration().encodePayloads) {
            payLoad = new EncodedPayload(content, minimalPayloadLength);
        } else {
            payLoad = new EncodedPayload(0, minimalPayloadLength);
        }
	}
	
	public McmContent getContent() {
		return mcmContent;
	}
	
	public long getGenerationTime() {
		return mcmContent.getGenerationTime();
	}
	
	public GeoPoint getPosition() {
		return mcmContent.getVehicleManeuverContainer().getCurrentPoint();
	}

}