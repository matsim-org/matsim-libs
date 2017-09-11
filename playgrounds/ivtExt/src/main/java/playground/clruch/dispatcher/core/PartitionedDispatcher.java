// code by clruch and jph
package playground.clruch.dispatcher.core;

import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.util.TravelTime;

import ch.ethz.idsc.queuey.core.networks.VirtualNetwork;
import ch.ethz.idsc.queuey.core.networks.VirtualNode;
import ch.ethz.idsc.queuey.util.GlobalAssert;
import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.passenger.AVRequest;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

/** All dispatchers wich perform rebalancing and use a virtualNetwork dividing the city into zones are derived from {@link PartitionedDispatcher}.
 * A {@link PartitionedDispatcher} always has a {@link VirtualNetwork}
 * 
 * @author Claudio Ruch
 * @param <T> */
public abstract class PartitionedDispatcher extends RebalancingDispatcher {
    protected final VirtualNetwork<Link> virtualNetwork; //

    protected PartitionedDispatcher( //
            AVDispatcherConfig config, //
            TravelTime travelTime, //
            ParallelLeastCostPathCalculator router, //
            EventsManager eventsManager, //
            VirtualNetwork<Link> virtualNetwork) {
        super(config, travelTime, router, eventsManager);
        this.virtualNetwork = virtualNetwork;
        GlobalAssert.that(virtualNetwork != null);
    }

    /** @return {@link java.util.Map} where all {@link AVRequest} are listed at the {@link VirtualNode} where their {@link AVRequest.fromLink} is. */
    protected Map<VirtualNode<Link>, List<AVRequest>> getVirtualNodeRequests() {
        return virtualNetwork.binToVirtualNode(getAVRequests(), AVRequest::getFromLink);
    }

    /** @return {@link java.util.Map} where all divertable not rebalancing {@link RoboTaxi} are listed at the {@link VirtualNode} where their {@link Link}
     *         divertableLocation is. */
    protected Map<VirtualNode<Link>, List<RoboTaxi>> getVirtualNodeDivertableNotRebalancingRoboTaxis() {
        return virtualNetwork.binToVirtualNode(getDivertableNotRebalancingRoboTaxis(), RoboTaxi::getDivertableLocation);
    }

    /** @return {@link java.util.Map} where all rebalancing {@link RoboTaxi} are listed at the {@link VirtualNode} where their {@link Link} current
     *         driveDestination is. */
    protected Map<VirtualNode<Link>, List<RoboTaxi>> getVirtualNodeRebalancingToRoboTaxis() {
        return virtualNetwork.binToVirtualNode(getRebalancingRoboTaxis(), RoboTaxi::getCurrentDriveDestination);
    }

    /** @return {@link java.util.Map} where all roboTaxis with customer {@link RoboTaxi} are listed at the {@link VirtualNode} where their {@link Link} current
     *         driveDestination is. */
    protected Map<VirtualNode<Link>, List<RoboTaxi>> getVirtualNodeArrivingWithCustomerRoboTaxis() {
        return virtualNetwork.binToVirtualNode(getRoboTaxiSubset(AVStatus.DRIVEWITHCUSTOMER), RoboTaxi::getCurrentDriveDestination);
    }

    /** @return {@link java.util.Map} where all stay roboTaxis with customer {@link RoboTaxi} are listed at the {@link VirtualNode} where their {@link Link} current
     *         divertableLocation is. */
    protected Map<VirtualNode<Link>, List<RoboTaxi>> getVirtualNodeStayVehicles() {
        return virtualNetwork.binToVirtualNode(getRoboTaxiSubset(AVStatus.STAY), RoboTaxi::getDivertableLocation);
    }

}
