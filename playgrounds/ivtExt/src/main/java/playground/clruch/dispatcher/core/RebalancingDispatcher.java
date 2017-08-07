// code by clruch and jph
package playground.clruch.dispatcher.core;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.util.TravelTime;

import playground.clruch.utils.GlobalAssert;
import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.passenger.AVRequest;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

/** rebalancing capability (without virtual network) */
public abstract class RebalancingDispatcher extends UniversalDispatcher {

    private final Map<RoboTaxi, Link> rebalancingVehicles = new HashMap<>();

    protected RebalancingDispatcher(AVDispatcherConfig avDispatcherConfig, TravelTime travelTime,
            ParallelLeastCostPathCalculator parallelLeastCostPathCalculator, EventsManager eventsManager) {
        super(avDispatcherConfig, travelTime, parallelLeastCostPathCalculator, eventsManager);
    }

    
    // TODO get rid of the AVVehicle here
    @Override
    final void updateDatastructures(Collection<AVVehicle> stayVehicles) {
        // mandatory call
        super.updateDatastructures(stayVehicles);

        // remove rebalancing vehicles that have reached their destination
        for(AVVehicle avVehicle : stayVehicles){
            Optional<RoboTaxi> optRob = getRoboTaxis().stream().filter(v->v.getAVVehicle().equals(avVehicle)).findAny();
            GlobalAssert.that(optRob.isPresent());
            rebalancingVehicles.remove(optRob.get());
        }
    }

    @Override
    protected Map<RoboTaxi, Link> getRebalancingVehicles() {
        return Collections.unmodifiableMap(rebalancingVehicles);
    }

    protected List<RoboTaxi> getDivertableUnassignedNotRebalancingVehicleLinkPairs() {
        return getDivertableUnassignedVehicleLinkPairs().stream() //
                .filter(v -> !rebalancingVehicles.containsKey(v.getAVVehicle())) //
                .collect(Collectors.toList());
    }

    protected synchronized final void setRoboTaxiRebalance(final RoboTaxi roboTaxi, final Link destination) {
        System.out.println("setRoboTaxiRebalance " +  roboTaxi.getAVVehicle().getId() + "  to  " +  destination.getId().toString());
        
        // in case vehicle is picking up, remove from pickup register
        if (pickupRegister.containsValue(roboTaxi)) {
            System.out.println("vehicle was previously picking up");
            pickupRegister.remove(pickupRegister.inverse().get(roboTaxi), roboTaxi);
        }

        // redivert roboTaxi, generate rebalancing event, add to rebalancing list
        setRoboTaxiDiversion(roboTaxi, destination);
        eventsManager.processEvent(RebalanceVehicleEvent.create(getTimeNow(), roboTaxi.getAVVehicle(), destination));
        Link returnVal = rebalancingVehicles.put(roboTaxi, destination);
        printRebalancingVehicles();
        

    }

    @Override
    boolean extraCheck(RoboTaxi roboTaxi) {
        return rebalancingVehicles.containsKey(roboTaxi);
    }
    
    
    
    
    private void printRebalancingVehicles(){
        System.out.println("rebalancingVehicles: ==============================");
        for(RoboTaxi roboTaxi : rebalancingVehicles.keySet()){
            System.out.println(roboTaxi.getAVVehicle().getId() + "    to    " + rebalancingVehicles.get(roboTaxi).getId().toString());
        }
    }

}

// @Override
// public final void setVehiclePickup(AVVehicle avVehicle, AVRequest avRequest) {
// super.setVehiclePickup(avVehicle, avRequest);
// if (rebalancingVehicles.containsKey(avVehicle))
// rebalancingVehicles.remove(avVehicle);
// }

//// This function has to be called only after getVirtualNodeRebalancingVehicles
// protected synchronized final void setVehicleRebalance(final AVVehicle avVehicle, final Link
//// destination) {
// // in case vehicle is picking up, remove from pickup register
// if (pickupRegister.containsValue(avVehicle)) {
// AVRequest avRequest = pickupRegister.inverse().get(avVehicle);
// pickupRegister.forcePut(avRequest, null);
// // TODO do not use forcePut(avRequest,null) because it violates bijection.
// }
//
// // redivert the vehicle, then generate a rebalancing event and add to list of currently
// // rebalancing vehicles
// setVehicleDiversion(avVehicle, destination);
// eventsManager.processEvent(RebalanceVehicleEvent.create(getTimeNow(), avVehicle, destination));
// Link returnVal = rebalancingVehicles.put(RoboTaxi, destination);
// }
