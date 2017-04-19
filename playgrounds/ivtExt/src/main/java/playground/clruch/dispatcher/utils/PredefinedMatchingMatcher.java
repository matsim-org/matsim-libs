package playground.clruch.dispatcher.utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.collections.QuadTree;

import playground.clruch.utils.GlobalAssert;
import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.passenger.AVRequest;

import playground.clruch.dispatcher.core.UniversalDispatcher;

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
    
    
    
    public int matchPredefined(HashMap<AVVehicle, Link> stayVehicles, HashMap<AVRequest, AVVehicle> matchings) {
            int num_matchedRequests = 0;
            // match vehicles which have arrived at origin of request
            for(Entry<AVRequest,AVVehicle> entry : matchings.entrySet()){
                if(stayVehicles.containsKey(entry.getValue()) && stayVehicles.get(entry.getValue()) == entry.getKey().getFromLink()){
                    biConsumer.accept(entry.getValue(), entry.getKey());
                    ++num_matchedRequests;
                };
            }
            
            return num_matchedRequests;
        
        
    }
        


}
