package playground.clruch.dispatcher.utils;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;

import org.matsim.api.core.v01.network.Link;

import playground.clruch.dispatcher.core.UniversalDispatcher;
import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.passenger.AVRequest;

@Deprecated
public class MatchRequestsWithStayVehicles { // TODO remove

    /**
     * if a vehicle is in stay more and there is a request at the link of where the car is
     * the vehicle will pickup the customer.
     * this is repeated for all possible matches until either there are no more available cars at a link,
     * or no more requests.
     * customers who have waited longer are picked up first.
     * 
     * functionality extracted for convenience and reuse
     * 
     * @param universalDispatcher
     * @return
     */
    // TODO make functions protected once this is removed
    @Deprecated
    public static int inOrderOfArrival(UniversalDispatcher universalDispatcher) {
        int num_matchedRequests = 0;
        // match requests with stay vehicles
        Map<Link, List<AVRequest>> requests = universalDispatcher.getAVRequestsAtLinks();
        Map<Link, Queue<AVVehicle>> stayVehicles = universalDispatcher.getStayVehicles();

        for (Entry<Link, List<AVRequest>> entry : requests.entrySet()) {
            final Link link = entry.getKey();
            if (stayVehicles.containsKey(link)) {
                Iterator<AVRequest> requestIterator = entry.getValue().iterator();
                Queue<AVVehicle> vehicleQueue = stayVehicles.get(link);
                while (!vehicleQueue.isEmpty() && requestIterator.hasNext()) {
                    universalDispatcher.setAcceptRequest(vehicleQueue.poll(), requestIterator.next());
                    ++num_matchedRequests;
                }
            }
        }

        return num_matchedRequests;
    }
}
