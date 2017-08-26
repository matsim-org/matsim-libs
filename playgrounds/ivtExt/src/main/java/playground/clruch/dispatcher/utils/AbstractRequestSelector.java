// code by clruch and jph
package playground.clruch.dispatcher.utils;

import java.util.Collection;

import playground.clruch.dispatcher.core.RoboTaxi;
import playground.sebhoerl.avtaxi.passenger.AVRequest;

public abstract class AbstractRequestSelector {
    public abstract Collection<AVRequest> selectRequests( //
            Collection<RoboTaxi> vehicleLinkPairs, Collection<AVRequest> avRequests, int size);
}
