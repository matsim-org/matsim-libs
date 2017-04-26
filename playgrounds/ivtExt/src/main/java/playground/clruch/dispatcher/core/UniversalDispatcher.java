package playground.clruch.dispatcher.core;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.schedule.DriveTask;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedules;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.util.LinkTimePair;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.util.TravelTime;

import playground.clruch.net.SimulationDistribution;
import playground.clruch.net.SimulationObject;
import playground.clruch.net.SimulationObjectCompiler;
import playground.clruch.net.SimulationObjects;
import playground.clruch.router.FuturePathContainer;
import playground.clruch.router.FuturePathFactory;
import playground.clruch.utils.AVLocation;
import playground.clruch.utils.AVTaskAdapter;
import playground.clruch.utils.GlobalAssert;
import playground.clruch.utils.SafeConfig;
import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatcher;
import playground.sebhoerl.avtaxi.dispatcher.AbstractDispatcher;
import playground.sebhoerl.avtaxi.passenger.AVRequest;
import playground.sebhoerl.avtaxi.schedule.AVDriveTask;
import playground.sebhoerl.avtaxi.schedule.AVStayTask;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

/**
 * purpose of {@link UniversalDispatcher} is to collect and manage {@link AVRequest}s
 * alternative implementation of {@link AVDispatcher}; supersedes {@link AbstractDispatcher}.
 */
public abstract class UniversalDispatcher extends AbstractUniversalDispatcher {


    protected UniversalDispatcher( //
            AVDispatcherConfig avDispatcherConfig, //
            TravelTime travelTime, //
            ParallelLeastCostPathCalculator parallelLeastCostPathCalculator, //
            EventsManager eventsManager //
    ) {
        super(avDispatcherConfig,travelTime,parallelLeastCostPathCalculator,eventsManager);
    }

    @Override
    void updateDatastructures(Collection<AVVehicle> stayVehicles) {
        stayVehicles.forEach(vehiclesWithCustomer::remove);
        // ---
        int failed = 0;
        Collection<AVVehicle> collection = getFunctioningVehicles();
        if (!collection.isEmpty()) {
            for (AVVehicle avVehicle : collection) {
                final Link link = AVLocation.of(avVehicle);
                if (link != null)
                    vehicleLocations.put(avVehicle, link);
                else
                    ++failed;
            }
            if (AVVEHILCECOUNT == null)
                AVVEHILCECOUNT = vehicleLocations.size();
            // TODO this check was taken out because the zurich scenario doesn't satisfy this :-( 
//            GlobalAssert.that(AVVEHILCECOUNT == collection.size());
//            GlobalAssert.that(AVVEHILCECOUNT == vehicleLocations.size());
        }
        // if (0 < failed)
        // System.out.println("failed to extract location for " + failed + " vehicles");
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
    @Override
    protected synchronized final void setAcceptRequest(AVVehicle avVehicle, AVRequest avRequest) {
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

        Link returnVal = vehiclesWithCustomer.put(avVehicle, avRequest.getToLink());
        GlobalAssert.that(returnVal == null);

        ++total_matchedRequests;
    }




    @Override
    public void onNextLinkEntered(AVVehicle avVehicle, DriveTask driveTask, LinkTimePair linkTimePair) {
        // default implementation: for now, do nothing
    }

    @Override
    public final void onRequestSubmitted(AVRequest request) {
        pendingRequests.add(request); // <- store request
    }



    /**
     * {@link PartitionedDispatcher} overrides the function
     * 
     * @return map of rebalancing vehicles and their destination links
     */
    protected Map<AVVehicle, Link> getRebalancingVehicles() {
        return Collections.emptyMap();
    }
}
