package playground.clruch.dispatcher;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.schedule.AbstractTask;
import org.matsim.contrib.dvrp.schedule.DriveTask;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedules;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.tracker.OnlineDriveTaskTracker;
import org.matsim.contrib.dvrp.tracker.TaskTracker;
import org.matsim.contrib.dvrp.util.LinkTimePair;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.util.TravelTime;

import playground.clruch.router.SimpleBlockingRouter;
import playground.clruch.utils.ScheduleUtils;
import playground.clruch.utils.VrpPathUtils;
import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.config.AVTimingParameters;
import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.dispatcher.AbstractDispatcher;
import playground.sebhoerl.avtaxi.passenger.AVRequest;
import playground.sebhoerl.avtaxi.schedule.AVDriveTask;
import playground.sebhoerl.avtaxi.schedule.AVDropoffTask;
import playground.sebhoerl.avtaxi.schedule.AVPickupTask;
import playground.sebhoerl.avtaxi.schedule.AVStayTask;
import playground.sebhoerl.avtaxi.schedule.AVTask;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

/**
 * alternative to {@link AbstractDispatcher}
 */
public abstract class UniversalDispatcher extends VehicleMaintainer {
    protected final AVDispatcherConfig avDispatcherConfig;
    protected final TravelTime travelTime;
    protected final ParallelLeastCostPathCalculator parallelLeastCostPathCalculator;

    private final Set<AVRequest> pendingRequests = new HashSet<>(); // access via getAVRequests()
    private final Set<AVRequest> matchedRequests = new HashSet<>(); // for data integrity, private!

    public UniversalDispatcher( //
            AVDispatcherConfig avDispatcherConfig, //
            TravelTime travelTime, //
            ParallelLeastCostPathCalculator parallelLeastCostPathCalculator, //
            EventsManager eventsManager //
    ) {
        super(eventsManager);
        this.avDispatcherConfig = avDispatcherConfig;
        this.travelTime = travelTime;
        this.parallelLeastCostPathCalculator = parallelLeastCostPathCalculator;
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
     * called from derived class to match a vehicle with a request.
     * the function appends the pick-up, drive, and drop-off tasks for the car.
     * 
     * @param avVehicle
     * @param avRequest
     */
    protected final void setAcceptRequest(AVVehicle avVehicle, AVRequest avRequest) {
        matchedRequests.add(avRequest); // TODO assert that true

        // System.out.println(private_now + " @ " + avVehicle.getId() + " picksup " + avRequest.getPassenger().getId());
        AVTimingParameters timing = avDispatcherConfig.getParent().getTimingParameters();
        Schedule<AbstractTask> schedule = (Schedule<AbstractTask>) avVehicle.getSchedule();

        AVStayTask stayTask = (AVStayTask) Schedules.getLastTask(schedule);
        final double scheduleEndTime = schedule.getEndTime();
        stayTask.setEndTime(getTimeNow());

        AVPickupTask pickupTask = new AVPickupTask(getTimeNow(), getTimeNow() + timing.getPickupDurationPerStop(), avRequest.getFromLink(), Arrays.asList(avRequest));
        schedule.addTask(pickupTask);

        SimpleBlockingRouter simpleBlockingRouter = new SimpleBlockingRouter(parallelLeastCostPathCalculator, travelTime);
        VrpPathWithTravelData dropoffPath = simpleBlockingRouter.getRoute(avRequest.getFromLink(), avRequest.getToLink(), pickupTask.getEndTime());
        AVDriveTask dropoffDriveTask = new AVDriveTask(dropoffPath, Arrays.asList(avRequest));
        schedule.addTask(dropoffDriveTask);

        AVDropoffTask dropoffTask = new AVDropoffTask( //
                dropoffPath.getArrivalTime(), dropoffPath.getArrivalTime() + timing.getDropoffDurationPerStop(), avRequest.getToLink(), Arrays.asList(avRequest));
        schedule.addTask(dropoffTask);

        // jan: following computation is mandatory for the internal scoring function
        final double distance = VrpPathUtils.getDistance(dropoffPath);
        avRequest.getRoute().setDistance(distance);

        if (dropoffTask.getEndTime() < scheduleEndTime)
            schedule.addTask(new AVStayTask(dropoffTask.getEndTime(), scheduleEndTime, dropoffTask.getLink())); // TODO redundant
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
                    // vehicleLinkPair.avVehicle.
                    System.out.println("REROUTING [" + vehicleLinkPair.avVehicle.getId() + " @" + 1 + "]");
                    System.out.println("schedule before:");
                    System.out.println(ScheduleUtils.scheduleOf(vehicleLinkPair.avVehicle));

                    SimpleBlockingRouter simpleBlockingRouter = new SimpleBlockingRouter(parallelLeastCostPathCalculator, travelTime);
                    VrpPathWithTravelData vrpPathWithTravelData = simpleBlockingRouter.getRoute( //
                            vehicleLinkPair.linkTimePair.link, destination, vehicleLinkPair.linkTimePair.time);
                    // System.out.println(newSubPath.getFromLink().getId() + " =? " + vehicleLinkPair.linkTimePair.link.getId());
                    // System.out.println(newSubPath.getDepartureTime() + " =? " + vehicleLinkPair.linkTimePair.time);
                    {
                        TaskTracker taskTracker = avDriveTask.getTaskTracker();
                        OnlineDriveTaskTracker onlineDriveTaskTracker = (OnlineDriveTaskTracker) taskTracker;
                        onlineDriveTaskTracker.divertPath(vrpPathWithTravelData);
                    }
                    // newSubPath.getArrivalTime()

                    AVStayTask avStayTask = (AVStayTask) Schedules.getLastTask(schedule);
                    avStayTask.setBeginTime(avDriveTask.getEndTime());
                    System.out.println("schedule after:");
                    System.out.println(ScheduleUtils.scheduleOf(vehicleLinkPair.avVehicle));
                    System.out.println("---");
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
                    schedule.addTask(new AVStayTask(avDriveTask.getEndTime(), scheduleEndTime, destination)); // TODO redundant
                    // ---
                    System.out.println(ScheduleUtils.scheduleOf(vehicleLinkPair.avVehicle));
                }
            }
        };
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
        stringBuilder.append(", #stay " + getStayVehicles().size()); // TODO count better!
        stringBuilder.append(", #divert " + getDivertableVehicles().size());
        return stringBuilder.toString();
    }

}
