package playground.clruch.dispatcher;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.collections.QuadTree;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import playground.clruch.dispatcher.core.UniversalDispatcher;
import playground.clruch.dispatcher.core.RoboTaxi;
import playground.clruch.dispatcher.utils.AbstractRequestSelector;
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

public class NewSingleHeuristicDispatcher extends UniversalDispatcher {

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
            // TODO use the normal call functions and implement in UniveralDispatcher a setVehiclePickupBinding function instead
            List<RoboTaxi> vehicles = getDivertableUnassignedVehicleLinkPairs();
            addUnassignedVehicles(getDivertableUnassignedVehicleLinkPairs());

            List<AVRequest> requests = getUnassignedAVRequests();
            addOpenRequests(requests);

            boolean oversupply = false;
            if (unassignedVehicles.size() >= requests.size())
                oversupply = true;

            boolean canMatch = (unassignedVehicles.size() > 0 && requests.size() > 0);

            if (oversupply && canMatch) { // OVERSUPPLY CASE
                for (AVRequest avr : requests) {
                    AVVehicle cloestVeh = findClosestVehicle(avr);
                    if (cloestVeh != null) {
                        setVehiclePickup(cloestVeh, avr);
                        removeFromTrees(cloestVeh, avr);
                    }
                }
            }
            if (!oversupply && canMatch) { // UNDERSUPPLY CASE
                for (RoboTaxi vlp : vehicles) {

                    AVRequest closestReq = findClosestRequest(vlp);
                    if (closestReq != null) {
                        setVehiclePickup(vlp.getAVVehicle(), closestReq);
                        removeFromTrees(vlp.getAVVehicle(), closestReq);
                    }
                }
            }
        }
    }

    private boolean removeFromTrees(AVVehicle avVehicle, AVRequest avRequest) {
        // remove avRequest
        boolean succRM = openRequests.remove(avRequest);
        boolean succRT = pendingRequestsTree.remove(avRequest.getFromLink().getFromNode().getCoord().getX(),
                avRequest.getFromLink().getFromNode().getCoord().getY(), avRequest);
        boolean removeSuccessR = succRT && succRM;
        GlobalAssert.that(removeSuccessR);

        // remove avVehicle
        boolean succVM = unassignedVehicles.remove(avVehicle);
        boolean succVT = unassignedVehiclesTree.remove(getVehicleLocation(avVehicle).getCoord().getX(),
                getVehicleLocation(avVehicle).getCoord().getY(), avVehicle);
        boolean removeSuccessV = succVT && succVM;
        GlobalAssert.that(removeSuccessV);

        return removeSuccessR && removeSuccessV;
    }

    /**
     * @param avRequest
     *            some request
     * @param avVehicles
     *            list of currently unassigned VehicleLinkPairs
     * @return the AVVehicle closest to the given request
     */
    private AVVehicle findClosestVehicle(AVRequest avRequest) {
        Coord requestCoord = avRequest.getFromLink().getCoord();
        // System.out.println("treesize " + unassignedVehiclesTree.size());
        return unassignedVehiclesTree.getClosest(requestCoord.getX(), requestCoord.getY());
    }

    /**
     * @param vehicleLinkPair
     *            ensures that new unassignedVehicles are added to a list with all unassigned vehicles
     */
    private void addUnassignedVehicles(List<RoboTaxi> vehicleLinkPair) {
        for (RoboTaxi avLinkPair : vehicleLinkPair) {
            if (!unassignedVehicles.contains(avLinkPair.getAVVehicle())) {
                Coord toMatchVehicleCoord = getVehicleLocation(avLinkPair.getAVVehicle()).getCoord();
                boolean uaSucc = unassignedVehicles.add(avLinkPair.getAVVehicle());
                boolean qtSucc = unassignedVehiclesTree.put( //
                        toMatchVehicleCoord.getX(), //
                        toMatchVehicleCoord.getY(), //
                        avLinkPair.getAVVehicle());
                GlobalAssert.that(uaSucc && qtSucc);
            }
        }
    }

    /**
     * 
     * @param vehicleLinkPair
     * @param avRequests
     *            (open requests)
     * @return the closest Request to vehicleLinkPair.avVehicle found with tree-search
     */
    private AVRequest findClosestRequest(RoboTaxi vehicleLinkPair) {
        Coord vehicleCoord = vehicleLinkPair.getDivertableLocation().getFromNode().getCoord();
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
                GlobalAssert.that(orSucc && qtSucc);
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