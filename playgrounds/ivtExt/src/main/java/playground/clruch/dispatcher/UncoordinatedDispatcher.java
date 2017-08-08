package playground.clruch.dispatcher;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.DataFormatException;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import playground.clruch.dispatcher.core.PartitionedDispatcher;
import playground.clruch.dispatcher.core.RoboTaxi;
import playground.clruch.dispatcher.utils.DrivebyRequestStopper;
import playground.clruch.netdata.VirtualNetwork;
import playground.clruch.netdata.VirtualNetworkIO;
import playground.clruch.netdata.VirtualNode;
import playground.clruch.utils.GlobalAssert;
import playground.clruch.utils.SafeConfig;
import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.config.AVGeneratorConfig;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatcher;
import playground.sebhoerl.avtaxi.framework.AVModule;
import playground.sebhoerl.avtaxi.passenger.AVRequest;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

/**
 * This dispatcher tries to simulate the behavior of an UncoordinatedFleet as for instance todays 
 * taxi operations. Not verified with real-world data. Initial working version.
 * @author Claudio Ruch
 *
 */
public class UncoordinatedDispatcher extends PartitionedDispatcher {
    private final int dispatchPeriod;

    final int numberOfAVs;
    final double maxWaitTime = 5 * 60.0;;
    final Network network; // <- for verifying link references
    final Map<VirtualNode, Link> waitLocations;

    private UncoordinatedDispatcher( //
            AVDispatcherConfig config, //
            AVGeneratorConfig generatorConfig, //
            TravelTime travelTime, //
            ParallelLeastCostPathCalculator router, //
            EventsManager eventsManager, //
            Network network, //
            VirtualNetwork virtualNetwork) {
        super(config, travelTime, router, eventsManager, virtualNetwork);
        SafeConfig safeConfig = SafeConfig.wrap(config);
        this.network = network;
        numberOfAVs = (int) generatorConfig.getNumberOfVehicles();
        dispatchPeriod = getDispatchPeriod(safeConfig, 10); // safeConfig.getInteger("dispatchPeriod", 10);
        waitLocations = fillWaitLocations(network, virtualNetwork);
    }

    int total_abortTrip = 0;
    int total_driveOrder = 0;

    @Override
    public void redispatch(double now) {

        final long round_now = Math.round(now);
        if (round_now % dispatchPeriod == 0 && round_now > 100) {

            // stop all vehicles which are driving by an open request
            total_abortTrip += DrivebyRequestStopper.stopDrivingBy(getAVRequestsAtLinks(), getDivertableRoboTaxis(), this::setRoboTaxiPickup);

            
            { // TODO implement some logic here that matches the behavior of a currently operating taxi company.
              // currently not tested, not verified simplistic implementation.

                // I: for every unassigned request, send one vehicle from the same virtualNode
                Collection<RoboTaxi> vehicles = getDivertableUnassignedRoboTaxis();
                List<AVRequest> unassignedRequests = getUnassignedAVRequests();

                for (AVRequest avr : unassignedRequests) {
                    VirtualNode vn = virtualNetwork.getVirtualNode(avr.getFromLink());
                    Optional<RoboTaxi> optVh = vehicles.stream()
                            .filter(v -> virtualNetwork.getVirtualNode(v.getDivertableLocation()).equals(vn)).findAny();
                    if (optVh.isPresent()) {
                        RoboTaxi vehicleToSend = optVh.get();
                        setRoboTaxiPickup(vehicleToSend, avr);
                        vehicles.remove(vehicleToSend);
                    }
                }

                // II: if request is waiting for more than maxWaitTime mins, send any available vehicle (if existing)
                unassignedRequests = getUnassignedAVRequests();
                for (AVRequest avr : unassignedRequests) {
                    if (now - avr.getSubmissionTime() > maxWaitTime) {
                        Optional<RoboTaxi> optVh = vehicles.stream().findAny();
                        if (optVh.isPresent()) {
                            RoboTaxi vehicleToSend = optVh.get();
                            setRoboTaxiPickup(vehicleToSend, avr);
                            vehicles.remove(vehicleToSend);
                        }
                    }
                }

                // III: return all idle vehicles to wait Link
                vehicles = getDivertableUnassignedRoboTaxis();
                for (RoboTaxi roboTaxi : vehicles) {
                    VirtualNode vn = virtualNetwork.getVirtualNode(roboTaxi.getDivertableLocation());
                    if(!roboTaxi.getDivertableLocation().equals(waitLocations.get(vn))){
                        setRoboTaxiRebalance(roboTaxi, waitLocations.get(vn));                        
                    }
                }
            }
        }
    }

    /**
     * 
     * @param network
     * @param virtualNetwork
     * @return HashMap<VirtualNode, Link> with one wait location per VirtualNode
     */
    Map<VirtualNode, Link> fillWaitLocations(Network network, VirtualNetwork virtualNetwork) {
        double carsPerVNode = ((double) numberOfAVs) / virtualNetwork.getvNodesCount();

        Map<VirtualNode, Link> waitLocations = new HashMap<>();
        for (VirtualNode vn : virtualNetwork.getVirtualNodes()) {
            // select some link in virtualNode, if possible with high enough capacity
            Link link = null;
            Optional<Link> optLink = vn.getLinks().stream().filter(v -> v.getCapacity() > carsPerVNode)
                    .filter(v -> v.getAllowedModes().contains("car")).findAny();
            link = optLink.isPresent() ? optLink.get() : vn.getLinks().stream().findAny().get();
            waitLocations.put(vn, vn.getLinks().stream().findAny().get());
        }
        return waitLocations;

    }

    @Override
    public String getInfoLine() {
        return String.format("%s AT=%5d do=%5d", //
                super.getInfoLine(), //
                total_abortTrip, //
                total_driveOrder //
        );
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
        public static VirtualNetwork virtualNetwork;

        @Override
        public AVDispatcher createDispatcher(AVDispatcherConfig config, AVGeneratorConfig generatorConfig) {

            final File virtualnetworkDir = new File(config.getParams().get("virtualNetworkDirectory"));
            GlobalAssert.that(virtualnetworkDir.isDirectory());
            {
                final File virtualnetworkFile = new File(virtualnetworkDir, "virtualNetwork");
                GlobalAssert.that(virtualnetworkFile.isFile());
                try {
                    virtualNetwork = VirtualNetworkIO.fromByte(network, virtualnetworkFile);
                } catch (ClassNotFoundException | DataFormatException | IOException e) {
                    e.printStackTrace();
                }
            }

            return new UncoordinatedDispatcher( //
                    config, generatorConfig, travelTime, router, eventsManager, network, virtualNetwork);
        }
    }
}
