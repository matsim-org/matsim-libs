// code by clruch and jph
package playground.clruch.dispatcher.utils;

import java.util.Collection;

import ch.ethz.matsim.av.passenger.AVRequest;
import playground.clruch.dispatcher.core.RoboTaxi;

public abstract class AbstractRequestSelector {
    public abstract Collection<AVRequest> selectRequests( //
            Collection<RoboTaxi> vehicleLinkPairs, Collection<AVRequest> avRequests, int size);
}
