package playground.clruch.dispatcher;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import playground.sebhoerl.avtaxi.data.AVVehicle;

public class VehicleLinkPair {
    public final AVVehicle avVehicle;
    public final Id<Link> idLink;
    public final double time;

    public VehicleLinkPair(AVVehicle avVehicle, Id<Link> idLink, double time) {
        this.avVehicle = avVehicle;
        this.idLink = idLink;
        this.time = time;
    }

}
