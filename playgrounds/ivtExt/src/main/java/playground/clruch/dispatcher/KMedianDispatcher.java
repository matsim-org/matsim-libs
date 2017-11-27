package playground.clruch.dispatcher;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import ch.ethz.idsc.queuey.core.networks.VirtualNetwork;
import ch.ethz.idsc.queuey.core.networks.VirtualNode;
import ch.ethz.idsc.queuey.util.GlobalAssert;
import playground.clruch.dispatcher.core.AVStatus;
import playground.clruch.dispatcher.core.PartitionedDispatcher;
import playground.clruch.dispatcher.core.RoboTaxi;
import playground.clruch.netdata.VirtualNetworkGet;
import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.config.AVGeneratorConfig;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatcher;
import playground.sebhoerl.avtaxi.framework.AVModule;
import playground.sebhoerl.avtaxi.passenger.AVRequest;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

/** Upon arrival, a demand is assigned to the depot closest to its pick-up location. The RoboTaxi services its demands in FIFO order
 * returning to the depot after each delivery */

public class KMedianDispatcher extends PartitionedDispatcher {
    private VirtualNetwork<Link> virtualNetwork;
    private final Map<VirtualNode<Link>, Link> waitLocations;

    // private final VirtualNetwork<Link> virtualNetwork; //

    protected KMedianDispatcher(//
            Config config, //
            AVDispatcherConfig avconfig, //
            TravelTime travelTime, //
            AVGeneratorConfig generatorConfig, //
            ParallelLeastCostPathCalculator router, //
            EventsManager eventsManager, //
            Network network, //
            VirtualNetwork<Link> virtualNetwork) {
        super(config, avconfig, travelTime, router, eventsManager, virtualNetwork);
        this.virtualNetwork = virtualNetwork;
        GlobalAssert.that(virtualNetwork != null);
        waitLocations = fillWaitLocations(network, virtualNetwork, (int) generatorConfig.getNumberOfVehicles());

    }

    @Override
    public void redispatch(double now) {
        // should get only taxi waiting at their "waiting point"

        Collection<RoboTaxi> vehicles = getRoboTaxiSubset(AVStatus.STAY);

        List<AVRequest> unassigned_requests = getUnassignedAVRequests();

        for (AVRequest avr : unassigned_requests) {
            VirtualNode<Link> virtualNode = virtualNetwork.getVirtualNode(avr.getFromLink());
            Optional<RoboTaxi> optVh = vehicles.stream().filter(v -> virtualNetwork.getVirtualNode(v.getDivertableLocation()).equals(virtualNode)).findAny();

            if (optVh.isPresent()) {
                RoboTaxi assigned_vehicle = optVh.get();
                setRoboTaxiPickup(assigned_vehicle, avr);
                vehicles.remove(assigned_vehicle);
            }
        }
        // return idle vehicles to assigned wait Link

        vehicles = getDivertableUnassignedRoboTaxis();
        for (RoboTaxi roboTaxi : vehicles) {
            VirtualNode<Link> vn = virtualNetwork.getVirtualNode(roboTaxi.getDivertableLocation());
            if (!roboTaxi.getDivertableLocation().equals(waitLocations.get(vn))) {
                setRoboTaxiRebalance(roboTaxi, waitLocations.get(vn));
            }
        }
    }

    /** @param network
     * @param virtualNetwork
     * @return HashMap<VirtualNode, Link> with one wait location per VirtualNode */

    private static Map<VirtualNode<Link>, Link> fillWaitLocations(Network network, VirtualNetwork<Link> virtualNetwork, int numberofRoboTaxis) {
        double carsPerVNode = ((double) numberofRoboTaxis) / virtualNetwork.getvNodesCount();

        Map<VirtualNode<Link>, Link> waitLocations = new HashMap<>();
        for (VirtualNode<Link> vn : virtualNetwork.getVirtualNodes()) {
            // select some link in virtualNode, if possible with high enough capacity
            @SuppressWarnings("unused")
            Link link = null;
            Optional<Link> optLink = vn.getLinks().stream().filter(v -> v.getCapacity() > carsPerVNode).filter(v -> v.getAllowedModes().contains("car"))
                    .findAny();
            link = optLink.isPresent() ? optLink.get() : vn.getLinks().stream().findAny().get();
            waitLocations.put(vn, vn.getLinks().stream().findAny().get());
        }
        return waitLocations;

    }

    public static class Factory implements AVDispatcherFactory {
        @Inject
        @Named(AVModule.AV_MODE)
        private ParallelLeastCostPathCalculator router;

        @Inject
        @Named(AVModule.AV_MODE)
        private TravelTime travelTime;

        @Inject
        private EventsManager eventsManager;

        @Inject
        private Network network;
        public static VirtualNetwork<Link> virtualNetwork;

        @Override
        public AVDispatcher createDispatcher(Config config, AVDispatcherConfig avconfig, AVGeneratorConfig generatorConfig) {
            virtualNetwork = VirtualNetworkGet.readDefault(network);
            return new KMedianDispatcher(config, avconfig, travelTime, generatorConfig, router, eventsManager, network, //
                    virtualNetwork);
        }
    }

}