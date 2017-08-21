package playground.clruch.dispatcher.core;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.network.Link;

import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.passenger.AVRequest;

public enum DispatcherUtils {
    ;
    // ---

    /**
     * @return map containing all staying vehicles and their respective links needed for the matcher
     */
    public static Map<AVVehicle, Link> vehicleMapper(Map<Link, Queue<AVVehicle>> stayVehicles) {
        Map<AVVehicle, Link> stayVehiclesAtLinks = new HashMap<>();
        for (Link link : stayVehicles.keySet()) {
            Queue<AVVehicle> queue = stayVehicles.get(link);
            for (AVVehicle avVehicle : queue) {
                stayVehiclesAtLinks.put(avVehicle, link);
            }
        }
        return stayVehiclesAtLinks;
    }
    
    
    
    /** function call leaves the state of the {@link UniversalDispatcher} unchanged. successive
     * calls to the function return the identical collection.
     * 
     * @return list of {@link AVRequest}s grouped by link */
    public static final Map<Link, List<AVRequest>> getAVRequestsAtLinks(Collection<AVRequest> avRequests) {
        return avRequests.stream() // <- intentionally not parallel to guarantee ordering of requests
                .collect(Collectors.groupingBy(AVRequest::getFromLink));
    }
}
