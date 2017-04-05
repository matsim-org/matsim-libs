package playground.clruch.dispatcher.utils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.function.BiConsumer;

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
    final BiConsumer<AVVehicle, AVRequest> biConsumer;

    public InOrderOfArrivalMatcher(BiConsumer<AVVehicle, AVRequest> biConsumer) {
        this.biConsumer = biConsumer;
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
                    biConsumer.accept(vehicleQueue.poll(), requestIterator.next());
                    ++num_matchedRequests;
                }
            }
        }
        return num_matchedRequests;
    }
    
    public int matchRecord(Map<Link, Queue<AVVehicle>> stayVehicles, Map<Link, List<AVRequest>> requestsAtLinks, HashMap<AVVehicle, List<AVRequest>> requestsServed) {
        int num_matchedRequests = 0;
        // match requests with stay vehicles
        for (Entry<Link, List<AVRequest>> entry : requestsAtLinks.entrySet()) {
            final Link link = entry.getKey();
            if (stayVehicles.containsKey(link)) {
                Iterator<AVRequest> requestIterator = entry.getValue().iterator();
                Queue<AVVehicle> vehicleQueue = stayVehicles.get(link);
                while (!vehicleQueue.isEmpty() && requestIterator.hasNext()) {
                	AVVehicle toMatchVehicle = vehicleQueue.poll();
                	AVRequest toMatchRequest = requestIterator.next();
                	biConsumer.accept(toMatchVehicle, toMatchRequest);
                    requestsServed.get(toMatchVehicle).add(toMatchRequest);
                    ++num_matchedRequests;
                }
            }
        }
        return num_matchedRequests;
    }
    

}
