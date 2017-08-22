// code by clruch and jph
package playground.clruch.dispatcher.core;

import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.util.TravelTime;

import playground.clruch.netdata.VirtualNetwork;
import playground.clruch.netdata.VirtualNode;
import playground.clruch.utils.GlobalAssert;
import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.passenger.AVRequest;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

/** All dispatchers wich perform rebalancing and use a virtualNetwork dividing the city into zones are derived from {@link PartitionedDispatcher}.
 * A {@link PartitionedDispatcher} always has a {@link VirtualNetwork}
 * 
 * @author Claudio Ruch
 * @param <T> */
public abstract class PartitionedDispatcher<T> extends RebalancingDispatcher {
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

    /** @return {@link java.util.Map} where all {@link AVRequest} are listed at the {@link VirtualNode} where their {@link AVRequest.fromLink} is. */
    protected Map<VirtualNode, List<AVRequest>> getVirtualNodeRequests() {
        Map<VirtualNode, List<AVRequest>> returnMap = virtualNetwork.createVNodeTypeMap();
        getAVRequests().stream()//
                .forEach(avr -> returnMap.get(virtualNetwork.getVirtualNode(avr.getFromLink())).add(avr));
        return returnMap;
    }

    /** @return {@link java.util.Map} where all divertable not rebalancing {@link RoboTaxi} are listed at the {@link VirtualNode} where their {@link Link}
     *         divertableLocation is. */
    protected synchronized Map<VirtualNode, List<RoboTaxi>> getVirtualNodeDivertablenotRebalancingRoboTaxis() {
        Map<VirtualNode, List<RoboTaxi>> returnMap = virtualNetwork.createVNodeTypeMap();
        getDivertableNotRebalancingRoboTaxis().stream()//
                .forEach(rt -> returnMap.get(virtualNetwork.getVirtualNode(rt.getDivertableLocation())).add(rt));
        return returnMap;
    }

    /** @return {@link java.util.Map} where all rebalancing {@link RoboTaxi} are listed at the {@link VirtualNode} where their {@link Link} current
     *         driveDestination is. */
    protected Map<VirtualNode, List<RoboTaxi>> getVirtualNodeRebalancingToRoboTaxis() {
        Map<VirtualNode, List<RoboTaxi>> returnMap = virtualNetwork.createVNodeTypeMap();
        getRebalancingRoboTaxis().stream()//
                .forEach(rt -> returnMap.get(virtualNetwork.getVirtualNode(rt.getCurrentDriveDestination())).add(rt));
        return returnMap;

    }

    /** @return {@link java.util.Map} where all roboTaxis with customer {@link RoboTaxi} are listed at the {@link VirtualNode} where their {@link Link} current
     *         driveDestination is. */
    protected Map<VirtualNode, List<RoboTaxi>> getVirtualNodeArrivingWithCustomerRoboTaxis() {
        Map<VirtualNode, List<RoboTaxi>> returnMap = virtualNetwork.createVNodeTypeMap();
        getRoboTaxiSubset(AVStatus.DRIVEWITHCUSTOMER).stream()//
                .forEach(rt -> returnMap.get(virtualNetwork.getVirtualNode(rt.getCurrentDriveDestination())).add(rt));
        return returnMap;
    }

    


}
