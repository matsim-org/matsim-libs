package playground.clruch.dispatcher.core;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.schedule.*;
import org.matsim.contrib.dvrp.util.LinkTimePair;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.util.TravelTime;

import playground.clruch.dispatcher.AVTaskAdapter;
import playground.clruch.router.FuturePathContainer;
import playground.clruch.router.FuturePathFactory;
import playground.clruch.utils.GlobalAssert;
import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatcher;
import playground.sebhoerl.avtaxi.dispatcher.AbstractDispatcher;
import playground.sebhoerl.avtaxi.passenger.AVRequest;
import playground.sebhoerl.avtaxi.schedule.AVDriveTask;
import playground.sebhoerl.avtaxi.schedule.AVStayTask;
import playground.sebhoerl.avtaxi.schedule.AVTask;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

/**
 * purpose of {@link UniversalDispatcher} is to collect and manage {@link AVRequest}s
 * alternative implementation of {@link AVDispatcher}; supersedes {@link AbstractDispatcher}.
 */
public abstract class UniversalDispatcher extends VehicleMaintainer {
    private final FuturePathFactory futurePathFactory;

    private final Set<AVRequest> pendingRequests = new LinkedHashSet<>(); // access via getAVRequests()
    private final Set<AVRequest> matchedRequests = new HashSet<>(); // for data integrity, private!

    private final double pickupDurationPerStop;
    private final double dropoffDurationPerStop;

    protected UniversalDispatcher( //
            AVDispatcherConfig avDispatcherConfig, //
            TravelTime travelTime, //
            ParallelLeastCostPathCalculator parallelLeastCostPathCalculator, //
            EventsManager eventsManager //
    ) {
        super(eventsManager);
        this.futurePathFactory = new FuturePathFactory(parallelLeastCostPathCalculator, travelTime);

        pickupDurationPerStop = avDispatcherConfig.getParent().getTimingParameters().getPickupDurationPerStop();
        dropoffDurationPerStop = avDispatcherConfig.getParent().getTimingParameters().getDropoffDurationPerStop();
    }

    /**
     * function call leaves the state of the {@link UniversalDispatcher} unchanged.
     * successive calls to the function return the identical collection.
     * 
     * @return collection of all requests that have not been matched
     */
    protected synchronized final Collection<AVRequest> getAVRequests() {
        pendingRequests.removeAll(matchedRequests);
        matchedRequests.clear();
        return Collections.unmodifiableCollection(pendingRequests);
    }

    /**
     * function call leaves the state of the {@link UniversalDispatcher} unchanged.
     * successive calls to the function return the identical collection.
     * 
     * @return list of {@link AVRequest}s grouped by link
     */
    public final Map<Link, List<AVRequest>> getAVRequestsAtLinks() {
        return getAVRequests().stream() // <- intentionally not parallel to guarantee ordering of requests
                .collect(Collectors.groupingBy(AVRequest::getFromLink));
    }

    /**
     * Function called from derived class to match a vehicle with a request.
     * The function appends the pick-up, drive, and drop-off tasks for the car.
     * 
     * @param avVehicle
     *            vehicle in {@link AVStayTask} in order to match the request
     * @param avRequest
     *            provided by getAVRequests()
     */
    // TODO should be 'protected'
    public synchronized final Void setAcceptRequest(AVVehicle avVehicle, AVRequest avRequest) {
        GlobalAssert.that(pendingRequests.contains(avRequest)); // request is known to the system

        boolean status = matchedRequests.add(avRequest);
        GlobalAssert.that(status); // matchedRequests did not already contain avRequest

        final Schedule schedule = avVehicle.getSchedule();
        GlobalAssert.that(schedule.getCurrentTask() == Schedules.getLastTask(schedule)); // check that current task is last task in schedule

        final double endPickupTime = getTimeNow() + pickupDurationPerStop;
        FuturePathContainer futurePathContainer = futurePathFactory.createFuturePathContainer( //
                avRequest.getFromLink(), avRequest.getToLink(), endPickupTime);

        assignDirective(avVehicle, new AcceptRequestDirective( //
                avVehicle, avRequest, futurePathContainer, getTimeNow(), dropoffDurationPerStop));
        return null;
    }

    /**
     * assigns new destination to vehicle.
     * if vehicle is already located at destination, nothing happens.
     * 
     * in one pass of redispatch(...), the function setVehicleDiversion(...)
     * may only be invoked once for a single vehicle (specified in vehicleLinkPair). 
     *
     * @param vehicleLinkPair
     *            is provided from super.getDivertableVehicles()
     * @param destination
     */
    protected final void setVehicleDiversion(final VehicleLinkPair vehicleLinkPair, final Link destination) {
        final Schedule schedule = vehicleLinkPair.avVehicle.getSchedule();
        Task abstractTask = schedule.getCurrentTask(); // <- implies that task is started
        new AVTaskAdapter(abstractTask) {
            @Override
            public void handle(AVDriveTask avDriveTask) {
                if (!avDriveTask.getPath().getToLink().equals(destination)) { // ignore when vehicle is already going there
                    FuturePathContainer futurePathContainer = futurePathFactory.createFuturePathContainer( //
                            vehicleLinkPair.linkTimePair.link, destination, vehicleLinkPair.linkTimePair.time);

                    assignDirective(vehicleLinkPair.avVehicle, new DriveVehicleDiversionDirective( //
                            vehicleLinkPair, destination, futurePathContainer));
                } else
                    assignDirective(vehicleLinkPair.avVehicle, new EmptyDirective());
            }

            @Override
            public void handle(AVStayTask avStayTask) {
                if (!avStayTask.getLink().equals(destination)) { // ignore request where location == target
                    FuturePathContainer futurePathContainer = futurePathFactory.createFuturePathContainer( //
                            vehicleLinkPair.linkTimePair.link, destination, vehicleLinkPair.linkTimePair.time);

                    assignDirective(vehicleLinkPair.avVehicle, new StayVehicleDiversionDirective( //
                            vehicleLinkPair, destination, futurePathContainer));
                } else
                    assignDirective(vehicleLinkPair.avVehicle, new EmptyDirective());
            }
        };
    }

    protected final void setVehicleDiversion(final Entry<VehicleLinkPair, Link> entry) {
        setVehicleDiversion(entry.getKey(), entry.getValue());
    }

    @Override
    public void onNextLinkEntered(AVVehicle avVehicle, DriveTask driveTask, LinkTimePair linkTimePair) {
        // default implementation: for now, do nothing
    }

    @Override
    public final void onRequestSubmitted(AVRequest request) {
        pendingRequests.add(request); // <- store request
    }

    // TODO this will not be necessary!!!
    @Override
    public final void onNextTaskStarted(AVTask task) {
        // intentionally empty
    }

    /**
     * @return debug information
     */
    public final String getUniversalDispatcherStatusString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("#requests " + getAVRequests().size());
        stringBuilder.append(", #stay " + //
                getStayVehicles().values().stream().flatMap(Queue::stream).count());
        stringBuilder.append(", #divert " + getDivertableVehicles().size()); // TODO not efficient, only debug
        return stringBuilder.toString();
    }

}
