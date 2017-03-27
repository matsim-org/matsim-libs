package playground.clruch.net;

import java.io.Serializable;

import playground.clruch.export.AVStatus;

public class VehicleContainer implements Serializable {
    public int vehicleIndex = -1; // for tracking of individual vehicles
    public int linkIndex = -1;
    public AVStatus avStatus = null;
    public int destinationLinkIndex = -1; // <- value -1 in case no particular destination

    public int getLinkId() {
        return linkIndex;
    }
}
