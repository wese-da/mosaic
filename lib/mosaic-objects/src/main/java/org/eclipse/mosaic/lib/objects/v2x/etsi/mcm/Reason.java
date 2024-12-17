package org.eclipse.mosaic.lib.objects.v2x.etsi.mcm;

/**
 * Analog to ETSI TR 103 578<br>
 * 
 * 0: none,<br>
 * 1: lane opening,<br>
 * 2: lane closing,<br>
 * 3: lane change
 */
public enum Reason {
	NONE(0),
	LANE_OPENING(1),
	LANE_CLOSING(2),
	LANE_CHANGE(3);

	public final int id;
	
	Reason(int id) {
		this.id = id;
	}
	
	public static Reason fromId(int id) {
		for (Reason type : Reason.values()) {
			if (type.id == id) {
				return type;
			}
		}
		throw new IllegalArgumentException("Unknown Reason with id " + id);
	}
	
}