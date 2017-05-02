package playground.clruch.dispatcher.core;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.util.TravelTime;

import playground.clruch.utils.GlobalAssert;
import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

public abstract class RebalancingDispatcher extends UniversalDispatcher {
    
    private final Map<AVVehicle, Link> rebalancingVehicles = new HashMap<>();
    protected boolean nonStrict = false;
    
    protected RebalancingDispatcher(AVDispatcherConfig avDispatcherConfig, TravelTime travelTime,
            ParallelLeastCostPathCalculator parallelLeastCostPathCalculator, EventsManager eventsManager) {
        super(avDispatcherConfig, travelTime, parallelLeastCostPathCalculator, eventsManager);
        // TODO Auto-generated constructor stub
    }
    
    @Override
    final void updateDatastructures(Collection<AVVehicle> stayVehicles) {
        super.updateDatastructures(stayVehicles); // mandatory call
        // TODO currently the vehicles are removed when arriving at final link,
        // ... could be removed as soon as they reach rebalancing destination virtualNode instead
        stayVehicles.forEach(rebalancingVehicles::remove);
    }

    @Override
    protected Map<AVVehicle, Link> getRebalancingVehicles() {
        return Collections.unmodifiableMap(rebalancingVehicles);
    }

    // This function has to be called only after getVirtualNodeRebalancingVehicles
    protected synchronized final void setVehicleRebalance(final VehicleLinkPair vehicleLinkPair, final Link destination) {
        // redivert the vehicle, then generate a rebalancing event and add to list of currently rebalancing vehicles
        setVehicleDiversion(vehicleLinkPair, destination);
        eventsManager.processEvent(RebalanceVehicleEvent.create(getTimeNow(), vehicleLinkPair.avVehicle, destination));
        Link returnVal = rebalancingVehicles.put(vehicleLinkPair.avVehicle, destination);
        GlobalAssert.that(nonStrict || returnVal == null);
    }
}
