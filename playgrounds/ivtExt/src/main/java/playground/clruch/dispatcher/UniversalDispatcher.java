package playground.clruch.dispatcher;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.schedule.AbstractTask;
import org.matsim.contrib.dvrp.schedule.DriveTask;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedules;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.tracker.OnlineDriveTaskTracker;
import org.matsim.contrib.dvrp.tracker.OnlineDriveTaskTrackerImpl;
import org.matsim.contrib.dvrp.tracker.TaskTracker;
import org.matsim.contrib.dvrp.util.LinkTimePair;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.util.TravelTime;

import playground.clruch.router.FuturePathContainer;
import playground.clruch.router.FuturePathFactory;
import playground.clruch.router.SimpleBlockingRouter;
import playground.clruch.utils.GlobalAssert;
import playground.clruch.utils.VrpPathUtils;
import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.config.AVTimingParameters;
import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatcher;
import playground.sebhoerl.avtaxi.dispatcher.AbstractDispatcher;
import playground.sebhoerl.avtaxi.passenger.AVRequest;
import playground.sebhoerl.avtaxi.schedule.AVDriveTask;
import playground.sebhoerl.avtaxi.schedule.AVDropoffTask;
import playground.sebhoerl.avtaxi.schedule.AVPickupTask;
import playground.sebhoerl.avtaxi.schedule.AVStayTask;
import playground.sebhoerl.avtaxi.schedule.AVTask;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

/**
 * purpose of {@link UniversalDispatcher} is to collect and manage {@link AVRequest}s
 * alternative implementation of {@link AVDispatcher}; supersedes {@link AbstractDispatcher}.
 */
public abstract class UniversalDispatcher extends VehicleMaintainer {
    // TODO at least remove the use of this variable inside the core functions!
    @Deprecated
    protected final AVDispatcherConfig avDispatcherConfig; // so far used only for timing parameters
    private final FuturePathFactory futurePathFactory;
    @Deprecated
    protected final TravelTime travelTime;
    @Deprecated
    protected final ParallelLeastCostPathCalculator parallelLeastCostPathCalculator;

    private final Set<AVRequest> pendingRequests = new HashSet<>(); // access via getAVRequests()
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
        this.avDispatcherConfig = avDispatcherConfig;
        this.futurePathFactory = new FuturePathFactory(parallelLeastCostPathCalculator, travelTime);
        this.travelTime = travelTime;
        this.parallelLeastCostPathCalculator = parallelLeastCostPathCalculator;

