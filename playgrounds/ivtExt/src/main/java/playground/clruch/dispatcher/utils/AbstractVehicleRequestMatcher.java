// code by clruch and jph
package playground.clruch.dispatcher.utils;

import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.matsim.api.core.v01.network.Link;

import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.passenger.AVRequest;

public abstract class AbstractVehicleRequestMatcher {
    public abstract int match(Map<Link, Queue<AVVehicle>> stayVehicles, Map<Link, List<AVRequest>> requestsAtLinks);

}
