package playground.clruch.net;

import java.io.Serializable;

import playground.clruch.export.AVStatus;

public class VehicleContainer implements Serializable {
    public static final int LINK_UNSPECIFIED = -1;
    // ---
    public String vehicleId; // for tracking of individual vehicles
    public int linkId;
    public AVStatus avStatus;
    public int destinationLinkId = LINK_UNSPECIFIED; // <- value -1 in case no particular destination

    public int getLinkId() {
        return linkId;
    }
}
