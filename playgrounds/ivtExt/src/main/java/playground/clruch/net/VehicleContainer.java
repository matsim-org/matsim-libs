package playground.clruch.net;

import java.io.Serializable;

import playground.clruch.export.AVStatus;

public class VehicleContainer implements Serializable {

    /**
     * WARNING:
     * 
     * ANY MODIFICATION IN THIS CLASS EXCEPT COMMENTS
     * WILL INVALIDATE PREVIOUS SIMULATION RECORDINGS
     * 
     * DO NOT MODIFY THIS CLASS UNLESS
     * THERE IS A VERY GOOD REASON
     */

    public int vehicleIndex = -1; // for tracking of individual vehicles
    public int linkIndex = -1;
    public AVStatus avStatus = null;
    public int destinationLinkIndex = -1; // <- value -1 in case no particular destination

    public int getLinkId() {
        return linkIndex;
    }
}
