package playground.clruch.dispatcher;

import java.util.*;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.util.TravelTime;

import playground.clruch.dispatcher.core.RebalanceVehicleEvent;
import playground.clruch.dispatcher.core.UniversalDispatcher;
import playground.clruch.dispatcher.core.VehicleLinkPair;
import playground.clruch.netdata.VirtualNetwork;
import playground.clruch.netdata.VirtualNode;
import playground.clruch.utils.GlobalAssert;
import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.passenger.AVRequest;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

public abstract class PartitionedDispatcher extends UniversalDispatcher {
    protected final VirtualNetwork virtualNetwork; //
    private final Map<AVVehicle, Link> rebalancingVehicles = new HashMap<>();

    public PartitionedDispatcher( //
                                  AVDispatcherConfig config, //
                                  TravelTime travelTime, //
                                  ParallelLeastCostPathCalculator router, //
                                  EventsManager eventsManager, //
                                  VirtualNetwork virtualNetwork) {
        super(config, travelTime, router, eventsManager);
        this.virtualNetwork = virtualNetwork;
    }

    // This function has to be called only after getVirtualNodeRebalancingVehicles
    protected synchronized final void setVehicleRebalance(final VehicleLinkPair vehicleLinkPair, final Link destination) {
        updateRebVehicles();
        // redivert the vehicle, then generate a rebalancing event and add to list of currently rebalancing vehicles
        setVehicleDiversion(vehicleLinkPair, destination);
        eventsManager.processEvent(RebalanceVehicleEvent.create(getTimeNow(), vehicleLinkPair.avVehicle, destination));
        Link returnVal = rebalancingVehicles.put(vehicleLinkPair.avVehicle, destination);
        GlobalAssert.that(returnVal == null);
    }


    /**
     * @return returns the divertable vehicles per virtualNode
     */
    protected Map<VirtualNode, List<VehicleLinkPair>> getVirtualNodeAvailableVehicles() {
        Map<VirtualNode, List<VehicleLinkPair>> returnMap = getDivertableVehicles().stream() //
                .parallel() //
                .collect(Collectors.groupingBy(vlp -> virtualNetwork.getVirtualNode(vlp.getDivertableLocation())));

        for (VirtualNode virtualNode : virtualNetwork.getVirtualNodes()) {
            if (!returnMap.containsKey(virtualNode)) {
                returnMap.put(virtualNode, Collections.emptyList());
            }
        }
        return returnMap;
    }

    // TODO currently the vehicles are removed when arriving at final link, could be removed as soon as they reach rebalancing destination virtualNode instead
    private synchronized void updateRebVehicles() {
        getStayVehicles().values().stream() //
                .flatMap(q -> q.stream()).forEach(rebalancingVehicles::remove);
    }

    /**
     * @return <VirtualNode, Set<AVVehicle>> with the rebalancing vehicles AVVehicle rebalancing to every node VirtualNode
     */
    protected synchronized Map<VirtualNode, Set<AVVehicle>> getVirtualNodeRebalancingToVehicles() {
        // update the list of rebalancing vehicles
        updateRebVehicles();

        // create set
        Map<VirtualNode, Set<AVVehicle>> returnMap = new HashMap<>();
        for (VirtualNode virtualNode : virtualNetwork.getVirtualNodes()) {
            returnMap.put(virtualNode, new HashSet<>());
        }
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
        Collection<VehicleLinkPair> customVehicles = getVehiclesWithCustomer();
        HashMap<VirtualNode, Set<AVVehicle>> customVehiclesMap = new HashMap<>();

        for (VirtualNode virtualNode : virtualNetwork.getVirtualNodes()) {
            customVehiclesMap.put(virtualNode, new HashSet<>());
        }

        customVehicles.stream().forEach(v -> customVehiclesMap.get(virtualNetwork.getVirtualNode(v.getCurrentDriveDestination())).add(v.avVehicle));
        return customVehiclesMap;
    }

    /**
     * // same as getVirtualNodeAvailableVehicles() but returns only vehicles which are currently not in a rebalancing task
     *
     * @return
     */
    protected synchronized Map<VirtualNode, List<VehicleLinkPair>> getVirtualNodeDivertableNotRebalancingVehicles() {
        // update the list of rebalancing vehicles
        updateRebVehicles();

        // return list of vehicles
        Map<VirtualNode, List<VehicleLinkPair>> returnMap = getVirtualNodeAvailableVehicles();
        Map<VirtualNode, List<VehicleLinkPair>> rebalanceMap = new HashMap<>();

        // remove vehicles which are rebalancing
        for (Map.Entry<VirtualNode, List<VehicleLinkPair>> entry : returnMap.entrySet()) {
            rebalanceMap.put(entry.getKey(), entry.getValue().stream().filter(v -> !rebalancingVehicles.containsKey(v.avVehicle)).collect(Collectors.toList()));
        }

        return rebalanceMap;
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


    /**
     *
     * @return return virtualNode related HashMaps
     */
    protected <Type> Map<VirtualNode, List<Type>> createvNodeLinksMap() {
        Map<VirtualNode, List<Type>> returnMap = new HashMap<>();
        for (VirtualNode virtualNode : virtualNetwork.getVirtualNodes())
            returnMap.put(virtualNode, new ArrayList<>());
        return returnMap;
    }


}
