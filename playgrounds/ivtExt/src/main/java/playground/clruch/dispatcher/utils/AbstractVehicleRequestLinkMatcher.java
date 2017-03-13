package playground.clruch.dispatcher.utils;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.network.Link;

import playground.clruch.dispatcher.core.VehicleLinkPair;
import playground.sebhoerl.avtaxi.passenger.AVRequest;

public abstract class AbstractVehicleRequestLinkMatcher {
    public abstract Map<VehicleLinkPair, Link> match( //
            Map<Link, List<AVRequest>> requests, Collection<VehicleLinkPair> vehicles);
}
