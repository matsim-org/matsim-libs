package playground.clruch.dispatcher;

import java.util.*;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordUtils;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import playground.clruch.dispatcher.core.DispatcherUtils;
import playground.clruch.dispatcher.core.UniversalDispatcher;
import playground.clruch.dispatcher.core.BindingUniversalDispatcher;
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

public class NewSingleHeuristicDispatcher extends BindingUniversalDispatcher {

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
        dispatchPeriod = getDispatchPeriod(safeConfig, 10); // safeConfig.getInteger("dispatchPeriod", 10);
        networkBounds = NetworkUtils.getBoundingBox(network.getNodes().values());
        pendingRequestsTree = new QuadTree<>(networkBounds[0], networkBounds[1], networkBounds[2], networkBounds[3]);
        unassignedVehiclesTree = new QuadTree<>(networkBounds[0], networkBounds[1], networkBounds[2], networkBounds[3]);
    }

    @Override
    public void redispatch(double now) {
        final long round_now = Math.round(now);

        if (round_now % dispatchPeriod == 0) {
            
            // get open requests and available vehicles
            List<VehicleLinkPair> vehicles = getDivertableUnassignedVehicleLinkPairs();
            List<AVRequest> requests = getUnassignedAVRequests();

            boolean oversupply = false;
            if (vehicles.size() >= requests.size())
                oversupply = true;

            boolean canMatch = (vehicles.size() > 0 && requests.size() > 0);

            if (oversupply && canMatch) { // OVERSUPPLY CASE
                for (AVRequest avr : requests) {
                    VehicleLinkPair closestVeh = getClosestVehicle(vehicles, avr);
                    if (closestVeh != null) {
                        setVehiclePickup(closestVeh.avVehicle, avr);
                        vehicles.remove(closestVeh);
                    }
                }
            }
            if (!oversupply && canMatch) { // UNDERSUPPLY CASE
                for (VehicleLinkPair vlp : vehicles) {
                    
                    AVRequest closestReq = getClosestRequest(requests, vlp);
                    if (closestReq != null) {
                        setVehiclePickup(vlp.avVehicle, closestReq);
                        requests.remove(closestReq);
                    }
                }
            }
        }

    }

    private VehicleLinkPair getClosestVehicle(List<VehicleLinkPair> vehicles, AVRequest avRequest) {
        VehicleLinkPair closestVehicle = null;
        double closestDistance = Double.MAX_VALUE;
        for (VehicleLinkPair vehicleLinkPair : vehicles) {
            double dist = CoordUtils.calcEuclideanDistance(avRequest.getFromLink().getCoord(), vehicleLinkPair.getDivertableLocation().getCoord());
            if (dist < closestDistance) {
                closestVehicle = vehicleLinkPair;
                closestDistance = dist;
            }
        }
        return closestVehicle;
    }

    private AVRequest getClosestRequest(List<AVRequest> requests, VehicleLinkPair vehicleLinkPair) {
        AVRequest closestRequest = null;
        double closestDistance = Double.MAX_VALUE;
        for (AVRequest avRequest : requests) {
            double dist = CoordUtils.calcEuclideanDistance(avRequest.getFromLink().getCoord(), vehicleLinkPair.getDivertableLocation().getCoord());
            if (dist < closestDistance) {
                closestRequest = avRequest;
                closestDistance = dist;
            }
        }
        return closestRequest;
    }

    // // ========================== OLD FCTNS. =========================================

    /**
     * 
     * @param vehicleLinkPair
     * @param avRequests
     *            (open requests)
     * @return the closest Request to vehicleLinkPair.avVehicle found with tree-search
     */
    private AVRequest findClosestRequest(VehicleLinkPair vehicleLinkPair) {
        Coord vehicleCoord = vehicleLinkPair.getDivertableLocation().getFromNode().getCoord();
        return pendingRequestsTree.getClosest(vehicleCoord.getX(), vehicleCoord.getY());
    }

    //
    // /**
    // * @param avRequest
    // * some request
    // * @param avVehicles
    // * list of currently unassigned VehicleLinkPairs
    // * @return the AVVehicle closest to the given request
    // */
    // private AVVehicle findClosestVehicle(AVRequest avRequest, List<VehicleLinkPair> avVehicles) {
    // Coord requestCoord = avRequest.getFromLink().getCoord();
    // // System.out.println("treesize " + unassignedVehiclesTree.size());
    // return unassignedVehiclesTree.getClosest(requestCoord.getX(), requestCoord.getY());
    // }
    //
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
    //
    // /**
    // * @param vehicleLinkPair
    // * ensures that new unassignedVehicles are added to a list with all unassigned vehicles
    // */
    // private void addUnassignedVehicles(List<VehicleLinkPair> vehicleLinkPair) {
    // for (VehicleLinkPair avLinkPair : vehicleLinkPair) {
    // if (!unassignedVehicles.contains(avLinkPair.avVehicle)) {
    // Coord toMatchVehicleCoord = getVehicleLocation(avLinkPair.avVehicle).getCoord();
    // boolean uaSucc = unassignedVehicles.add(avLinkPair.avVehicle);
    // boolean qtSucc = unassignedVehiclesTree.put( //
    // toMatchVehicleCoord.getX(), //
    // toMatchVehicleCoord.getY(), //
    // avLinkPair.avVehicle);
    // GlobalAssert.that(uaSucc && qtSucc);
    // }
    // }
    // }

    // /**
    // * @param request
    // * vehicle shoul be sent here
    // * @param vehicle
    // * assigned to the request
    // */
    // private void sendAndRemove(AVRequest request, AVVehicle vehicle) {
    // sendStayVehicleCustomer(vehicle, request);
    // Coord toMatchRequestCoord = request.getFromLink().getFromNode().getCoord();
    // pendingRequestsTree.remove(toMatchRequestCoord.getX(), toMatchRequestCoord.getY(), request);
    // Coord toMatchVehicleCoord = getVehicleLocation(vehicle).getCoord();
    // unassignedVehicles.remove(vehicle);
    // unassignedVehiclesTree.remove(toMatchVehicleCoord.getX(), toMatchVehicleCoord.getY(), vehicle);
    // }

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

