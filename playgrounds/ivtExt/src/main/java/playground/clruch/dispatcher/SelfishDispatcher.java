package playground.clruch.dispatcher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.collections.QuadTree;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import playground.clruch.dispatcher.core.UniversalDispatcher;
import playground.clruch.dispatcher.core.VehicleLinkPair;
import playground.clruch.dispatcher.utils.AbstractRequestSelector;
import playground.clruch.dispatcher.utils.InOrderOfArrivalMatcher;
import playground.clruch.dispatcher.utils.OldestRequestSelector;
import playground.clruch.utils.GlobalAssert;
import playground.clruch.utils.SafeConfig;
import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.config.AVGeneratorConfig;
import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatcher;
import playground.sebhoerl.avtaxi.framework.AVModule;
import playground.sebhoerl.avtaxi.passenger.AVRequest;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

public class SelfishDispatcher extends UniversalDispatcher {

    private final int dispatchPeriod;

    // specific to SelfishDispatcher
    private final int updateRefPeriod; // implementation may not use this
    private final int weiszfeldMaxIter; // Weiszfeld max iterations
    private final double weiszfeldTol; // Weiszfeld convergence tolerance
    private final boolean useVoronoiSets; // flag for using Voronoi sets in selfish strategy
    private boolean vehiclesInitialized = false;
    private final HashMap<AVVehicle, List<AVRequest>> requestsServed = new HashMap<>();
    private final HashMap<AVVehicle, Link> refPositions = new HashMap<>();
    private final Network network;
    private final QuadTree<AVRequest> pendingRequestsTree;
    private final QuadTree<Link> networkLinksTree;

    // two data structures are used to enable fast "contains" searching
    private final HashSet<AVRequest> openRequests = new HashSet<>();
    private final double[] networkBounds;

    private SelfishDispatcher( //
            AVDispatcherConfig avDispatcherConfig, //
            AVGeneratorConfig generatorConfig, //
            TravelTime travelTime, //
            ParallelLeastCostPathCalculator parallelLeastCostPathCalculator, //
            EventsManager eventsManager, //
            Network networkIn, AbstractRequestSelector abstractRequestSelector) {

        super(avDispatcherConfig, travelTime, parallelLeastCostPathCalculator, eventsManager);

        // Load parameters from av.xml
        SafeConfig safeConfig = SafeConfig.wrap(avDispatcherConfig);
        dispatchPeriod = safeConfig.getInteger("dispatchPeriod", 10);

        updateRefPeriod = safeConfig.getInteger("updateRefPeriod", 3600);
        weiszfeldMaxIter = safeConfig.getIntegerStrict("weiszfeldMaxIter");
        weiszfeldTol = safeConfig.getDoubleStrict("weiszfeldTol");
        useVoronoiSets = safeConfig.getBoolStrict("useVoronoiSets");

        network = networkIn;
        networkBounds = NetworkUtils.getBoundingBox(network.getNodes().values());
        pendingRequestsTree = new QuadTree<>(networkBounds[0], networkBounds[1], networkBounds[2], networkBounds[3]);
        networkLinksTree = buildNetworkTree();

    }

    @Override
    public void redispatch(double now) {
        final long round_now = Math.round(now);
        if (!vehiclesInitialized) {
            initializeVehicles();
        } else {
            if (round_now % dispatchPeriod == 0) {
                // add new open requests to list
                addOpenRequests(getAVRequests());
                GlobalAssert.that(openRequests.size() == pendingRequestsTree.size());
                
                // match vehicles on same link as request
                new InOrderOfArrivalMatcher(this::setAcceptRequest)
                    .matchRecord(getStayVehicles(), getAVRequestsAtLinks(), requestsServed, openRequests, pendingRequestsTree);

                // ensure all requests recorded properly
                GlobalAssert.that(requestsServed.values().stream().mapToInt(List::size).sum() == getTotalMatchedRequests());

                // update reference positions periodically
                if (round_now % updateRefPeriod == 0) {
                    updateRefPositions();
                }
                               
                // if requests present ...
                if (!getAVRequests().isEmpty()) { 
                    if (useVoronoiSets) { // check whether to use the Voronoi sets in the selfish strategy
           
                        HashMap<AVVehicle, QuadTree<AVRequest>> voronoiSets = computeVoronoiSets();
                                    
                        for (VehicleLinkPair vehicleLinkPair : getDivertableVehicles()) {
                            Coord vehicleCoord = vehicleLinkPair.getDivertableLocation().getFromNode().getCoord();
                            QuadTree<AVRequest> voronoiSet = voronoiSets.get(vehicleLinkPair.avVehicle);
                            if (voronoiSet.size() > 0) {
                                AVRequest closestRequest = voronoiSet.getClosest(vehicleCoord.getX(), vehicleCoord.getY());
                                // AVRequest closestRequest findClosestRequest(vehicleLinkPair, voronoiSet.values());
                                setVehicleDiversion(vehicleLinkPair, closestRequest.getFromLink());
                            }
                        }
                    } else { // send every vehicle to closest customer
                        for (VehicleLinkPair vehicleLinkPair : getDivertableVehicles()) {
                            Coord vehicleCoord = vehicleLinkPair.getDivertableLocation().getFromNode().getCoord();
                            AVRequest closestRequest = pendingRequestsTree.getClosest(vehicleCoord.getX(), vehicleCoord.getY());  
                            setVehicleDiversion(vehicleLinkPair, closestRequest.getFromLink());
                        }
                    }                    
                }
                
                // send remaining vehicles to their reference position
                for (VehicleLinkPair vehicleLinkPair : getDivertableVehicles()) {
                    GlobalAssert.that(refPositions.containsKey(vehicleLinkPair.avVehicle));
                    Link link = refPositions.get(vehicleLinkPair.avVehicle);
                    GlobalAssert.that(link != null);
                    setVehicleDiversion(vehicleLinkPair, link);
                }
            }
        }
    }

