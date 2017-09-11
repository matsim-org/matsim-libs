package playground.clruch.dispatcher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.collections.QuadTree;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import ch.ethz.idsc.queuey.util.GlobalAssert;
import playground.clruch.dispatcher.core.RebalancingDispatcher;
import playground.clruch.dispatcher.core.RoboTaxi;
import playground.clruch.dispatcher.utils.AbstractRequestSelector;
import playground.clruch.dispatcher.utils.OldestRequestSelector;
import playground.clruch.utils.SafeConfig;
import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.config.AVGeneratorConfig;
import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatcher;
import playground.sebhoerl.avtaxi.framework.AVModule;
import playground.sebhoerl.avtaxi.passenger.AVRequest;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

@Deprecated
//ATTENTION: THIS DISPATCHER HAS NOT BEEN TESTED WITH THE NEW INTERFACE, LIKELY NOT TO 
// FUNCTION CORRECTLY.
public class SpencerSelfishOldNotWorking extends RebalancingDispatcher {

    private final int dispatchPeriod;

    // specific to SelfishDispatcher
    private final int updateRefPeriod; // implementation may not use this
    private final int weiszfeldMaxIter; // Weiszfeld max iterations
    private final double weiszfeldTol; // Weiszfeld convergence tolerance
    private final boolean nonStrict;
    
    private final String subStrategy;
    private final double primaryWeight;
    private final double secondaryWeight;
    
    
    private boolean vehiclesInitialized = false;
    private final HashMap<AVVehicle, List<AVRequest>> requestsServed = new HashMap<>();
    private final HashMap<AVVehicle, Link> refPositions = new HashMap<>();
    private final Network network;
    private final QuadTree<AVRequest> pendingRequestsTree;
    private final QuadTree<Link> networkLinksTree;

    // two data structures are used to enable fast "contains" searching
    private final HashSet<AVRequest> openRequests = new HashSet<>();
    private final double[] networkBounds;

