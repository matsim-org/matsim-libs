package playground.clruch.dispatcher;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import playground.clruch.dispatcher.core.UniversalDispatcher;
import playground.clruch.dispatcher.core.VehicleLinkPair;
import playground.clruch.dispatcher.utils.AbstractRequestSelector;
import playground.clruch.dispatcher.utils.HungarBiPartVehicleDestMatcher;
import playground.clruch.dispatcher.utils.ImmobilizeVehicleDestMatcher;
import playground.clruch.dispatcher.utils.InOrderOfArrivalMatcher;
import playground.clruch.dispatcher.utils.OldestRequestSelector;
import playground.clruch.red3gen.DistanceFunction;
import playground.clruch.red3gen.KdTree;
import playground.clruch.red3gen.NearestNeighborIterator;
import playground.clruch.red3gen.SquareEuclideanDistanceFunction;
import playground.clruch.utils.SafeConfig;
import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.config.AVGeneratorConfig;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatcher;
import playground.sebhoerl.avtaxi.framework.AVModule;
import playground.sebhoerl.avtaxi.passenger.AVRequest;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

public class HungarianDispatcher extends UniversalDispatcher {

    private final int dispatchPeriod;
    private final int maxMatchNumber; // implementation may not use this
    private Tensor printVals = Tensors.empty();

    private HungarianDispatcher( //
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
            if(round_now == 11890){
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
        List<Link> requestlocsCut = reduceRequestsKDTree(requestlocs, divertableVehicles);
        System.out.println("number of requests before cutting: " + requestlocs.size());
        System.out.println("number of requetss after cutting:" + requestlocsCut.size());

        {
            returnTensor.append(Tensors.vectorInt(divertableVehicles.size(), requestlocsCut.size()));

            // find the Euclidean bipartite matching for all vehicles using the Hungarian method
            new HungarBiPartVehicleDestMatcher().match(divertableVehicles, requestlocsCut) //
                    .entrySet().forEach(dispatcher::setVehicleDiversion);
        }


        return returnTensor;
    }


    private static List<Link> reduceRequestsKDTree(List<Link> requestlocs, Collection<VehicleLinkPair> divertableVehicles) {
        HashMap<String, Link> requestsChosen = new HashMap<>();
        List<Link> toSearchrequestlocs = new LinkedList<>();
        DistanceFunction dist = new SquareEuclideanDistanceFunction();

        // if the number of requests is much larger than the number of available vehicles, reduce search space using kd trees
        if (requestlocs.size() > 5 * divertableVehicles.size() && requestlocs.size()>0 && divertableVehicles.size()>0) {
            // fill the KD tree with the links
            KdTree<RequestWithID> reqKDTree = new KdTree<>(2);
            int iter = 0;
            for (Link link : requestlocs) {
                if(link == null ||reqKDTree== null ){
                    System.out.println("problem here?");
                }
                reqKDTree.addPoint(new double[]{link.getCoord().getX(), link.getCoord().getY()}, new RequestWithID(link, Integer.toString(iter)));
                iter++;
            }

            // for all divertable vehicles, start nearestNeighborSearch until union is as large as the number of vehicles
            iter = 1;
            do {
                for (VehicleLinkPair vehicleLinkPair : divertableVehicles) {
                    double[] vehLoc = new double[]{vehicleLinkPair.getDivertableLocation().getToNode().getCoord().getX(),
                            vehicleLinkPair.getDivertableLocation().getToNode().getCoord().getX()};
                    NearestNeighborIterator<RequestWithID> myNearestNeighborIterator = reqKDTree.getNearestNeighborIterator(vehLoc, iter, dist);
                    for (RequestWithID requestWithID : myNearestNeighborIterator) {
                        requestsChosen.put(requestWithID.id, requestWithID.link);
                    }
                }
                ++iter;
            } while (requestsChosen.size() < divertableVehicles.size() && iter < 100);

            for(Link link : requestsChosen.values()){
                toSearchrequestlocs.add(link);
            }
            return toSearchrequestlocs;
        }
        return requestlocs;
    }


    // class to uniquely distinguish requests
    static class RequestWithID {
        public Link link;
        public String id;

        RequestWithID(Link linkIn, String idIn) {
            link = linkIn;
            id = idIn;
        }
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
            return new HungarianDispatcher( //
                    config, travelTime, router, eventsManager, network, abstractRequestSelector);
        }
    }
}
