package playground.clruch.dispatcher;

import java.util.*;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.util.TravelTime;

import playground.clruch.dispatcher.core.RebalanceEvent;
import playground.clruch.dispatcher.core.UniversalDispatcher;
import playground.clruch.dispatcher.core.VehicleLinkPair;
import playground.clruch.netdata.VirtualNetwork;
import playground.clruch.netdata.VirtualNode;
import playground.clruch.utils.GlobalAssert;
import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.passenger.AVRequest;
import playground.sebhoerl.avtaxi.schedule.AVStayTask;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

public abstract class PartitionedDispatcher extends UniversalDispatcher {
    protected final VirtualNetwork virtualNetwork; //
    private final Set<AVVehicle> rebalancingVehicles = new HashSet<>();

    public PartitionedDispatcher( //
                                  AVDispatcherConfig config, //
                                  TravelTime travelTime, //
                                  ParallelLeastCostPathCalculator router, //
                                  EventsManager eventsManager, //
                                  VirtualNetwork virtualNetwork) {
        super(config, travelTime, router, eventsManager);
        this.virtualNetwork = virtualNetwork;
    }

    // TODO call this function instead of  "setVehicleDiversion"  whenever the cause for rerouting is "rebalance"
    // <- not final code design
    protected synchronized final void setVehicleRebalance(final VehicleLinkPair vehicleLinkPair, final Link destination) {
        // redivert the vehicle, then generate a rebalancing event and add to list of currently rebalancing vehicles
        setVehicleDiversion(vehicleLinkPair, destination);
        eventsManager.processEvent(new RebalanceEvent(destination, vehicleLinkPair.avVehicle, getTimeNow()));
        boolean successAdded = rebalancingVehicles.add(vehicleLinkPair.avVehicle);
        GlobalAssert.that(successAdded);
    }


    protected Map<VirtualNode, List<VehicleLinkPair>> getVirtualNodeAvailableVehicles() {
        Map<VirtualNode, List<VehicleLinkPair>> returnMap =
                getDivertableVehicles().stream() //
                        .parallel() //
                        .collect(Collectors.groupingBy(vlp -> virtualNetwork.getVirtualNode(vlp.getDivertableLocation())));

        for (VirtualNode virtualNode : virtualNetwork.getVirtualNodes()) {
            if (!returnMap.containsKey(virtualNode)) {
                returnMap.put(virtualNode, Collections.emptyList());
            }
        }
        return returnMap;
    }


    private synchronized void updateRebVehicles() {
        // TODO currently the vehicles are removed when arriving at final link, could be removed as soon as they reach rebalancing destination virtualNode instead
        this.getStayVehicles().values().stream().flatMap(q -> q.stream()).forEach(rebalancingVehicles::remove);
    }


    // same as getVirtualNodeAvailableVehicles() but returns only vehicles which are currently not in a rebalancing task
    protected synchronized Map<VirtualNode, List<VehicleLinkPair>> getVirtualNodeAvailableNotRebalancingVehicles() {

        // update the list of rebalancing vehicles
        updateRebVehicles();

        // return list of vehicles
        Map<VirtualNode, List<VehicleLinkPair>> returnMap = getVirtualNodeAvailableVehicles();

        // remove vehicles which are rebalancing
        for(List<VehicleLinkPair> lvlinkpair : returnMap.values()){
            for(VehicleLinkPair vehicleLinkPair : lvlinkpair){
                if(rebalancingVehicles.contains(vehicleLinkPair.avVehicle)){
                    boolean wasRemoved = lvlinkpair.remove(vehicleLinkPair);
                    GlobalAssert.that(wasRemoved);
                }
            }
        }

        return returnMap;
    }


    //


    protected Map<VirtualNode, List<AVRequest>> getVirtualNodeRequests() {
        Map<VirtualNode, List<AVRequest>> returnMap = getAVRequests().stream() //
                .parallel() //
                .collect(Collectors.groupingBy(req -> virtualNetwork.getVirtualNode(req.getFromLink())));

        for (VirtualNode virtualNode : virtualNetwork.getVirtualNodes()) {
            if (!returnMap.containsKey(virtualNode)) {
                returnMap.put(virtualNode, Collections.emptyList());
            }
        }
        return returnMap;
    }

}