// // add open requests to search tree
// addOpenRequests(getUnassignedAVRequests());
// // add unassigned vehicles to search tree
// addUnassignedVehicles(getDivertableUnassignedVehicleLinkPairs());
//
// if (getDivertableUnassignedVehicleLinkPairs().size() > 0 && getUnassignedAVRequests().size() > 0){
//
// }
//
// while (!(getDivertableUnassignedVehicleLinkPairs().size() == 0 || getUnassignedAVRequests().size() == 0)) {
//
// // oversupply case
// if (getDivertableUnassignedVehicleLinkPairs().size() >= getUnassignedAVRequests().size()) {
// // System.out.println("oversupply: more unassigned vehicles than unassigned requests; " +
// // "(" + getavailableUnassignedVehicleLinkPairs().size() + ":" + getUnassignedAVRequests().size() + ")");
// AVRequest request = getUnassignedAVRequests().iterator().next();
// AVVehicle closestVehicle = findClosestVehicle(request, getDivertableUnassignedVehicleLinkPairs());
// if (closestVehicle != null)
// sendAndRemove(request, closestVehicle);
// }
//
// // undersupply case
// else {
// // System.out.println("undersupply: more unassigned requests than unassigned vehicles; " +
// // "(" + getavailableUnassignedVehicleLinkPairs().size() + ":" + getUnassignedAVRequests().size() + ")");
// VehicleLinkPair vehicleLinkPair = getDivertableUnassignedVehicleLinkPairs().get(0);
// AVRequest closestRequest = findClosestRequest(vehicleLinkPair, getUnassignedAVRequests());
// if (closestRequest != null)
// sendAndRemove(closestRequest, vehicleLinkPair.avVehicle);
// }
// }
