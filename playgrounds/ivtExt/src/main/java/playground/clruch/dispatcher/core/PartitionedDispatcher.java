package playground.clruch.dispatcher.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.util.TravelTime;

import playground.clruch.netdata.VirtualNetwork;
import playground.clruch.netdata.VirtualNode;
import playground.clruch.utils.GlobalAssert;
import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.passenger.AVRequest;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

/**
 * a {@link PartitionedDispatcher} has a {@link VirtualNetwork}
 */
public abstract class PartitionedDispatcher extends RebalancingDispatcher {
    protected final VirtualNetwork virtualNetwork; //

    public PartitionedDispatcher( //
            AVDispatcherConfig config, //
            TravelTime travelTime, //
            ParallelLeastCostPathCalculator router, //
            EventsManager eventsManager, //
            VirtualNetwork virtualNetwork) {
        super(config, travelTime, router, eventsManager);
        this.virtualNetwork = virtualNetwork;
        GlobalAssert.that(virtualNetwork!=null);
    }

    /**
     * @return returns the divertable vehicles per virtualNode
     */
    protected Map<VirtualNode, List<VehicleLinkPair>> getVirtualNodeAvailableVehicles() {
        Map<VirtualNode, List<VehicleLinkPair>> returnMap = getDivertableVehicles().stream() //
                .parallel() //
                .collect(Collectors.groupingBy(vlp -> virtualNetwork.getVirtualNode(vlp.getDivertableLocation())));

        for (VirtualNode virtualNode : virtualNetwork.getVirtualNodes()) 
            if (!returnMap.containsKey(virtualNode)) 
                returnMap.put(virtualNode, Collections.emptyList());
        
        GlobalAssert.that(returnMap.size() == virtualNetwork.getvNodesCount());
        return returnMap;
    }

    /**
     * @return <VirtualNode, Set<AVVehicle>> with the rebalancing vehicles AVVehicle rebalancing to every node VirtualNode
     */
    protected synchronized Map<VirtualNode, Set<AVVehicle>> getVirtualNodeRebalancingToVehicles() {
        // create set
        Map<VirtualNode, Set<AVVehicle>> returnMap = new HashMap<>();
        for (VirtualNode virtualNode : virtualNetwork.getVirtualNodes()) {
            returnMap.put(virtualNode, new HashSet<>());
        }
        final Map<AVVehicle, Link> rebalancingVehicles = getRebalancingVehicles();
        for (AVVehicle avVehicle : rebalancingVehicles.keySet()) {
            boolean successAdd = returnMap.get(virtualNetwork.getVirtualNode(rebalancingVehicles.get(avVehicle))).add(avVehicle);
            GlobalAssert.that(successAdd);
        }

        // return set
        return Collections.unmodifiableMap(returnMap);
    }

    /**
     * @return
     */
    protected synchronized Map<VirtualNode, Set<AVVehicle>> getVirtualNodeArrivingWCustomerVehicles() {
        final Map<AVVehicle, Link> customMap = getVehiclesWithCustomer();
        final HashMap<VirtualNode, Set<AVVehicle>> customVehiclesMap = new HashMap<>();
        for (VirtualNode virtualNode : virtualNetwork.getVirtualNodes())
            customVehiclesMap.put(virtualNode, new HashSet<>());
        customMap.entrySet().stream() //
                .forEach(e -> customVehiclesMap.get(virtualNetwork.getVirtualNode(e.getValue())).add(e.getKey()));
        return customVehiclesMap;
    }

    /**
     * @return map
     */
    // possibly not needed but still keep around...
    // protected Map<VirtualNode, List<AVVehicle>> getVirtualNodeVehiclesWithCustomer() {
    // Map<AVVehicle, Link> map = getVehiclesWithCustomer();
    // return map.keySet().stream() //
    // .collect(Collectors.groupingBy(vehicle -> virtualNetwork.getVirtualNode(map.get(vehicle))));
    // }

    /**
     * same as getVirtualNodeAvailableVehicles() but returns only vehicles which are currently not in a rebalancing task
     *
     * @return
     */
    protected synchronized NavigableMap<VirtualNode, List<VehicleLinkPair>> getVirtualNodeDivertableNotRebalancingVehicles() {
        // return list of vehicles
        NavigableMap<VirtualNode, List<VehicleLinkPair>> nonRebalanceMap = new TreeMap<>();

        // remove vehicles which are rebalancing
        final Map<AVVehicle, Link> rebalancingVehicles = getRebalancingVehicles();
        Map<VirtualNode, List<VehicleLinkPair>> returnMap = getVirtualNodeAvailableVehicles();
        for (Map.Entry<VirtualNode, List<VehicleLinkPair>> entry : returnMap.entrySet()) {
            nonRebalanceMap.put(entry.getKey(), entry.getValue().stream() //
                    .filter(v -> !rebalancingVehicles.containsKey(v.avVehicle)) //
                    .collect(Collectors.toList()));
        }

        return nonRebalanceMap;
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

    /**
     * @return return virtualNode related HashMaps
     */
    // TODO move this to VirtualNetwork class
    protected <Type> Map<VirtualNode, List<Type>> createvNodeLinksMap() {
        Map<VirtualNode, List<Type>> returnMap = new HashMap<>();
        for (VirtualNode virtualNode : virtualNetwork.getVirtualNodes())
            returnMap.put(virtualNode, new ArrayList<>());
        return returnMap;
    }

}
