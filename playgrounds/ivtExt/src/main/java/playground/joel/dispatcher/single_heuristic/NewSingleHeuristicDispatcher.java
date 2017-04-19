package playground.joel.dispatcher.single_heuristic;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;

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
import playground.clruch.dispatcher.utils.OldestRequestSelector;
import playground.clruch.dispatcher.utils.PredefinedMatchingMatcher;
import playground.clruch.utils.GlobalAssert;
import playground.clruch.utils.SafeConfig;
import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.config.AVGeneratorConfig;
import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatcher;
import playground.sebhoerl.avtaxi.framework.AVModule;
import playground.sebhoerl.avtaxi.passenger.AVRequest;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

public class NewSingleHeuristicDispatcher extends UniversalDispatcher {

    private final int dispatchPeriod;
    private final QuadTree<AVRequest> pendingRequestsTree;
    private final double[] networkBounds;
    private final HashSet<AVRequest> openRequests = new HashSet<>(); // two data structures are used
                                                                     // to enable fast "contains"
                                                                     // searching

    private NewSingleHeuristicDispatcher( //
            AVDispatcherConfig avDispatcherConfig, //
            TravelTime travelTime, //
            ParallelLeastCostPathCalculator parallelLeastCostPathCalculator, //
            EventsManager eventsManager, //
            Network network, AbstractRequestSelector abstractRequestSelector) {
        super(avDispatcherConfig, travelTime, parallelLeastCostPathCalculator, eventsManager);
        SafeConfig safeConfig = SafeConfig.wrap(avDispatcherConfig);
        dispatchPeriod = safeConfig.getInteger("dispatchPeriod", 10);
        networkBounds = NetworkUtils.getBoundingBox(network.getNodes().values());
        pendingRequestsTree = new QuadTree<>(networkBounds[0], networkBounds[1], networkBounds[2], networkBounds[3]);
    }

    @Override
    public void redispatch(double now) {
        final long round_now = Math.round(now);

        // get Map for Matcher
        HashMap<AVVehicle, Link> stayVehiclesAtLinks = new HashMap<>();
        Map<Link, Queue<AVVehicle>> stayVehicles = getStayVehicles();
        for (Link link : stayVehicles.keySet()) {
            Queue<AVVehicle> queue = stayVehicles.get(link);
            for (AVVehicle avVehicle : queue) {
                stayVehiclesAtLinks.put(avVehicle, link);
            }
        }

        // match all matched av/request pairs which are at same link
        new PredefinedMatchingMatcher(this::setAcceptRequest) //
                .matchPredefined(stayVehiclesAtLinks, getMatchings());

        if (round_now % dispatchPeriod == 0) {

            // add open requests to search tree
            addOpenRequests(getUnassignedAVRequests());
            
            if(getavailableUnassignedVehicleLinkPairs().size()>0){
                VehicleLinkPair vehicleLinkPair = getavailableUnassignedVehicleLinkPairs().get(0);
                if(getUnassignedAVRequests().size()>0){
                    
                    
                    AVRequest closestRequest = findClosestRequest(vehicleLinkPair, getUnassignedAVRequests());
                    if (closestRequest != null) {
                        sendStayVehicleCustomer(vehicleLinkPair.avVehicle, closestRequest);
                        Coord toMatchRequestCoord = closestRequest.getFromLink().getFromNode().getCoord();
                        pendingRequestsTree.remove(toMatchRequestCoord.getX(),toMatchRequestCoord.getY(), closestRequest);
                    }
                }
            }

        }

    }

    /**
     * @param vehicleLinkPair
     *            some vehicle link pair
     * @param avRequests
     *            list of currently open AVRequests
     * @return the Link with fromNode closest to vehicleLinkPair divertableLocation fromNode
     */
    private AVRequest findClosestRequest(VehicleLinkPair vehicleLinkPair, Collection<AVRequest> avRequests) {
        Coord vehicleCoord = vehicleLinkPair.getDivertableLocation().getFromNode().getCoord();
        System.out.println("treesize " + pendingRequestsTree.size());
        return pendingRequestsTree.getClosest(vehicleCoord.getX(), vehicleCoord.getY());
    }

    /**
     * @param avRequests
     *            ensures that new open requests are added to a list with all open requests
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
            return new NewSingleHeuristicDispatcher( //
                    config, travelTime, router, eventsManager, network, abstractRequestSelector);
        }
    }
}
