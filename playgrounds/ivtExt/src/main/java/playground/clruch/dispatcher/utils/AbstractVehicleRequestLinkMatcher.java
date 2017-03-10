package playground.clruch.dispatcher.utils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.network.Link;

import playground.clruch.dispatcher.core.VehicleLinkPair;
import playground.sebhoerl.avtaxi.passenger.AVRequest;

public abstract class AbstractVehicleRequestLinkMatcher {
    public final Collection<VehicleLinkPair> match(
            Map<Link,List<AVRequest>> requests, Collection<VehicleLinkPair> vehicles) {
        if (vehicles.isEmpty())
            return Collections.emptyList();
        return protected_match(requests, vehicles);
    }

    protected abstract Collection<VehicleLinkPair> protected_match(
            Map<Link,List<AVRequest>> requests, //
            Collection<VehicleLinkPair> vehicles //
    );

}
