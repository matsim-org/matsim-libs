package playground.clruch.dispatcher.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.function.BiConsumer;

import org.matsim.api.core.v01.network.Link;

import playground.clruch.utils.GlobalAssert;
import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.passenger.AVRequest;

/**
 * if a vehicle is in stay more and there is a request at the link of where the car is the vehicle
 * will pickup the customer. this is repeated for all possible matches until either there are no
 * more available cars at a link, or no more requests. customers who have waited longer are picked
 * up first.
 */
public class PredefinedMatchingMatcher extends AbstractVehicleRequestMatcher {
    final BiConsumer<AVVehicle, AVRequest> biConsumer;

    public PredefinedMatchingMatcher(BiConsumer<AVVehicle, AVRequest> biConsumer) {
        this.biConsumer = biConsumer;
    }

    @Override
    public int match(Map<Link, Queue<AVVehicle>> stayVehicles, Map<Link, List<AVRequest>> requestsAtLinks) {
        // this should not be called
        GlobalAssert.that(false);
        return 0;
    }

    public int matchPredefined(Map<AVVehicle, Link> stayVehicles, Map<AVRequest, AVVehicle> matchings) {
        int num_matchedRequests = 0;
        // match vehicles which have arrived at origin of request
        for (Entry<AVRequest, AVVehicle> entry : new HashMap<>(matchings).entrySet()) {
            final AVRequest avRequest = entry.getKey();
            final AVVehicle avVehicle = entry.getValue();
            if (stayVehicles.containsKey(avVehicle)) {
                Link link = stayVehicles.get(avVehicle);
                if (link.equals(avRequest.getFromLink())) {
                    biConsumer.accept(avVehicle, avRequest);
                    ++num_matchedRequests;
                }
            }
        }
        return num_matchedRequests;
    }
}
