package playground.clruch.dispatcher;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.zip.DataFormatException;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.geometry.CoordUtils;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import playground.clruch.dispatcher.core.PartitionedDispatcher;
import playground.clruch.dispatcher.core.UniversalDispatcher;
import playground.clruch.dispatcher.core.VehicleLinkPair;
import playground.clruch.dispatcher.utils.DrivebyRequestStopper;
import playground.clruch.dispatcher.utils.InOrderOfArrivalMatcher;
import playground.clruch.netdata.VirtualNetwork;
import playground.clruch.netdata.VirtualNetworkIO;
import playground.clruch.netdata.VirtualNode;
import playground.clruch.simonton.Cluster;
import playground.clruch.simonton.EuclideanDistancer;
import playground.clruch.simonton.MyTree;
import playground.clruch.utils.GlobalAssert;
import playground.clruch.utils.SafeConfig;
import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.config.AVGeneratorConfig;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatcher;
import playground.sebhoerl.avtaxi.framework.AVModule;
import playground.sebhoerl.avtaxi.passenger.AVRequest;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

public class UncoodrinatedDispatcher extends PartitionedDispatcher {
    private final int dispatchPeriod;

    final int numberOfAVs;
    final Network network; // <- for verifying link references
    final Map<VirtualNode, Link> waitLocations;

    private UncoodrinatedDispatcher( //
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
        if (round_now % dispatchPeriod == 0) {

            // stop all vehicles which are driving by an open request
            total_abortTrip += DrivebyRequestStopper.stopDrivingBy(getAVRequestsAtLinks(), getDivertableVehicleLinkPairs(), this::setVehiclePickup);

            { // TODO implement some logic here that matches the behavior of a currently operating taxi company.
              // currently not tested, not verified simplistic implementation.

                // for every unassigned request, send one vehicle from the same virtualNode

                // return all idle vehicles to wait Link
                Map<VirtualNode, List<VehicleLinkPair>> availableVehicles =  getVirtualNodeAvailableVehicles();
                for(VirtualNode vn : availableVehicles.keySet()){
                    for(VehicleLinkPair vlp : availableVehicles.get(vn)){
                        setVehicleRebalance(vlp.avVehicle, waitLocations.get(vn));                        
                    }
                }

            }
        } 
    }
//                        
//                setVehicleRebalance(avVehicle, destination);
//                
//
//                Iterator<AVRequest> requestIterator = getAVRequests().iterator();
//                while (requestIterator.hasNext()) {
//                    Link reqLink = requestIterator.next().getFromLink();
//                    VehicleLinkPair vehicletoMatch = null;
//                    if (round_now % dispatchPeriod * 3 == 0) {
//                        vehicletoMatch = someCloseVehicleReturner(getDivertableVehicles(), reqLink, 2);
//                    } else {
//                        vehicletoMatch = closeVehicleReturner(getDivertableVehicles(), reqLink, 2000.0);
//                    }
//
//                    /*
//                     * if (round_now % 600 == 0) { vehicletoMatch = closeVehicleReturner(getDivertableVehicles(), reqLink, 100000000000.0); } else {
//                     * vehicletoMatch = closeVehicleReturner(getDivertableVehicles(), reqLink, 2000.0); }
//                     */
//                    if (vehicletoMatch != null) {
//                        setVehicleDiversion(vehicletoMatch, reqLink);
//                        ++total_driveOrder;
//                    }

    /**
     * 
     * @param network
     * @param virtualNetwork
     * @return HashMap<VirtualNode, Link> with one wait location per VirtualNode
     */
    Map<VirtualNode, Link> fillWaitLocations(Network network, VirtualNetwork virtualNetwork){
        double carsPerVNode = ((double) numberOfAVs)/ virtualNetwork.getvNodesCount();
        
        Map<VirtualNode, Link> waitLocations = new HashMap<>();
        for(VirtualNode vn : virtualNetwork.getVirtualNodes()){
            // select some link in virtualNode, if possible with high enough capacity
            Link link = null;
            Optional<Link> optLink = vn.getLinks().stream()
                    .filter(v->v.getCapacity()>carsPerVNode)
                    .filter(v->v.getAllowedModes().contains("car")).findAny();
            link = optLink.isPresent() ?  optLink.get() :  vn.getLinks().stream().findAny().get();            
            waitLocations.put(vn,vn.getLinks().stream().findAny().get());
        }
        return waitLocations;
        
    }

    // TODO ugly implementation, make nicer using kdtree or similar
    VehicleLinkPair closeVehicleReturner(Collection<VehicleLinkPair> vehicleLinkPairs, Link reqlink, double MaxDist) {

        HashMap<VehicleLinkPair, Double> distanceMap = new HashMap<>();
        for (VehicleLinkPair vehicleLinkPair : vehicleLinkPairs) {
            distanceMap.put(vehicleLinkPair, CoordUtils.calcEuclideanDistance(vehicleLinkPair.getDivertableLocation().getCoord(), reqlink.getCoord()));
        }
        Optional<Map.Entry<VehicleLinkPair, Double>> totake = distanceMap.entrySet().stream().filter(v -> v.getValue() < MaxDist).findAny();
        if (totake.isPresent()) {
            return totake.get().getKey();
        } else
            return null;
    }

    /**
     *
     * @param vehicleLinkPairs
     * @param reqlink
     * @param numbNeigh
     * @return reurns random vehicleLinkPair among all vehicleLinkpairs among the numbNeigh closest ones to reqLink
     */
    VehicleLinkPair someCloseVehicleReturner(Collection<VehicleLinkPair> vehicleLinkPairs, Link reqlink, int numbNeigh) {
        // TODO code is fairly redundant to code in HungarianUtils
        // otherwise create KD tree and return reduced amount of requestlocs
        // create KD tree
        int dimensions = 2;
        int maxDensity = vehicleLinkPairs.size();
        double maxCoordinate = 1000000000000.0;
        int maxDepth = vehicleLinkPairs.size();
        MyTree<VehicleLinkPair> KDTree = new MyTree<>(dimensions, maxDensity, maxCoordinate, maxDepth);

        // add uniquely identifiable requests to KD tree
        // int reqIter = 0; // <- not used
        for (VehicleLinkPair vehicleLinkPair : vehicleLinkPairs) {
            double d1 = vehicleLinkPair.getDivertableLocation().getFromNode().getCoord().getX();
            double d2 = vehicleLinkPair.getDivertableLocation().getFromNode().getCoord().getY();
            GlobalAssert.that(Double.isFinite(d1));
            GlobalAssert.that(Double.isFinite(d2));
            KDTree.add(new double[] { d1, d2 }, vehicleLinkPair);
        }

        double[] vehLoc = new double[] { reqlink.getToNode().getCoord().getX(), reqlink.getToNode().getCoord().getY() };

        Cluster<VehicleLinkPair> nearestCluster = KDTree.buildCluster(vehLoc, numbNeigh, new EuclideanDistancer());
        Optional<VehicleLinkPair> optional = nearestCluster.getValues().stream().findAny();
        if (optional.isPresent())
            return optional.get();
        else
            return null;
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

            return new UncoodrinatedDispatcher( //
                    config, generatorConfig, travelTime, router, eventsManager, network, virtualNetwork);
        }
    }
}
