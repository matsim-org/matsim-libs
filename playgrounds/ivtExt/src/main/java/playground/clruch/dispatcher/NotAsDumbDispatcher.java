package playground.clruch.dispatcher;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import playground.clruch.dispatcher.core.UniversalDispatcher;
import playground.clruch.dispatcher.core.VehicleLinkPair;
import playground.clruch.dispatcher.utils.DrivebyRequestStopper;
import playground.clruch.dispatcher.utils.InOrderOfArrivalMatcher;
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

public class NotAsDumbDispatcher extends UniversalDispatcher {
    private final int dispatchPeriod;

    final Network network; // <- for verifying link references
    final Collection<Link> linkReferences; // <- for verifying link references

    private NotAsDumbDispatcher( //
            AVDispatcherConfig avDispatcherConfig, //
            TravelTime travelTime, //
            ParallelLeastCostPathCalculator parallelLeastCostPathCalculator, //
            EventsManager eventsManager, //
            Network network) {
        super(avDispatcherConfig, travelTime, parallelLeastCostPathCalculator, eventsManager);
        SafeConfig safeConfig = SafeConfig.wrap(avDispatcherConfig);
        this.network = network;
        linkReferences = new HashSet<>(network.getLinks().values());
        // vehicleRequestMatcher = ;
        dispatchPeriod = safeConfig.getInteger("dispatchPeriod", 10);
    }

    /**
     * verify that link references are present in the network
     */
    @SuppressWarnings("unused") // for verifying link references
    private void verifyLinkReferencesInvariant() {
        List<Link> testset = getDivertableVehicles().stream() //
                .map(VehicleLinkPair::getCurrentDriveDestination) //
                .filter(Objects::nonNull) //
                .collect(Collectors.toList());
        if (!linkReferences.containsAll(testset))
            throw new RuntimeException("network change 1");
        if (!linkReferences.containsAll(network.getLinks().values()))
            throw new RuntimeException("network change 2");
        if (0 < testset.size())
            System.out.println("network " + linkReferences.size() + " contains all " + testset.size());
    }

    int total_abortTrip = 0;
    int total_driveOrder = 0;

    @Override
    public void redispatch(double now) {
        // verifyReferences(); // <- debugging only

        new InOrderOfArrivalMatcher(this::setAcceptRequest).match(getStayVehicles(), getAVRequestsAtLinks());

        final long round_now = Math.round(now);
        if (round_now % dispatchPeriod == 0) {

            total_abortTrip += new DrivebyRequestStopper(this::setVehicleDiversion).realize(getAVRequestsAtLinks(), getDivertableVehicles());

            { // TODO this should be replaceable by some naive matcher

                Iterator<AVRequest> requestIterator = getAVRequests().iterator();
                while (requestIterator.hasNext()) {
                    Link reqLink = requestIterator.next().getFromLink();
                    VehicleLinkPair vehicletoMatch = null;
                    if (round_now % dispatchPeriod * 3 == 0) {
                        vehicletoMatch = someCloseVehicleReturner(getDivertableVehicles(), reqLink, 2);
                    } else {
                        vehicletoMatch = closeVehicleReturner(getDivertableVehicles(), reqLink, 2000.0);
                    }

                    /*
                     * if (round_now % 600 == 0) {
                     * vehicletoMatch = closeVehicleReturner(getDivertableVehicles(), reqLink, 100000000000.0);
                     * } else {
                     * vehicletoMatch = closeVehicleReturner(getDivertableVehicles(), reqLink, 2000.0);
                     * }
                     */
                    if (vehicletoMatch != null) {
                        setVehicleDiversion(vehicletoMatch, reqLink);
                        ++total_driveOrder;
                    }
                }

                /*
                 * 
                 * 
                 * 
                 * for (VehicleLinkPair vehicleLinkPair : getDivertableVehicles()) {
                 * Iterator<AVRequest> requestIterator = getAVRequests().iterator();
                 * Link dest = vehicleLinkPair.getCurrentDriveDestination();
                 * Link avloc = vehicleLinkPair.getDivertableLocation();
                 * if (dest == null && requestIterator.hasNext()) { // vehicle in stay task
                 * Link closestLink = requestIterator.next().getFromLink();
                 * double closestDist = LinkDistance(closestLink, avloc);
                 * while (requestIterator.hasNext()) {
                 * Link newLink = requestIterator.next().getFromLink();
                 * if (LinkDistance(newLink, avloc) < closestDist) {
                 * closestLink = newLink;
                 * }
                 * }
                 * setVehicleDiversion(vehicleLinkPair, closestLink);
                 * ++total_driveOrder;
                 * } else
                 * break;
                 * }
                 * 
                 */
            }
        }

    }

    // TODO ugly implementation, make nicer using kdtree or similar
    VehicleLinkPair closeVehicleReturner(Collection<VehicleLinkPair> vehicleLinkPairs, Link reqlink, double MaxDist) {

        HashMap<VehicleLinkPair, Double> distanceMap = new HashMap<>();
        for (VehicleLinkPair vehicleLinkPair : vehicleLinkPairs) {
            distanceMap.put(vehicleLinkPair, LinkDistance(vehicleLinkPair.getDivertableLocation(), reqlink));
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
        int reqIter = 0;
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

    // TODO replace by MATSim internal function?
    double LinkDistance(Link l1, Link l2) {
        Coord c1 = l1.getCoord();
        Coord c2 = l2.getCoord();
        double dx = c1.getX() - c2.getX();
        double dy = c1.getY() - c2.getY();
        return Math.hypot(dx, dy);
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

        @Override
        public AVDispatcher createDispatcher(AVDispatcherConfig config, AVGeneratorConfig generatorConfig) {
            return new NotAsDumbDispatcher( //
                    config, travelTime, router, eventsManager, network);
        }
    }
}
