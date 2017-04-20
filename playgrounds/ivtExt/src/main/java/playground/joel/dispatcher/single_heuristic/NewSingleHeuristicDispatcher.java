package playground.joel.dispatcher.single_heuristic;

import java.util.*;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.collections.QuadTree;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import playground.clruch.dispatcher.core.UniversalBindingDispatcher;
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

public class NewSingleHeuristicDispatcher extends UniversalBindingDispatcher {

    private final int dispatchPeriod;
    private final double[] networkBounds;
    private final QuadTree<AVRequest> pendingRequestsTree;
    private final HashSet<AVRequest> openRequests = new HashSet<>(); // two data structures are used to enable fast "contains" searching
    private final QuadTree<AVVehicle> unassignedVehiclesTree;
    private final HashSet<AVVehicle> unassignedVehicles = new HashSet<>(); // two data structures are used to enable fast "contains" searching

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
        unassignedVehiclesTree = new QuadTree<>(networkBounds[0], networkBounds[1], networkBounds[2], networkBounds[3]);
    }

    @Override
    public void redispatch(double now) {
        final long round_now = Math.round(now);

        // get Map for Matcher
        HashMap<AVVehicle, Link> stayVehiclesAtLinks = vehicleMapper();

        // match all matched av/request pairs which are at same link
        new PredefinedMatchingMatcher(this::setAcceptRequest) //
                .matchPredefined(stayVehiclesAtLinks, getMatchings());

        if (round_now % dispatchPeriod == 0) {

            // add open requests to search tree
            addOpenRequests(getUnassignedAVRequests());
            // add unassigned vehicles to search tree
            addUnassignedVehicles(getavailableUnassignedVehicleLinkPairs());
            
            if(getavailableUnassignedVehicleLinkPairs().size()>0)
                if(getUnassignedAVRequests().size()>0) {

                    // oversupply case
                    if (getavailableUnassignedVehicleLinkPairs().size() >= getUnassignedAVRequests().size()) {
                        System.out.println("oversupply: more unassigned vehicles than unassigned requests; " +
                                "("+getavailableUnassignedVehicleLinkPairs().size()+":"+getUnassignedAVRequests().size()+")");
                        AVRequest request = getUnassignedAVRequests().iterator().next();
                        AVVehicle closestVehicle = findClosestVehicle(request, getavailableUnassignedVehicleLinkPairs());
                        if (closestVehicle != null)
                            sendAndRemove(request, closestVehicle);
                    }

                    // undersupply case
                    else {
                        System.out.println("undersupply: more unassigned requests than unassigned vehicles; " +
                                "("+getavailableUnassignedVehicleLinkPairs().size()+":"+getUnassignedAVRequests().size()+")");
                        VehicleLinkPair vehicleLinkPair = getavailableUnassignedVehicleLinkPairs().get(0);
                        AVRequest closestRequest = findClosestRequest(vehicleLinkPair, getUnassignedAVRequests());
                        if (closestRequest != null)
                            sendAndRemove(closestRequest, vehicleLinkPair.avVehicle);
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
        // System.out.println("treesize " + pendingRequestsTree.size());
        return pendingRequestsTree.getClosest(vehicleCoord.getX(), vehicleCoord.getY());
    }

    /**
     * @param avRequest
     *          some request
     * @param avVehicles
     *          list of currently unassigned VehicleLinkPairs
     * @return the AVVehicle closest to the given request
     */
    private AVVehicle findClosestVehicle(AVRequest avRequest, List<VehicleLinkPair> avVehicles) {
        Coord requestCoord = avRequest.getFromLink().getCoord();
        // System.out.println("treesize " + unassignedVehiclesTree.size());
        return unassignedVehiclesTree.getClosest(requestCoord.getX(), requestCoord.getY());
    }

    /**
     *
     * @return map containing all staying vehicles and their respective links needed for the matcher
     */
    private HashMap<AVVehicle, Link> vehicleMapper() {
        // get Map for Matcher
        HashMap<AVVehicle, Link> stayVehiclesAtLinks = new HashMap<>();
        Map<Link, Queue<AVVehicle>> stayVehicles = getStayVehicles();
        for (Link link : stayVehicles.keySet()) {
            Queue<AVVehicle> queue = stayVehicles.get(link);
            for (AVVehicle avVehicle : queue) {
                stayVehiclesAtLinks.put(avVehicle, link);
            }
        }
        return stayVehiclesAtLinks;
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
                GlobalAssert.that(orSucc && qtSucc);
            }
        }
    }

    /**
     * @param vehicleLinkPair
     *            ensures that new unassignedVehicles are added to a list with all unassigned vehicles
     */
    private void addUnassignedVehicles(List<VehicleLinkPair> vehicleLinkPair) {
        for (VehicleLinkPair avLinkPair : vehicleLinkPair) {
            if (!unassignedVehicles.contains(avLinkPair.avVehicle)) {
                Coord toMatchVehicleCoord = getVehicleLocation(avLinkPair.avVehicle).getCoord();
                boolean uaSucc = unassignedVehicles.add(avLinkPair.avVehicle);
                boolean qtSucc = unassignedVehiclesTree.put( //
                        toMatchVehicleCoord.getX(), //
                        toMatchVehicleCoord.getY(), //
                        avLinkPair.avVehicle);
                GlobalAssert.that(uaSucc && qtSucc);
            }
        }
    }

    /**
     * @param request
     *          vehicle shoul be sent here
     * @param vehicle
     *          assigned to the request
     */
    private void sendAndRemove(AVRequest request, AVVehicle vehicle) {
        sendStayVehicleCustomer(vehicle, request);
        Coord toMatchRequestCoord = request.getFromLink().getFromNode().getCoord();
        pendingRequestsTree.remove(toMatchRequestCoord.getX(), toMatchRequestCoord.getY(), request);
        Coord toMatchVehicleCoord = getVehicleLocation(vehicle).getCoord();
        unassignedVehicles.remove(vehicle);
        unassignedVehiclesTree.remove(toMatchVehicleCoord.getX(), toMatchVehicleCoord.getY(), vehicle);
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
