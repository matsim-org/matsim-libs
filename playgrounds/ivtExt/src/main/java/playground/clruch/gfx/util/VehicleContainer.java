package playground.clruch.gfx.util;

import java.io.Serializable;

import playground.clruch.export.AVStatus;

public class VehicleContainer implements Serializable {
//    public String vehicleId; // <- probably obsolete
    public int linkId;
    public AVStatus avStatus;

    public int getLinkId() {
        return linkId;
    }
}
