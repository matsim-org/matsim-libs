package playground.clruch.dispatcher.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

import org.matsim.api.core.v01.network.Link;

import playground.sebhoerl.avtaxi.data.AVVehicle;

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
}