        // TODO not used yet
        pickupDurationPerStop = avDispatcherConfig.getParent().getTimingParameters().getPickupDurationPerStop();
        dropoffDurationPerStop = avDispatcherConfig.getParent().getTimingParameters().getDropoffDurationPerStop();
    }

    /**
     * function call leaves the state of the {@link UniversalDispatcher} unchanged.
     * successive calls to the function return the identical collection.
     * 
     * @return collection of all requests that have not been matched
     */
    protected final Collection<AVRequest> getAVRequests() {
        pendingRequests.removeAll(matchedRequests);
        matchedRequests.clear();
        return Collections.unmodifiableCollection(pendingRequests);
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
    protected final void setAcceptRequest(AVVehicle avVehicle, AVRequest avRequest) {
        boolean status = matchedRequests.add(avRequest);
        GlobalAssert.that(status); // matchedRequests did not already contain avRequest
        GlobalAssert.that(pendingRequests.contains(avRequest));
       
        final AVTimingParameters timing = avDispatcherConfig.getParent().getTimingParameters();
        final Schedule<AbstractTask> schedule = (Schedule<AbstractTask>) avVehicle.getSchedule();

        {
            AbstractTask currentTask = schedule.getCurrentTask();
            AbstractTask lastTask = Schedules.getLastTask(schedule);
            GlobalAssert.that(currentTask == lastTask); // check that current task is last task in schedule
        }

        final double endPickupTime = getTimeNow() + timing.getPickupDurationPerStop();
        final SimpleBlockingRouter simpleBlockingRouter = new SimpleBlockingRouter(parallelLeastCostPathCalculator, travelTime);
        VrpPathWithTravelData vrpPathWithTravelData = simpleBlockingRouter.getRoute( //
                avRequest.getFromLink(), avRequest.getToLink(), endPickupTime);

        final double scheduleEndTime = schedule.getEndTime();
        {
            AVStayTask avStayTask = (AVStayTask) Schedules.getLastTask(schedule);
            avStayTask.setEndTime(getTimeNow()); // finish the last task now
        }

        schedule.addTask(new AVPickupTask( //
                getTimeNow(), endPickupTime, avRequest.getFromLink(), Arrays.asList(avRequest)));

        schedule.addTask(new AVDriveTask( //
                vrpPathWithTravelData, Arrays.asList(avRequest)));

        final double endDropoffTime = vrpPathWithTravelData.getArrivalTime() + timing.getDropoffDurationPerStop();
        schedule.addTask(new AVDropoffTask( //
                vrpPathWithTravelData.getArrivalTime(), endDropoffTime, avRequest.getToLink(), Arrays.asList(avRequest)));

        // TODO redundant
        if (endDropoffTime < scheduleEndTime)
            schedule.addTask(new AVStayTask( //
                    endDropoffTime, scheduleEndTime, avRequest.getToLink()));

        // jan: following computation is mandatory for the internal scoring function
        final double distance = VrpPathUtils.getDistance(vrpPathWithTravelData);
        avRequest.getRoute().setDistance(distance);
    }

    private void createAcceptRequestDirective(AVVehicle avVehicle, AVRequest avRequest) {
        boolean status = matchedRequests.add(avRequest);
        GlobalAssert.that(status); // matchedRequests did not already contain avRequest
        GlobalAssert.that(pendingRequests.contains(avRequest));

        final AVTimingParameters timing = avDispatcherConfig.getParent().getTimingParameters();
        final Schedule<AbstractTask> schedule = (Schedule<AbstractTask>) avVehicle.getSchedule();

        {
            AbstractTask currentTask = schedule.getCurrentTask();
            AbstractTask lastTask = Schedules.getLastTask(schedule);
            GlobalAssert.that(currentTask == lastTask); // check that current task is last task in schedule
        }

        final double endPickupTime = getTimeNow() + timing.getPickupDurationPerStop();
        FuturePathContainer futurePathContainer = futurePathFactory.createFuturePathContainer( //
                avRequest.getFromLink(), avRequest.getToLink(), endPickupTime);

        AbstractDirective abstractDirective = new AcceptRequestDirective( //
                avVehicle, avRequest, futurePathContainer, getTimeNow(), timing.getDropoffDurationPerStop());
        assignDirective(avVehicle, abstractDirective);

        abstractDirective.execute(); // TODO temporary for testing
    }

    /**
     * assigns new destination to vehicle.
     * if vehicle is already located at destination, nothing happens.
     * 
     * @param vehicleLinkPair
     *            is provided from super.getDivertableVehicles()
     * @param destination
     */
    protected final void setVehicleDiversion(final VehicleLinkPair vehicleLinkPair, final Link destination) {
        final Schedule<AbstractTask> schedule = (Schedule<AbstractTask>) vehicleLinkPair.avVehicle.getSchedule();
        AbstractTask abstractTask = schedule.getCurrentTask(); // <- implies that task is started
        new AVTaskAdapter(abstractTask) {
            @Override
            public void handle(AVDriveTask avDriveTask) {
                if (!avDriveTask.getPath().getToLink().equals(destination)) {
                    SimpleBlockingRouter simpleBlockingRouter = new SimpleBlockingRouter( //
                            parallelLeastCostPathCalculator, travelTime);
                    VrpPathWithTravelData vrpPathWithTravelData = simpleBlockingRouter.getRoute( //
                            vehicleLinkPair.linkTimePair.link, destination, vehicleLinkPair.linkTimePair.time);
                    {
                        TaskTracker taskTracker = avDriveTask.getTaskTracker();
                        OnlineDriveTaskTrackerImpl onlineDriveTaskTrackerImpl = (OnlineDriveTaskTrackerImpl) taskTracker;
                        final int diversionLinkIndex = onlineDriveTaskTrackerImpl.getDiversionLinkIndex();
                        final int lengthOfDiversion = vrpPathWithTravelData.getLinkCount();
                        OnlineDriveTaskTracker onlineDriveTaskTracker = (OnlineDriveTaskTracker) taskTracker;
                        onlineDriveTaskTracker.divertPath(vrpPathWithTravelData);
                        VrpPathUtils.assertIsConsistent(avDriveTask.getPath());

                        final int lengthOfCombination = avDriveTask.getPath().getLinkCount();
                        // System.out.println(String.format("[@%d of %d]", diversionLinkIndex, lengthOfCombination));
                        if (diversionLinkIndex + lengthOfDiversion != lengthOfCombination)
                            throw new RuntimeException("mismatch " + diversionLinkIndex + "+" + lengthOfDiversion + " != " + lengthOfCombination);

                    }
                    final double scheduleEndTime;
                    {
                        AVStayTask avStayTask = (AVStayTask) Schedules.getLastTask(schedule);
                        scheduleEndTime = avStayTask.getEndTime();
                    }
                    schedule.removeLastTask(); // remove stay task
                    // TODO regarding min max of begin and end time!!! when NOT to append stayTask?
                    // TODO redundant
                    if (avDriveTask.getEndTime() < scheduleEndTime)
                        schedule.addTask(new AVStayTask(avDriveTask.getEndTime(), scheduleEndTime, destination));
                }
            }

            @Override
            public void handle(AVStayTask avStayTask) {
                if (!avStayTask.getLink().equals(destination)) { // ignore request where location == target
                    final double scheduleEndTime = schedule.getEndTime(); // typically 108000.0
                    if (avStayTask.getStatus() == Task.TaskStatus.STARTED) {
                        avStayTask.setEndTime(vehicleLinkPair.linkTimePair.time);
                    } else {
                        schedule.removeLastTask();
                        System.out.println("The last task was removed for " + vehicleLinkPair.avVehicle.getId());
                        throw new RuntimeException("task should be started since current!");
                    }
                    SimpleBlockingRouter simpleBlockingRouter = new SimpleBlockingRouter( //
                            parallelLeastCostPathCalculator, travelTime);
                    VrpPathWithTravelData vrpPathWithTravelData = simpleBlockingRouter.getRoute( //
                            vehicleLinkPair.linkTimePair.link, destination, vehicleLinkPair.linkTimePair.time);
                    final AVDriveTask avDriveTask = new AVDriveTask(vrpPathWithTravelData);
                    schedule.addTask(avDriveTask);
                    final double endDriveTime = avDriveTask.getEndTime();

                    // TODO redundant
                    if (endDriveTime < scheduleEndTime)
                        schedule.addTask(new AVStayTask(endDriveTime, scheduleEndTime, destination));
                }
            }
        };
    }

    // TODO obviously these functions are nearly identical -> abstract
    private void createDriveVehicleDiversionDirective(final VehicleLinkPair vehicleLinkPair, final Link destination) {
        FuturePathContainer futurePathContainer = futurePathFactory.createFuturePathContainer( //
                vehicleLinkPair.linkTimePair.link, destination, vehicleLinkPair.linkTimePair.time);

        AbstractDirective abstractDirective = new DriveVehicleDiversionDirective(vehicleLinkPair, destination, futurePathContainer);
        assignDirective(vehicleLinkPair.avVehicle, abstractDirective);

        abstractDirective.execute();
    }

    private void createStayVehicleDiversionDirective(final VehicleLinkPair vehicleLinkPair, final Link destination) {
        FuturePathContainer futurePathContainer = futurePathFactory.createFuturePathContainer( //
                vehicleLinkPair.linkTimePair.link, destination, vehicleLinkPair.linkTimePair.time);

        AbstractDirective abstractDirective = new StayVehicleDiversionDirective(vehicleLinkPair, destination, futurePathContainer);
        assignDirective(vehicleLinkPair.avVehicle, abstractDirective);

        abstractDirective.execute(); // TODO temporary for testing
    }

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
    public final String getStatusString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("#requests " + getAVRequests().size());
        stringBuilder.append(", #stay " + //
                getStayVehicles().values().stream().flatMap(Queue::stream).count());
        stringBuilder.append(", #divert " + getDivertableVehicles().size());
        return stringBuilder.toString();
    }

}
