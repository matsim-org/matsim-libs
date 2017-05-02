package playground.clruch.dispatcher.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedules;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.util.LinkTimePair;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.util.TravelTime;

import playground.clruch.router.FuturePathContainer;
import playground.clruch.utils.AVTaskAdapter;
import playground.clruch.utils.GlobalAssert;
import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatcher;
import playground.sebhoerl.avtaxi.dispatcher.AbstractDispatcher;
import playground.sebhoerl.avtaxi.passenger.AVRequest;
import playground.sebhoerl.avtaxi.schedule.AVDriveTask;
import playground.sebhoerl.avtaxi.schedule.AVStayTask;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

/**
 * purpose of {@link UniversalDispatcher} is to collect and manage {@link AVRequest}s alternative implementation of {@link AVDispatcher}; supersedes
 * {@link AbstractDispatcher}.
 */
public abstract class UniversalBindingDispatcher extends AbstractUniversalDispatcher {

    private final Set<AVRequest> assignedRequests = new HashSet<>(); // pending requests which are assigned to an AV
    private final Set<AVRequest> unassignedRequests = new HashSet<>(); // pending requests which are still unassigned
    private final Set<AVVehicle> assignedVehicles = new HashSet<>(); // vehicles which are currently assigned to a request
    private final HashMap<AVRequest, AVVehicle> matchings = new HashMap<>(); // history of customer/AV matchings

    protected UniversalBindingDispatcher( //
            AVDispatcherConfig avDispatcherConfig, //
            TravelTime travelTime, //
            ParallelLeastCostPathCalculator parallelLeastCostPathCalculator, //
            EventsManager eventsManager //
    ) {
        super(avDispatcherConfig, travelTime, parallelLeastCostPathCalculator, eventsManager);
    }

    /**
     * @return AVRequests which are currently not assigned to a vehicle
     */
    protected synchronized final Collection<AVRequest> getUnassignedAVRequests() {
        GlobalAssert.that(pendingRequests.size() == assignedRequests.size() + unassignedRequests.size());
        return Collections.unmodifiableCollection(unassignedRequests);
    }

    /**
     * Function called from derived class to match a vehicle with a request. The function appends the pick-up, drive, and drop-off tasks for the car.
     * 
     * @param avVehicle
     *            vehicle in {@link AVStayTask} in order to match the request
     * @param avRequest
     *            provided by getAVRequests()
     */
    @Override
    protected synchronized final void setAcceptRequest(AVVehicle avVehicle, AVRequest avRequest) {
        GlobalAssert.that(pendingRequests.contains(avRequest)); // request is known to the system

        boolean status = matchedRequests.add(avRequest);
        GlobalAssert.that(status); // matchedRequests did not already contain avRequest

        final Schedule schedule = avVehicle.getSchedule();

        // check that current task is last task in schedule
        GlobalAssert.that(schedule.getCurrentTask() == Schedules.getLastTask(schedule));

        final double endPickupTime = getTimeNow() + pickupDurationPerStop;
        FuturePathContainer futurePathContainer = futurePathFactory.createFuturePathContainer( //
                avRequest.getFromLink(), avRequest.getToLink(), endPickupTime);

        assignDirective(avVehicle, new AcceptRequestDirective( //
                avVehicle, avRequest, futurePathContainer, getTimeNow(), dropoffDurationPerStop));

        Link returnVal = vehiclesWithCustomer.put(avVehicle, avRequest.getToLink());
        GlobalAssert.that(returnVal == null);

        // this is only functional if the Set "assignedVehicles" was used (e.g. by
        // SingleHeuristicDispatcher)
        boolean succPR = pendingRequests.remove(avRequest);
        boolean succAR = assignedRequests.remove(avRequest);
        boolean succAV = assignedVehicles.remove(avVehicle);
        GlobalAssert.that(succAV == succAR && succAR == succPR);
        GlobalAssert.that(succPR);
        GlobalAssert.that(matchings.remove(avRequest) != null);
        GlobalAssert.that(pendingRequests.size() == unassignedRequests.size() + assignedRequests.size());
    }

