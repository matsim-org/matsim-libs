package playground.sebhoerl.avtaxi.dispatcher.single_heuristic;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.collections.QuadTree;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.config.AVGeneratorConfig;
import playground.sebhoerl.avtaxi.data.AVOperator;
import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatcher;
import playground.sebhoerl.avtaxi.dispatcher.AbstractDispatcher;
import playground.sebhoerl.avtaxi.dispatcher.utils.SingleRideAppender;
import playground.sebhoerl.avtaxi.framework.AVModule;
import playground.sebhoerl.avtaxi.passenger.AVRequest;
import playground.sebhoerl.avtaxi.schedule.AVStayTask;
import playground.sebhoerl.avtaxi.schedule.AVTask;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

public class SingleHeuristicDispatcher extends AbstractDispatcher {
    final private Id<AVOperator> operatorId;

    final private List<AVVehicle> availableVehicles = new LinkedList<>();
    final private List<AVRequest> pendingRequests = new LinkedList<>();

    /** data structure to find closest vehicles */
    final private QuadTree<AVVehicle> availableVehiclesTree;
    /** data structure to find closest requests/customers */
    final private QuadTree<AVRequest> pendingRequestsTree;

    final private Map<AVVehicle, Link> vehicleLinks = new HashMap<>();
    final private Map<AVRequest, Link> requestLinks = new HashMap<>();

    private SimpleDispatcherHeuristicMode mode = SimpleDispatcherHeuristicMode.OVERSUPPLY;

    public SingleHeuristicDispatcher(Id<AVOperator> operatorId, EventsManager eventsManager, Network network, SingleRideAppender appender) {
	super(eventsManager, appender);
        this.operatorId = operatorId;

        double[] bounds = NetworkUtils.getBoundingBox(network.getNodes().values()); // minx, miny, maxx, maxy

        availableVehiclesTree = new QuadTree<>(bounds[0], bounds[1], bounds[2], bounds[3]);
        pendingRequestsTree = new QuadTree<>(bounds[0], bounds[1], bounds[2], bounds[3]);
    }
    
    @Override
    public void onRequestSubmitted(AVRequest request) {
        addRequest(request, request.getFromLink());
    }
    
    private void addRequest(AVRequest request, Link link) { // function only called from one place
        pendingRequests.add(request);
        pendingRequestsTree.put(link.getCoord().getX(), link.getCoord().getY(), request);
        requestLinks.put(request, link);
    }

    @Override
    public void onNextTaskStarted(AVTask task) {
        if (task.getAVTaskType() == AVTask.AVTaskType.STAY) {
            private_addVehicle((AVVehicle) task.getSchedule().getVehicle(), ((AVStayTask) task).getLink());
        }
    }

    private void reoptimize(double now) {
        while (pendingRequests.size() > 0 && availableVehicles.size() > 0) {
            //System.out.println("single heuristic dispatcher is now reoptimizing. Pending requests.size(): " + pendingRequests.size() + "  availableVehicles.size()" + availableVehicles.size());
            AVRequest request = null;
            AVVehicle vehicle = null;

            switch (mode) {
                case OVERSUPPLY:
                    request = findRequest();
                    vehicle = findClosestVehicle(request.getFromLink());
                    break;
                case UNDERSUPPLY:
                    vehicle = findVehicle();
                    request = findClosestRequest(vehicleLinks.get(vehicle));
                    break;
            }

            removeRequest(request);
            private_removeVehicle(vehicle);

            appender.schedule(request, vehicle, now);
        }

        SimpleDispatcherHeuristicMode updatedMode = availableVehicles.size() > 0 ? SimpleDispatcherHeuristicMode.OVERSUPPLY : SimpleDispatcherHeuristicMode.UNDERSUPPLY;
        if (!updatedMode.equals(mode)) {
            mode = updatedMode;
            eventsManager.processEvent(new ModeChangeEvent(mode, operatorId, now));
        }
    }

    @Override
    public void onNextTimestep(double now) {
        appender.update();
        reoptimize(now);
    }

    private AVRequest findRequest() {
        return pendingRequests.get(0);
    }

    private AVVehicle findVehicle() {
        return availableVehicles.get(0);
    }

    private AVVehicle findClosestVehicle(Link link) {
        Coord coord = link.getCoord();
        return availableVehiclesTree.getClosest(coord.getX(), coord.getY());
    }

    private AVRequest findClosestRequest(Link link) {
        Coord coord = link.getCoord();
        return pendingRequestsTree.getClosest(coord.getX(), coord.getY());
    }

    @Override
    public void protected_registerVehicle(AVVehicle vehicle) {
        private_addVehicle(vehicle, vehicle.getStartLink());
    }

    private void private_addVehicle(AVVehicle vehicle, Link link) {
        availableVehicles.add(vehicle);
        // FIXME the coordinates of the car in the tree are never updated (only removed)
        availableVehiclesTree.put(link.getCoord().getX(), link.getCoord().getY(), vehicle);
        vehicleLinks.put(vehicle, link);
    }

    private void private_removeVehicle(AVVehicle vehicle) {
        availableVehicles.remove(vehicle);
        Coord coord = vehicleLinks.remove(vehicle).getCoord();
        availableVehiclesTree.remove(coord.getX(), coord.getY(), vehicle);
    }

    private void removeRequest(AVRequest request) {
        pendingRequests.remove(request);
        Coord coord = requestLinks.remove(request).getCoord();
        pendingRequestsTree.remove(coord.getX(), coord.getY(), request);
    }

    static public class Factory implements AVDispatcherFactory {
        @Inject private Network network;
        @Inject private EventsManager eventsManager;

        @Inject @Named(AVModule.AV_MODE)
        private TravelTime travelTime;

        @Inject @Named(AVModule.AV_MODE)
        private ParallelLeastCostPathCalculator router;

        @Override
        public AVDispatcher createDispatcher(AVDispatcherConfig config, AVGeneratorConfig generatorConfig) {
            return new SingleHeuristicDispatcher(
                    config.getParent().getId(),
                    eventsManager,
                    network,
                    new SingleRideAppender(config, router, travelTime)
            );
        }
    }
}
