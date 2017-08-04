// code by clruch and jph
package playground.clruch.dispatcher.core;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.util.TravelTime;

import playground.clruch.utils.GlobalAssert;
import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.passenger.AVRequest;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

/**
 * rebalancing capability (without virtual network)
 */
public abstract class RebalancingDispatcher extends UniversalDispatcher {

    private final Map<AVVehicle, Link> rebalancingVehicles = new HashMap<>();

    protected RebalancingDispatcher(AVDispatcherConfig avDispatcherConfig, TravelTime travelTime,
            ParallelLeastCostPathCalculator parallelLeastCostPathCalculator, EventsManager eventsManager) {
        super(avDispatcherConfig, travelTime, parallelLeastCostPathCalculator, eventsManager);
    }

    @Override
    final void updateDatastructures(Collection<AVVehicle> stayVehicles) {
        // mandatory call
        super.updateDatastructures(stayVehicles);
        
        
        // 1) remove rebalancing vehicles that have reached their destination
        // TODO currently the vehicles are removed when arriving at final link,
        // ... could be removed as soon as they reach rebalancing destination virtualNode instead
        stayVehicles.forEach(rebalancingVehicles::remove);
        
    }

    @Override
    protected Map<AVVehicle, Link> getRebalancingVehicles() {
        return Collections.unmodifiableMap(rebalancingVehicles);
    }
    
	protected List<RoboTaxi> getDivertableUnassignedNotRebalancingVehicleLinkPairs() {
		return getDivertableUnassignedVehicleLinkPairs().stream() //
				.filter(v -> !rebalancingVehicles.containsKey(v.avVehicle)) //
				.collect(Collectors.toList());
	}
    
    @Override
    public final void setVehiclePickup(AVVehicle avVehicle, AVRequest avRequest) {
        super.setVehiclePickup(avVehicle, avRequest);
        if(rebalancingVehicles.containsKey(avVehicle))rebalancingVehicles.remove(avVehicle);
    }
    
    

    // This function has to be called only after getVirtualNodeRebalancingVehicles
    protected synchronized final void setVehicleRebalance(final AVVehicle avVehicle, final Link destination) {
        // in case vehicle is picking up, remove from pickup register
        if (pickupRegister.containsValue(avVehicle)){
            AVRequest avRequest = pickupRegister.inverse().get(avVehicle);
            pickupRegister.forcePut(avRequest, null);
            // TODO do not use forcePut(avRequest,null) because it violates bijection. 
        }
        
        // redivert the vehicle, then generate a rebalancing event and add to list of currently rebalancing vehicles
        setVehicleDiversion(avVehicle, destination);
        eventsManager.processEvent(RebalanceVehicleEvent.create(getTimeNow(), avVehicle, destination));
        Link returnVal = rebalancingVehicles.put(avVehicle, destination);
    }
    
    
    @Override
    boolean extraCheck(RoboTaxi vehicleLinkPair) {
		return rebalancingVehicles.containsKey(vehicleLinkPair.avVehicle);
    }

}
