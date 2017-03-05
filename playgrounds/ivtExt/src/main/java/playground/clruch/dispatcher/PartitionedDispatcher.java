package playground.clruch.dispatcher;

import java.util.*;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.network.Link;
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
    private Set<AVVehicle> rebalancingVehicles;

    public PartitionedDispatcher( //
                                  AVDispatcherConfig config, //
                                  TravelTime travelTime, //
                                  ParallelLeastCostPathCalculator router, //
                                  EventsManager eventsManager, //
                                  VirtualNetwork virtualNetwork) {
        super(config, travelTime, router, eventsManager);
        this.virtualNetwork = virtualNetwork;
        rebalancingVehicles = new HashSet<>();
    }

    // TODO call this function instead of  "setVehicleDiversion"  whenever the cause for rerouting is "rebalance"
    // <- not final code design
    protected final void setVehicleRebalance(final VehicleLinkPair vehicleLinkPair, final Link destination) {
        // redivert the vehicle, then generate a rebalancing event and add to list of currently rebalancing vehicles
        setVehicleDiversion(vehicleLinkPair, destination);
        eventsManager.processEvent(new RebalanceEvent(destination, vehicleLinkPair.avVehicle, getTimeNow()));
        rebalancingVehicles.add(vehicleLinkPair.avVehicle);
    }

    @Deprecated
    protected Map<VirtualNode, Long> getAvailableVehicleCount() { // better to use getAvailableVehicles()
        return getDivertableVehicles().stream() //
                .parallel() //
                .map(VehicleLinkPair::getDivertableLocation) //
                .map(virtualNetwork::getVirtualNode) //
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()));
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

    // same as getVirtualNodeAvailableVehicles() but returns only vehicles which are currently not in a rebalancing task
    protected Map<VirtualNode, List<VehicleLinkPair>> getVirtualNodeAvailableNotRebalancingVehicles() {

        // return list of vehicles
        Map<VirtualNode, List<VehicleLinkPair>> returnMap =
                getDivertableVehicles().stream() //
                        .parallel() //
                        .collect(Collectors.groupingBy(vlp -> virtualNetwork.getVirtualNode(vlp.getDivertableLocation())));

        for (VirtualNode virtualNode : virtualNetwork.getVirtualNodes()) {
            if (!returnMap.containsKey(virtualNode)) {
                returnMap.put(virtualNode, Collections.emptyList());
            }
        }


        for (AVVehicle avVehicle : rebalancingVehicles) {
            VehicleLinkPair vehicleLinkPair = returnMap.values().stream()
                    .flatMap(v -> v.stream())
                    .filter(v -> v.avVehicle.equals(avVehicle))
                    .findFirst().get();
            Schedule schedule = avVehicle.getSchedule();
            Task LastStayTask = schedule.getTasks().get(schedule.getTaskCount() - 1);
            AVStayTask avStayTask = (AVStayTask) LastStayTask;
            // if vehicle has arrived in its destination node, remove from rebalancingVehicles
            if (virtualNetwork.getVirtualNode(vehicleLinkPair.getDivertableLocation()).equals(virtualNetwork.getVirtualNode(avStayTask.getLink()))) {
                rebalancingVehicles.remove(avVehicle);
            } else { // otherwise remove from availablevehicles
                boolean result = returnMap.get(virtualNetwork.getVirtualNode(vehicleLinkPair.getDivertableLocation())).remove(vehicleLinkPair);
                GlobalAssert.that(result);
            }
        }

        return returnMap;
    }



    @Deprecated
    protected Map<VirtualNode, Long> getRequestCount() {
        return getAVRequests().stream() //
                .parallel() //
                .map(AVRequest::getFromLink) //
                .map(virtualNetwork::getVirtualNode) //
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()));
    }

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
