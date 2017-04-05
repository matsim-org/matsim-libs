package playground.clruch.dispatcher;

import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.util.TravelTime;
import playground.clruch.dispatcher.core.UniversalDispatcher;
import playground.clruch.dispatcher.core.VehicleLinkPair;
import playground.clruch.dispatcher.utils.*;
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

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class SelfishDispatcher extends UniversalDispatcher {

    private final int dispatchPeriod;
    private final int maxMatchNumber; // implementation may not use this
    private Tensor printVals = Tensors.empty();

    private SelfishDispatcher( //
            AVDispatcherConfig avDispatcherConfig, //
            TravelTime travelTime, //
            ParallelLeastCostPathCalculator parallelLeastCostPathCalculator, //
            EventsManager eventsManager, //
            Network network, AbstractRequestSelector abstractRequestSelector) {
        super(avDispatcherConfig, travelTime, parallelLeastCostPathCalculator, eventsManager);
        SafeConfig safeConfig = SafeConfig.wrap(avDispatcherConfig);
        dispatchPeriod = safeConfig.getInteger("dispatchPeriod", 10);
        maxMatchNumber = safeConfig.getInteger("maxMatchNumber", Integer.MAX_VALUE);
    }

    @Override
    public void redispatch(double now) {
        final long round_now = Math.round(now);

        new InOrderOfArrivalMatcher(this::setAcceptRequest) //
                .match(getStayVehicles(), getAVRequestsAtLinks());

        if (round_now % dispatchPeriod == 0) {
            if (round_now == 11890) {
                System.out.println("arrived at problem");
            }
            printVals = globalBipartiteMatching(this, () -> getDivertableVehicles());

        }
    }

    public static Tensor globalBipartiteMatching(UniversalDispatcher dispatcher, Supplier<Collection<VehicleLinkPair>> supplier) {
        // assign new destination to vehicles with bipartite matching

        Tensor returnTensor = Tensors.empty();

        Map<Link, List<AVRequest>> requests = dispatcher.getAVRequestsAtLinks();

        // reduce the number of requests for a smaller running time
        // take out and match request-vehicle pairs with distance zero
        List<Link> requestlocs = requests.values().stream() //
                .flatMap(List::stream) // all from links of all requests with
                .map(AVRequest::getFromLink) // multiplicity
                .collect(Collectors.toList());

        {
            // call getDivertableVehicles again to get remaining vehicles
            Collection<VehicleLinkPair> divertableVehicles = supplier.get();

            returnTensor.append(Tensors.vectorInt(divertableVehicles.size(), requestlocs.size()));

            // this call removes the matched links from requestlocs
            new ImmobilizeVehicleDestMatcher().match(divertableVehicles, requestlocs) //
                    .entrySet().forEach(dispatcher::setVehicleDiversion);
        }

        /*
         * // only select MAXMATCHNUMBER of oldest requests
         * Collection<AVRequest> avRequests = requests.values().stream().flatMap(v -> v.stream()).collect(Collectors.toList());
         * Collection<AVRequest> avRequestsReduced = requestSelector.selectRequests(divertableVehicles, avRequests, MAXMATCHNUMBER);
         */

        // call getDivertableVehicles again to get remaining vehicles
        Collection<VehicleLinkPair> divertableVehicles = supplier.get();

        // in case requests >> divertablevehicles or divertablevehicles >> requests reduce the search space using kd-trees
        returnTensor.append(Tensors.vectorInt(divertableVehicles.size(), requestlocs.size()));
        List<Link> requestlocsCut = reduceRequestsKDTree(requestlocs, divertableVehicles);
        Collection<VehicleLinkPair> divertableVehiclesCut = reduceVehiclesKDTree(requestlocs, divertableVehicles);
        returnTensor.append(Tensors.vectorInt(divertableVehiclesCut.size(), requestlocsCut.size()));

        // System.out.println("number of available vehicles before cutting: " + divertableVehicles.size());
        // System.out.println("number of available vehicles after cutting: " + divertableVehiclesCut.size());
        // System.out.println("number of requests before cutting: " + requestlocs.size());
        // System.out.println("number of requetss after cutting:" + requestlocsCut.size());

        {

            // find the Euclidean bipartite matching for all vehicles using the Hungarian method
            new HungarBiPartVehicleDestMatcher().match(divertableVehicles, requestlocsCut) //
                    .entrySet().forEach(dispatcher::setVehicleDiversion);
        }

        return returnTensor;
    }

    // class to uniquely distinguish requests
    static class RequestWithID {
        public Link link;
        public int id;

        RequestWithID(Link linkIn, int idIn) {
            link = linkIn;
            id = idIn;
        }
    }

    private static List<Link> reduceRequestsKDTree(List<Link> requestlocs, Collection<VehicleLinkPair> divertableVehicles) {
        // for less requests than cars, don't do anything
        if (requestlocs.size() < divertableVehicles.size())
            return requestlocs;

        // otherwise create KD tree and return reduced amount of requestlocs
        // create KD tree
        int dimensions = 2;
        int maxDensity = requestlocs.size();
        double maxCoordinate = 1000000000000.0;
        int maxDepth = requestlocs.size();
        MyTree<RequestWithID> KDTree = new MyTree<>(dimensions, maxDensity, maxCoordinate, maxDepth);

        // add uniquely identifiable requests to KD tree
        int reqIter = 0;
        for (Link link : requestlocs) {
            double d1 = link.getFromNode().getCoord().getX();
            double d2 = link.getFromNode().getCoord().getY();
            GlobalAssert.that(Double.isFinite(d1));
            GlobalAssert.that(Double.isFinite(d2));
            KDTree.add(new double[] { d1, d2 }, new RequestWithID(link, reqIter));
            reqIter++;
        }

        // for all divertable vehicles, start nearestNeighborSearch until union is as large as the number of vehicles
        // start with only one request per vehicle
        HashMap<RequestWithID, Link> requestsChosen = new HashMap<>();
        int iter = 1;
        do {
            requestsChosen.clear();
            for (VehicleLinkPair vehicleLinkPair : divertableVehicles) {
                double[] vehLoc = new double[] { vehicleLinkPair.getDivertableLocation().getToNode().getCoord().getX(),
                        vehicleLinkPair.getDivertableLocation().getToNode().getCoord().getY() };
                Cluster<RequestWithID> nearestCluster = KDTree.buildCluster(vehLoc, iter, new EuclideanDistancer());
                nearestCluster.getValues().stream().forEach(v -> requestsChosen.put(v, v.link));
            }
            ++iter;
        } while (requestsChosen.size() < divertableVehicles.size() && iter <= divertableVehicles.size());

        List<Link> toSearchrequestlocs = new LinkedList<>();
        requestsChosen.values().stream().forEach(v -> toSearchrequestlocs.add(v));

        if (toSearchrequestlocs.size() < divertableVehicles.size()) {
            System.out.println("Hoppla");
        }

        return toSearchrequestlocs;
    }

    private static Collection<VehicleLinkPair> reduceVehiclesKDTree(List<Link> requestlocs, Collection<VehicleLinkPair> divertableVehicles) {
        // for less requests than cars, don't do anything
        if (divertableVehicles.size() < requestlocs.size())
            return divertableVehicles;

        // otherwise create KD tree and return reduced amount of requestlocs
        // create KD tree
        int dimensions = 2;
        int maxDensity = divertableVehicles.size();
        double maxCoordinate = 1000000000000.0;
        int maxDepth = divertableVehicles.size();
        MyTree<VehicleLinkPair> KDTree = new MyTree<>(dimensions, maxDensity, maxCoordinate, maxDepth);

        // add uniquely identifiable requests to KD tree
        int vehIter = 0;
        for (VehicleLinkPair vehicleLinkPair : divertableVehicles) {
            double d1 = vehicleLinkPair.getDivertableLocation().getToNode().getCoord().getX();
            double d2 = vehicleLinkPair.getDivertableLocation().getToNode().getCoord().getY();
            GlobalAssert.that(Double.isFinite(d1));
            GlobalAssert.that(Double.isFinite(d2));
            KDTree.add(new double[] { d1, d2 }, vehicleLinkPair);
            vehIter++;
        }

        // for all requests, start nearestNeighborSearch until union is as large as the number of requests
        // start with only one vehicle per request
        Collection<VehicleLinkPair> vehiclesChosen = new LinkedList<>();
        int iter = 1;
        do {
            vehiclesChosen.clear();
            for (Link link : requestlocs) {
                double[] reqloc = new double[] { link.getFromNode().getCoord().getX(), link.getFromNode().getCoord().getY() };
                Cluster<VehicleLinkPair> nearestCluster = KDTree.buildCluster(reqloc, iter, new EuclideanDistancer());
                nearestCluster.getValues().stream().forEach(v -> vehiclesChosen.add(v));
            }
            ++iter;
        } while (vehiclesChosen.size() < requestlocs.size() && iter <= requestlocs.size());

        return vehiclesChosen;
    }

    @Override
    public String getInfoLine() {
        return String.format("%s H=%s", //
                super.getInfoLine(), //
                printVals.toString() //
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
            AbstractRequestSelector abstractRequestSelector = new OldestRequestSelector();
            return new SelfishDispatcher( //
                    config, travelTime, router, eventsManager, network, abstractRequestSelector);
        }
    }
}
