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
    /* package */ static int MPC1Opation(int min, List<MpcRequest> requests, double[] networkBounds, List<RoboTaxi> cars, MPCDispatcher1 mpcDispatcher, Map<RoboTaxi, AVRequest> pickupAssignments) {

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
    
    
    
    
    /*package */ static int MPC2Option(List<MpcRequest> requests, List<RoboTaxi> cars, double now, MPCDispatcher1 mpcDispatcher){
        int totalPickupEffectiveAdd = 0;
        
        
        // ==================================
        // Inserting new code here

        // 1) build List<VehicleLinkPair> with stay vehicles and links
        List<RoboTaxi> unassignedVehiclesNode = new ArrayList<>();
        for (AVVehicle avVehicle : cars) {
            // TODO switch to roboTaxi
            //LinkTimePair ltp = new LinkTimePair(getVehicleLocation(avVehicle), now);
            LinkTimePair ltp = new LinkTimePair(null, now);
         // TODO switch to roboTaxi
            //unassignedVehiclesNode.add(new RoboTaxi(avVehicle, ltp, getVehicleLocation(avVehicle)));
           unassignedVehiclesNode.add(new RoboTaxi(avVehicle, ltp, null));
        }

        // 2) build List<link> with requests locations
        
//        List<Link> requestLinksNode = new  ArrayList<>();
//        for (int count = 0; count < min; ++count) {
//            MpcRequest mpcRequest = requests.get(count);
//            requestLinksNode.add(mpcRequest.avRequest.getFromLink());
//        }
        
        List<AVRequest> requestsAtNode = new ArrayList<>();
        for(MpcRequest mpcReq : requests){
            requestsAtNode.add(mpcReq.avRequest);
        }
        
        

        // 3) feed to matcher
        //Map<VehicleLinkPair, Link> matching = vehicleDestMatcher.match(unassignedVehiclesNode, requestLinksNode);

        Map<RoboTaxi, AVRequest> matching = mpcDispatcher.vehicleDestMatcher.matchAVRequest(unassignedVehiclesNode, requestsAtNode);
        
        
        // 4) execute the computed matching
        for (Entry<RoboTaxi, AVRequest> entry : matching.entrySet()) {
//            AVVehicle avVehicle = entry.getKey().getAVVehicle();
            // TODO adapt to new API
            AVVehicle avVehicle = null;
            GlobalAssert.that(!mpcDispatcher.getRoboTaxiInMatching().contains(avVehicle));
//                       Link pickupLocation = entry.getValue();
            
            setVehiclePickup(avVehicle, entry.getValue());
            
            //setVehiclePickup(avVehicle, entry);(avVehicle, pickupLocation); // send car to customer
            // find some mpc request with this link and remove it
            Optional<MpcRequest> optMpcRequst = requests.stream()
                    .filter(v -> v.avRequest.equals(entry.getValue())).findAny();
            GlobalAssert.that(optMpcRequst.isPresent());
            //getMatchings().put(optMpcRequst.get().avRequest, avVehicle);
            
            requests.remove(optMpcRequst.get());
            ++totalPickupEffectiveAdd;
        }

        // new code end
        // ==================================

        
        return totalPickupEffectiveAdd;
    }
    
    
    
}
