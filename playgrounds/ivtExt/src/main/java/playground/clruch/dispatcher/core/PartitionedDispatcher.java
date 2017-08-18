// code by clruch and jph
package playground.clruch.dispatcher.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.util.TravelTime;

import playground.clruch.netdata.VirtualNetwork;
import playground.clruch.netdata.VirtualNode;
import playground.clruch.utils.GlobalAssert;
import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.passenger.AVRequest;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

/** a {@link PartitionedDispatcher} has a {@link VirtualNetwork} */
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
        GlobalAssert.that(virtualNetwork != null);
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

    protected synchronized Map<VirtualNode, List<RoboTaxi>> getVirtualNodeDivertablenotRebalancingRoboTaxis() {
        // return list of vehicles
        Map<VirtualNode, List<RoboTaxi>> returnMap = virtualNetwork.createVNodeTypeMap();
        for (RoboTaxi robotaxi : getDivertableNotRebalancingRoboTaxis()) {
            Link link = robotaxi.getDivertableLocation();
            returnMap.get(virtualNetwork.getVirtualNode(link)).add(robotaxi);
        }
        return returnMap;
    }

    protected Map<VirtualNode, List<RoboTaxi>> getVirtualNodeRebalancingToRoboTaxis() {
        Map<VirtualNode, List<RoboTaxi>> returnMap = virtualNetwork.createVNodeTypeMap();
        for (RoboTaxi robotaxi : getRebalancingRoboTaxis()) {
            VirtualNode toNode = virtualNetwork.getVirtualNode(robotaxi.getCurrentDriveDestination());
            returnMap.get(toNode).add(robotaxi);
        }
        return returnMap;

    }

    protected Map<VirtualNode, List<RoboTaxi>> getVirtualNodeArrivingWithCustomerRoboTaxis() {
        Map<VirtualNode, List<RoboTaxi>> returnMap = virtualNetwork.createVNodeTypeMap();
        for (RoboTaxi robotaxi : getRoboTaxiSubset(AVStatus.DRIVEWITHCUSTOMER)) {
            VirtualNode toNode = virtualNetwork.getVirtualNode(robotaxi.getCurrentDriveDestination());
            returnMap.get(toNode).add(robotaxi);
        }
        return returnMap;
    }

}
