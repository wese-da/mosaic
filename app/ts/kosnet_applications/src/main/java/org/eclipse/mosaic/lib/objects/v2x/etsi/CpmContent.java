package org.eclipse.mosaic.lib.objects.v2x.etsi;

import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;
import java.util.Objects;

import javax.annotation.Nonnull;

import org.eclipse.mosaic.lib.objects.ToDataOutput;
import org.eclipse.mosaic.lib.objects.v2x.etsi.cpm.SenderInformation;

public class CpmContent implements ToDataOutput, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 9177024789065166904L;
	
    private final long generationTime;
    
    private final SenderInformation senderInfo;
    // private final SensorInformation sensorInfo;
    // private final PerceptionRegionData percRegInfo;
    // private final PerceivedObjectsData percObj;
    
    public CpmContent(long generationTime,
    		@Nonnull SenderInformation senderInfo) {
    	this.generationTime = generationTime;
    	this.senderInfo = Objects.requireNonNull(senderInfo);
    }

    public SenderInformation getSenderInfo() {
    	return senderInfo;
    }
    
	@Override
	public void toDataOutput(DataOutput dataOutput) throws IOException {
		
	}

}
