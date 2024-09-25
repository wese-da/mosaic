package org.eclipse.mosaic.lib.objects.v2x.etsi.cpm;

import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;

import org.eclipse.mosaic.lib.enums.SensorType;
import org.eclipse.mosaic.lib.objects.ToDataOutput;

public class SensorInformation implements ToDataOutput, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -823322070356042026L;
	
	private SensorType type;

	@Override
	public void toDataOutput(DataOutput dataOutput) throws IOException {
		// TODO Auto-generated method stub
		
	}

}
