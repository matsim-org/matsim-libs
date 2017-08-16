/**
 * 
 */
package playground.fseccamo.dispatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;

import org.matsim.contrib.dvrp.util.LinkTimePair;
import org.matsim.core.utils.collections.QuadTree;

import playground.clruch.dispatcher.core.RoboTaxi;
import playground.clruch.utils.GlobalAssert;
import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.passenger.AVRequest;

/** @author Claudio Ruch */
public class MPCAuxiliary {

    /** @param min
     * @param requests */
    /* package */ static int cellMatchingMPCOption1(int min, List<MpcRequest> requests, double[] networkBounds, List<RoboTaxi> cars, MPCDispatcher1 mpcDispatcher, Map<RoboTaxi, AVRequest> pickupAssignments) {

        int totalPickupEffectiveAdd = 0;
        for (int count = 0; count < min; ++count) {

            // take a request 
            final MpcRequest mpcRequest = requests.get(count);

            
            // build tree with robotaxis and get robotaxi closest to request
            final QuadTree<RoboTaxi> unassignedVehiclesTree = new QuadTree<>(networkBounds[0], networkBounds[1], networkBounds[2], networkBounds[3]);
            for (RoboTaxi robotaxi : cars) {
                unassignedVehiclesTree.put(robotaxi.getDivertableLocation().getCoord().getX(), robotaxi.getDivertableLocation().getCoord().getY(), robotaxi);
            }
            RoboTaxi robotaxi = unassignedVehiclesTree.getClosest( //
                    mpcRequest.avRequest.getFromLink().getCoord().getX(), //
                    mpcRequest.avRequest.getFromLink().getCoord().getY());
            
            
            GlobalAssert.that(!mpcDispatcher.getRoboTaxiInMatching().contains(robotaxi));
            {
                boolean removed = cars.remove(robotaxi);
                GlobalAssert.that(removed);
            }

            
            // dispatch the car and bookkeeping
            pickupAssignments.put(robotaxi, mpcRequest.avRequest);
            mpcDispatcher.getMatchings().put(mpcRequest.avRequest, robotaxi);
            ++totalPickupEffectiveAdd;
        }

        return totalPickupEffectiveAdd;
        
    }
    
    
    
    
    /*package */ static int cellMatchingMPCOption2(int min, List<MpcRequest> requests, List<RoboTaxi> cars, MPCDispatcher1 mpcDispatcher,Map<RoboTaxi, AVRequest> pickupAssignments){
        int totalPickupEffectiveAdd = 0;
        
        List<AVRequest> requestsAtNode = new ArrayList<>();
        for(MpcRequest mpcReq : requests){
            requestsAtNode.add(mpcReq.avRequest);
        }

        // feed to matcher
        Map<RoboTaxi, AVRequest> matching = mpcDispatcher.vehicleDestMatcher.matchAVRequest(cars, requestsAtNode);
        
        
        // execute the computed matching
        for (Entry<RoboTaxi, AVRequest> entry : matching.entrySet()) {
            if(totalPickupEffectiveAdd < min){
                pickupAssignments.put(entry.getKey(), entry.getValue());
                mpcDispatcher.getMatchings().put(entry.getValue(), entry.getKey());
                ++totalPickupEffectiveAdd;                
            }
        }        
        return totalPickupEffectiveAdd;
    }
    
    
    
}
