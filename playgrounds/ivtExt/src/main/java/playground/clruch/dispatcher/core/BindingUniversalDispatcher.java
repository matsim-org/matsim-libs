// code by clruch and spencer
package playground.clruch.dispatcher.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.util.TravelTime;

import playground.clruch.utils.GlobalAssert;
import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.passenger.AVRequest;
import playground.sebhoerl.avtaxi.schedule.AVStayTask;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

public abstract class BindingUniversalDispatcher extends UniversalDispatcher {

    protected BindingUniversalDispatcher( //
            AVDispatcherConfig avDispatcherConfig, //
            TravelTime travelTime, //
            ParallelLeastCostPathCalculator parallelLeastCostPathCalculator, //
            EventsManager eventsManager //
    ) {
        super(avDispatcherConfig, travelTime, parallelLeastCostPathCalculator, eventsManager);
    }

    /**
     * @return AVRequests which are currently not assigned to a vehicle
     */
    protected synchronized final List<AVRequest> getUnassignedAVRequests() {
        List<AVRequest> unassignedRequests = new ArrayList<>();
        for (AVRequest avRequest : pendingRequests) {
            if (pickupRegister.get(avRequest) == null) {
                unassignedRequests.add(avRequest);
            }
        }
        return unassignedRequests;
    }


    /**
     * 
     * @return available vehicles which are yet unassigned to a request as vehicle Link pairs
     */
    protected List<VehicleLinkPair> getDivertableUnassignedVehicleLinkPairs() {
        // get the staying vehicles and requests
        List<VehicleLinkPair> divertableUnassignedVehiclesLinkPairs = new ArrayList<>();
        for (VehicleLinkPair vehicleLinkPair : getDivertableVehicleLinkPairs()) {
            if (!pickupRegister.inverse().containsKey(vehicleLinkPair.avVehicle)) {
                divertableUnassignedVehiclesLinkPairs.add(vehicleLinkPair);
            }
        }
        return divertableUnassignedVehiclesLinkPairs;
    }


}