    private SpencerSelfishOldNotWorking( //
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
        
        subStrategy = safeConfig.getStringStrict("subStrategy");
        GlobalAssert.that(subStrategy.equals("noComm") 
                       || subStrategy.equals("voronoi")  
                       || subStrategy.equals("selfishLoiter") );
        primaryWeight = safeConfig.getDoubleStrict("primaryWeight");
        secondaryWeight = safeConfig.getDoubleStrict("secondaryWeight");
        
        network = networkIn;
        networkBounds = NetworkUtils.getBoundingBox(network.getNodes().values());
        pendingRequestsTree = new QuadTree<>(networkBounds[0], networkBounds[1], networkBounds[2], networkBounds[3]);
        networkLinksTree = buildNetworkTree();
        nonStrict = true; // TODO improve API design here
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
                
                // ensure all requests recorded properly
                GlobalAssert.that(false);
                // TODO getTotalMatchedRequests was removed from API, find different solution. 
                
                // /** @return total matched request until now */
                // public int getTotalMatchedRequests() {
                // return total_matchedRequests;
                // }
                
                //GlobalAssert.that(requestsServed.values().stream().mapToInt(List::size).sum() == getTotalMatchedRequests());

                // update reference positions periodically
                if ((round_now % updateRefPeriod == 0) && (requestsServed.size() > 10)) {
                    updateRefPositions();
                }
                               
                // if requests present ...
                if (!getAVRequests().isEmpty()) { 
                    if (subStrategy.equals("noComm")) { // send every vehicle to closest customer
                        for (RoboTaxi vehicleLinkPair : getDivertableRoboTaxis()) {
                            Coord vehicleCoord = vehicleLinkPair.getDivertableLocation().getFromNode().getCoord();
                            AVRequest closestRequest = pendingRequestsTree.getClosest(vehicleCoord.getX(), vehicleCoord.getY()); 
                            setRoboTaxiPickup(vehicleLinkPair, closestRequest);
                        }
                    } else if (subStrategy.equals("voronoi")) { // use the Voronoi sets in the selfish strategy
                        HashMap<AVVehicle, QuadTree<AVRequest>> voronoiSets = computeVoronoiSets();
                        for (RoboTaxi vehicleLinkPair : getDivertableRoboTaxis()) {
                            Coord vehicleCoord = vehicleLinkPair.getDivertableLocation().getFromNode().getCoord();
                            // TODO adapt to new API
                            GlobalAssert.that(false);
//                            QuadTree<AVRequest> voronoiSet = voronoiSets.get(vehicleLinkPair.getAVVehicle());
                            QuadTree<AVRequest> voronoiSet = voronoiSets.get(null);
                            if (voronoiSet.size() > 0) {
                                AVRequest closestRequest = voronoiSet.getClosest(vehicleCoord.getX(), vehicleCoord.getY());
                                // AVRequest closestRequest = findClosestRequest(vehicleLinkPair, voronoiSet.values());
                                setRoboTaxiPickup(vehicleLinkPair, closestRequest);
                            }
                        }   
                    } else if (subStrategy.equals("selfishLoiter")) { //
                        for (AVRequest pendingRequest : getAVRequests()) {
                            if (getDivertableRoboTaxis().size() == 0) {
                                continue;
                            } else {
                            RoboTaxi closestDivertableVehicle = findClosestDivertableVehicle(pendingRequest);
                            // TODO instead of just diverting, MATCH the closest vehicle with the pending request
                            setRoboTaxiPickup(closestDivertableVehicle, pendingRequest);
                            // setAcceptRequest(closestDivertableVehicle.avVehicle, pendingRequest); // TODO throws error 
                            }
                        }
                    }                    
                }
                
                // send remaining vehicles to their reference position
                for (RoboTaxi vehicleLinkPair : getDivertableRoboTaxis()) {
                    // TODO adapt to new API
//                    GlobalAssert.that(refPositions.containsKey(vehicleLinkPair.getAVVehicle()));
                    GlobalAssert.that(refPositions.containsKey(null));
//                    Link link = refPositions.get(vehicleLinkPair.getAVVehicle());
                    Link link = refPositions.get(null);
                    GlobalAssert.that(link != null);
                    // setVehicleDiversion(vehicleLinkPair, link);
                    setRoboTaxiRebalance(vehicleLinkPair, link);
                }
            }
        }
    }

    /**
     * build a quadtree for all links in the network (with respect to their from-nodes)
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
    private RoboTaxi findClosestDivertableVehicle(AVRequest avRequest) {
        
        Coord requestCoord = avRequest.getFromLink().getCoord();
        RoboTaxi closestVehicle = null;
        double closestDistance = Double.POSITIVE_INFINITY;
        for (RoboTaxi vehicleLinkPair : getDivertableRoboTaxis()) {
            Coord vehicleCoord = vehicleLinkPair.getDivertableLocation().getFromNode().getCoord();
            double distance = Math.hypot(requestCoord.getX() - vehicleCoord.getX(), requestCoord.getY() - vehicleCoord.getY());
            if (distance < closestDistance) {
                closestDistance = distance;
                closestVehicle = vehicleLinkPair;
            }
        }
        GlobalAssert.that(closestVehicle != null);
        return closestVehicle;
    }

    /**
     * update the reference positions of all AVs
     */
    private void updateRefPositions() {
        GlobalAssert.that(!getRoboTaxis().isEmpty());
        GlobalAssert.that(0 < networkLinksTree.size());
        
        Set<AVVehicle> replaceMeSet = new HashSet<>(); // use getRoboTaxis and iterate over RoboTaxis // TODO
        
        for (AVVehicle thisAV : replaceMeSet) {
            Link initialGuess = refPositions.get(thisAV); // initialize with previous Weber link
            Link weberLink;
            if ( subStrategy.equals("noComm")  || subStrategy.equals("voronoi") ) {
                ArrayList<Double> useEqualWeights = new ArrayList<>();
                weberLink = computeWeberLink(requestsServed.get(thisAV), useEqualWeights, initialGuess);
            } else {
                List<AVRequest> allPastRequests = new ArrayList<>();
                ArrayList<Double> weights = new ArrayList<>();
                for (AVVehicle otherAV : replaceMeSet) {
                    allPastRequests.addAll(requestsServed.get(otherAV));
                    ArrayList<Double> avWeights = new ArrayList<>(requestsServed.get(otherAV).size());
                    for (int idx = 0; idx < requestsServed.get(otherAV).size(); idx++) {
                        if (otherAV == thisAV) {
                            avWeights.add(primaryWeight);   // use the primary weight for requests served by this AV      
                        } else {
                            avWeights.add(secondaryWeight); // use the secondary weight for requests served by others
                        }
                    }
                    weights.addAll(avWeights);
                }
                weberLink = computeWeberLink(allPastRequests, weights, initialGuess);
            }
            GlobalAssert.that(weberLink != null);
            refPositions.put(thisAV, weberLink);
        }
    }

    /**
     * compute the Voronoi set for each AV
     */
    private HashMap<AVVehicle, QuadTree<AVRequest>> computeVoronoiSets() {
        
        HashMap<AVVehicle, QuadTree<AVRequest>> voronoiSets = new HashMap<>();
        // INSTEAD OF MAINTAINEDVEHICLES USE getRoboTaxis();
        Set<AVVehicle> replaceMeSet = new HashSet<>();
        for (AVVehicle avVehicle : replaceMeSet) {
            voronoiSets.put(avVehicle, new QuadTree<>(networkBounds[0], networkBounds[1], networkBounds[2], networkBounds[3]));
        }
        
        // for each outstanding request, assign it to the Voronoi set of the closest divertible AV
        for (AVRequest avRequest : getAVRequests()) {
            Coord requestCoord = avRequest.getFromLink().getFromNode().getCoord();
            AVVehicle closestVehicle = null;
            double closestDistance = Double.POSITIVE_INFINITY;
            for (RoboTaxi vehicleLinkPair : getDivertableRoboTaxis()) {
                
                // Coord avCoord = vehicleLinkPair.getDivertableLocation().getFromNode().getCoord();
                GlobalAssert.that(false);
                // TODO fix this, not adapted to new API
                Coord avCoord =  null; //refPositions.get(vehicleLinkPair.getAVVehicle()).getCoord();
                
                double distance = Math.hypot(requestCoord.getX() - avCoord.getX(), requestCoord.getY() - avCoord.getY());
                if (distance < closestDistance) {
                    closestDistance = distance;
                    // TODO adapt to new API
                    GlobalAssert.that(false);
                    //closestVehicle = vehicleLinkPair.getAVVehicle();
                    closestVehicle = null;
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
    private Link computeWeberLink(List<AVRequest> avRequests, ArrayList<Double> weights, Link initialGuess) {
        if (avRequests.isEmpty()) {
            return initialGuess;
        }
        List<Coord> requestCoords = new ArrayList<>();
        for (AVRequest avRequest : avRequests) {
            requestCoords.add(avRequest.getFromLink().getCoord());
        }
        
        Coord weberCoord = weiszfeldAlgorithm(requestCoords, weights, initialGuess.getCoord());
        
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
    * @param weights
    *            list of corresponding weights / multiplicities
    * @param initialGuess
    *            initial guess as a coordinate for the iterative algorithm
    * @return 2D Weber point of the data coordinates approximated by Weiszfeld's
    *         algorithm
    */
   private Coord weiszfeldAlgorithm(List<Coord> data, ArrayList<Double> weights, Coord initialGuess) {
       if (weights.size() > 0) {
           // if using weights, we must have a weight for each data point
           GlobalAssert.that(data.size() == weights.size()); 
       }
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
           for (int idx = 0; idx < data.size(); idx++) {
               Coord point = data.get(idx);
               double weight;
               if (weights.size() > 0) {
                   GlobalAssert.that(weights.get(idx) != null);
                   weight = weights.get(idx);
               } else { // empty weights array => use unit weights
                   weight = 1.0;
               }
               double distance = Math.hypot(point.getX() - weberX, point.getY() - weberY);
               if (distance != 0) {
                   x += weight*point.getX() / distance;
                   y += weight*point.getY() / distance;
                   normalizer += weight / distance;
               } else {
                   x = point.getX();
                   y = point.getY();
                   normalizer = 1.0;
                   break;
               }
           }
           GlobalAssert.that(normalizer > 0);
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
        // use getRoboTaxis and iterate over RoboTaxis
        Set<AVVehicle> replaceMeSet = new HashSet<>();
        for (AVVehicle avVehicle : replaceMeSet) {
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
            return new SpencerSelfishOldNotWorking( //
                    config, generatorConfig, travelTime, router, eventsManager, network, abstractRequestSelector);
        }
    }
}
