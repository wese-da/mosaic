package org.eclipse.mosaic.lib.objects.v2x.etsi.cpm;


public enum SenderType {
	
	VEHICLE(0),
	RSU(1);
	
	public final int id;
	
	SenderType(int id) {
		this.id = id;
	}

    /**
     * Returns the enum mapped from an integer.
     *
     * @param id identifying integer
     * @return the enum mapped from an integer.
     */
    public static SenderType fromId(int id) {
        for (SenderType type : SenderType.values()) {
            if (type.id == id) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown SenderType id " + id);
    }

}
