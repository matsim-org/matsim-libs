package playground.clruch.dispatcher.utils;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.function.BiFunction;

import org.matsim.api.core.v01.network.Link;

import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.passenger.AVRequest;

/**
 * if a vehicle is in stay more and there is a request at the link of where the car is
 * the vehicle will pickup the customer.
 * this is repeated for all possible matches until either there are no more available cars at a link,
 * or no more requests.
 * customers who have waited longer are picked up first.
 */
public class InOrderOfArrivalMatcher extends AbstractVehicleRequestMatcher {
    final BiFunction<AVVehicle, AVRequest, Void> biFunction;

    public InOrderOfArrivalMatcher(BiFunction<AVVehicle, AVRequest, Void> biFunction) {
        this.biFunction = biFunction;
    }

    @Override
    public int match(Map<Link, Queue<AVVehicle>> stayVehicles, Map<Link, List<AVRequest>> requestsAtLinks) {
        int num_matchedRequests = 0;
        // match requests with stay vehicles
        for (Entry<Link, List<AVRequest>> entry : requestsAtLinks.entrySet()) {
            final Link link = entry.getKey();
            if (stayVehicles.containsKey(link)) {
                Iterator<AVRequest> requestIterator = entry.getValue().iterator();
                Queue<AVVehicle> vehicleQueue = stayVehicles.get(link);
                while (!vehicleQueue.isEmpty() && requestIterator.hasNext()) {
                    biFunction.apply(vehicleQueue.poll(), requestIterator.next());
                    ++num_matchedRequests;
                }
            }
        }
        return num_matchedRequests;
    }

}