    /**
     * 
     */
    private QuadTree<Link> buildNetworkTree() {
        QuadTree<Link> networkQuadTree = new QuadTree<>(networkBounds[0], networkBounds[1], networkBounds[2], networkBounds[3]);
        Collection<? extends Link> networkLinks = network.getLinks().values();
        for (Link link : networkLinks) {
            GlobalAssert.that(link != null);
            Coord linkCoord = link.getFromNode().getCoord();
            boolean putSuccess = networkQuadTree.put(linkCoord.getX(), linkCoord.getY(), link);
            GlobalAssert.that(putSuccess);
        }
        return networkQuadTree;
    }

    /**
     * @param avRequests
     *            ensures that new open requests are added to a list with all
     *            open requests
     */
    private void addOpenRequests(Collection<AVRequest> avRequests) {
        for (AVRequest avRequest : avRequests) {
            if (!openRequests.contains(avRequest)) {
                Coord toMatchRequestCoord = avRequest.getFromLink().getFromNode().getCoord();
                boolean orSucc = openRequests.add(avRequest);
                boolean qtSucc = pendingRequestsTree.put( //
                        toMatchRequestCoord.getX(), //
                        toMatchRequestCoord.getY(), //
                        avRequest);
                GlobalAssert.that(orSucc == qtSucc && orSucc == true);
            }
        }

    }

    /**
     * @param vehicleLinkPair
     *            some vehicleLinkPair
     * @param avRequests
     *            list of AVRequests
     * @return the closest request (with respect to its From node coord)
     */
    private static AVRequest findClosestRequest(VehicleLinkPair vehicleLinkPair, Collection<AVRequest> avRequests) {
        
        Coord vehicleCoord = vehicleLinkPair.getDivertableLocation().getFromNode().getCoord();
        AVRequest closestRequest = null;
        double closestDistance = Double.POSITIVE_INFINITY;
        for (AVRequest avRequest : avRequests) {
            Coord requestCoord = avRequest.getFromLink().getFromNode().getCoord();
            double distance = Math.hypot(requestCoord.getX() - vehicleCoord.getX(), requestCoord.getY() - vehicleCoord.getY());
            if (distance < closestDistance) {
                closestDistance = distance;
                closestRequest = avRequest;
            }
        }
        GlobalAssert.that(closestRequest != null);
        return closestRequest;
    }

    /**
     * update the reference positions of all AVs
     */
    private void updateRefPositions() {
        GlobalAssert.that(!getMaintainedVehicles().isEmpty());
        GlobalAssert.that(0 < networkLinksTree.size());
        for (AVVehicle avVehicle : getMaintainedVehicles()) {
            Link initialGuess = refPositions.get(avVehicle); // initialize with previous Weber link
            Link weberLink = computeWeberLink(requestsServed.get(avVehicle), initialGuess);
            GlobalAssert.that(weberLink != null);
            refPositions.put(avVehicle, weberLink);
        }
    }

