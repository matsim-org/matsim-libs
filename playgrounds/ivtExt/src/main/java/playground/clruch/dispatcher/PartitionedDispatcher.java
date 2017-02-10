package playground.clruch.dispatcher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.util.TravelTime;

import playground.clruch.netdata.VirtualNetwork;
import playground.clruch.netdata.VirtualNode;
import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.passenger.AVRequest;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

public abstract class PartitionedDispatcher extends UniversalDispatcher {
    protected final VirtualNetwork virtualNetwork; //

    public PartitionedDispatcher( //
            AVDispatcherConfig config, //
            TravelTime travelTime, //
            ParallelLeastCostPathCalculator router, //
            EventsManager eventsManager, //
            VirtualNetwork virtualNetwork) {
        super(config, travelTime, router, eventsManager);
        this.virtualNetwork = virtualNetwork;
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
        Map<VirtualNode, List<VehicleLinkPair>> map = new HashMap<>();
        for (VirtualNode virtualNode : virtualNetwork.getVirtualNodes())
            map.put(virtualNode, new ArrayList<>());
        getDivertableVehicles().stream() //
                .parallel() //
                .forEach(vehicleLinkPair -> {
                    Link link = vehicleLinkPair.getDivertableLocation();
                    VirtualNode virtualNode = virtualNetwork.getVirtualNode(link);
                    map.get(virtualNode).add(vehicleLinkPair);
                });
        return map;
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
        Map<VirtualNode, List<AVRequest>> map = new HashMap<>();
        for (VirtualNode virtualNode : virtualNetwork.getVirtualNodes())
            map.put(virtualNode, new ArrayList<>());
        getAVRequests().stream() //
                .parallel() //
                .forEach(avRequest -> {
                    Link link = avRequest.getFromLink();
                    VirtualNode virtualNode = virtualNetwork.getVirtualNode(link);
                    map.get(virtualNode).add(avRequest);
                });
        return map;
    }

}