    /**
     * Sends a stay vehicle to a customer in a way such that it can be made unavailalbe for future dispatcher calls
     * 
     * @param avVehicle
     * @param avRequest
     */
    protected final void sendStayVehicleCustomer(AVVehicle avVehicle, AVRequest avRequest) {

        final Schedule schedule = avVehicle.getSchedule();
        Task task = schedule.getCurrentTask();

        Link customerOrig = avRequest.getFromLink();
        Link customerDest = avRequest.getToLink();

        new AVTaskAdapter(task) {
            @Override
            public void handle(AVDriveTask avDriveTask) {
                GlobalAssert.that(false);
            }

            @Override
            public void handle(AVStayTask avStayTask) {
                LinkTimePair linkTimePair = new LinkTimePair(avStayTask.getLink(), getTimeNow());
                VehicleLinkPair vehicleLinkPair = new VehicleLinkPair(avVehicle, linkTimePair, null);

                // let AV drive to request location (setVehicleDiversion)
                if (!avStayTask.getLink().equals(customerOrig)) { // ignore request where location
                                                                  // ==
                    // target
                    FuturePathContainer futurePathContainer = futurePathFactory.createFuturePathContainer( //
                            vehicleLinkPair.linkTimePair.link, customerOrig, vehicleLinkPair.linkTimePair.time);

                    assignDirective(vehicleLinkPair.avVehicle, new StayVehicleDiversionDirective( //
                            vehicleLinkPair, customerOrig, futurePathContainer));
                } else
                    assignDirective(vehicleLinkPair.avVehicle, new EmptyDirective());

                // move request from unassigned to assigned
                boolean succReqU = unassignedRequests.remove(avRequest);
                boolean succReqA = assignedRequests.add(avRequest);
                GlobalAssert.that(succReqU == succReqA);
                GlobalAssert.that(succReqA);

                // declare vehicle as assigned
                boolean succVeh = assignedVehicles.add(avVehicle);
                GlobalAssert.that(succVeh);

                // add pair to matchings
                GlobalAssert.that(matchings.put(avRequest, avVehicle) == null);

                GlobalAssert.that(pendingRequests.size() == unassignedRequests.size() + assignedRequests.size());

            }

        };

        ++total_matchedRequests;
    }

    /**
     * 
     * @return available vehicles which are yet unassigned to a request as vehicle Link pairs
     */
    protected List<VehicleLinkPair> getavailableUnassignedVehicleLinkPairs() {
        // get the staying vehicles and requests
        List<AVVehicle> availableVehicles = getStayVehicles().values().stream().flatMap(Queue::stream).collect(Collectors.toList());
        List<AVVehicle> availableUnassignedVehicles = availableVehicles.stream().filter(v -> !assignedVehicles.contains(v))
                .collect(Collectors.toList());
        List<VehicleLinkPair> returnList = new ArrayList<>();

        for (AVVehicle avVehicle : availableUnassignedVehicles) {
            final Schedule schedule = avVehicle.getSchedule();
            AVStayTask task = (AVStayTask) schedule.getCurrentTask();
            LinkTimePair linkTimePair = new LinkTimePair(task.getLink(), getTimeNow());
            VehicleLinkPair vehicleLinkPair = new VehicleLinkPair(avVehicle, linkTimePair, null);
            returnList.add(vehicleLinkPair);
        }

        return returnList;
    }

    @Override
    public final void onRequestSubmitted(AVRequest request) {
        boolean succAddP = pendingRequests.add(request); // <- store request
        boolean succAddU = unassignedRequests.add(request);
        GlobalAssert.that(succAddP == succAddU);
        GlobalAssert.that(pendingRequests.size() == unassignedRequests.size() + assignedRequests.size());
    }

    /**
     * 
     * @return matchings between AVRequests and AVVehicles
     */
    protected HashMap<AVRequest, AVVehicle> getMatchings() {
        return matchings;
    }

}