    /**
     * compute the Voronoi set for each AV
     */
    private HashMap<AVVehicle, QuadTree<AVRequest>> computeVoronoiSets() {
        
        HashMap<AVVehicle, QuadTree<AVRequest>> voronoiSets = new HashMap<>();
        for (AVVehicle avVehicle : getMaintainedVehicles()) {
            voronoiSets.put(avVehicle, new QuadTree<>(networkBounds[0], networkBounds[1], networkBounds[2], networkBounds[3]));
        }
        
        // for each outstanding request, assign it to the Voronoi set of the closest divertible AV
        for (AVRequest avRequest : getAVRequests()) {
            Coord requestCoord = avRequest.getFromLink().getFromNode().getCoord();
            AVVehicle closestVehicle = null;
            double closestDistance = Double.POSITIVE_INFINITY;
            for (VehicleLinkPair vehicleLinkPair : getDivertableVehicles()) {
                
                Coord avCoord = vehicleLinkPair.getDivertableLocation().getFromNode().getCoord();
                // Coord avCoord = refPositions.get(vehicleLinkPair.avVehicle).getCoord();
                
                double distance = Math.hypot(requestCoord.getX() - avCoord.getX(), requestCoord.getY() - avCoord.getY());
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestVehicle = vehicleLinkPair.avVehicle; // TODO how to deal with equidistant vehicles?
                }
            }
            GlobalAssert.that(closestVehicle != null);    
            boolean putSuccess = voronoiSets.get(closestVehicle).put(requestCoord.getX(), requestCoord.getY(), avRequest);
            GlobalAssert.that(putSuccess);
        }
        return voronoiSets;
    }
    
    /**
     *
     * @param avRequests
     *            list of requests served so far
     * @param initialGuess
     *            initial guess as a link for the iterative algorithm
     * @return closest link to 2D Weber point of past requests approximated by Weiszfeld's
     *         algorithm
     */
    private Link computeWeberLink(List<AVRequest> avRequests, Link initialGuess) {
        if (avRequests.isEmpty()) {
            return initialGuess;
        }
        List<Coord> requestCoords = new ArrayList<>();
        for (AVRequest avRequest : avRequests) {
            requestCoords.add(avRequest.getFromLink().getCoord());
        }
        
        Coord weberCoord = weiszfeldAlgorithm(requestCoords, initialGuess.getCoord());
        
        // Weber point must be inside the network
        GlobalAssert.that((weberCoord.getX() >= networkBounds[0]) 
                       && (weberCoord.getY() >= networkBounds[1]) 
                       && (weberCoord.getX() <= networkBounds[2])
                       && (weberCoord.getY() <= networkBounds[3]));

        Link weberLink = networkLinksTree.getClosest(weberCoord.getX(), weberCoord.getY());
        GlobalAssert.that(weberLink != null);

        return weberLink;
    }
    
    /**
    *
    * @param data
    *            list of data points in 2D Euclidean space
    * @param initialGuess
    *            initial guess as a coordinate for the iterative algorithm
    * @return 2D Weber point of the data coordinates approximated by Weiszfeld's
    *         algorithm
    */
   private Coord weiszfeldAlgorithm(List<Coord> data, Coord initialGuess) {
       if (data.isEmpty()) {
           return initialGuess;
       }
       double weberX = initialGuess.getX();
       double weberY = initialGuess.getY();
       int count = 0;
       while (count <= weiszfeldMaxIter) {
           double x = 0.0;
           double y = 0.0;
           double normalizer = 0.0;
           for (Coord point : data) {
               double distance = Math.hypot(point.getX() - weberX, point.getY() - weberY);
               if (distance != 0) {
                   x += point.getX() / distance;
                   y += point.getY() / distance;
                   normalizer += 1.0 / distance;
               } else {
                   x = point.getX();
                   y = point.getY();
                   normalizer = 1.0;
                   break;
               }
           }
           x /= normalizer;
           y /= normalizer;
           double change = Math.hypot(x - weberX, y - weberY);
           weberX = x;
           weberY = y;
           if (change < weiszfeldTol) {
               break;
           }
           count++;
       }
       return new Coord(weberX, weberY);
   }

    /**
     * initializes the vehicle Lists which are used for vehicle specific
     * tracking
     */
    private void initializeVehicles() {
        // collection of all links in the network
        Collection<? extends Link> networkLinksCol = network.getLinks().values();
        // create a list from this collection
        List<Link> networkLinks = new ArrayList<>();
        for (Link link : networkLinksCol) {
            networkLinks.add(link);
        }
        for (AVVehicle avVehicle : getMaintainedVehicles()) {
            requestsServed.put(avVehicle, new ArrayList<>());
            Collections.shuffle(networkLinks);

            // assign a random link from the network to every AV
            refPositions.put(avVehicle, networkLinks.get(0));
        }
        vehiclesInitialized = true;
    }

    /**
     * 
     */
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
                    config, generatorConfig, travelTime, router, eventsManager, network, abstractRequestSelector);
        }
    }
}
