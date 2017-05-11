package playground.clruch.dispatcher.utils;

import java.util.Collection;

import playground.clruch.dispatcher.core.VehicleLinkPair;
import playground.sebhoerl.avtaxi.passenger.AVRequest;

public abstract class AbstractRequestSelector {
    public abstract Collection<AVRequest> selectRequests( //
            Collection<VehicleLinkPair> vehicleLinkPairs, Collection<AVRequest> avRequests, int size);
}
