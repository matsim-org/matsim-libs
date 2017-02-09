package playground.clruch.dispatcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
import org.matsim.contrib.dvrp.tracker.TaskTracker;
import org.matsim.contrib.dvrp.util.LinkTimePair;
import org.matsim.core.api.experimental.events.EventsManager;

import playground.clruch.router.SimpleBlockingRouter;
import playground.sebhoerl.avtaxi.config.AVTimingParameters;
import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.dispatcher.AbstractDispatcher;
import playground.sebhoerl.avtaxi.dispatcher.utils.SingleRideAppender;
import playground.sebhoerl.avtaxi.passenger.AVRequest;
import playground.sebhoerl.avtaxi.schedule.AVDriveTask;
import playground.sebhoerl.avtaxi.schedule.AVDropoffTask;
import playground.sebhoerl.avtaxi.schedule.AVPickupTask;
import playground.sebhoerl.avtaxi.schedule.AVStayTask;
import playground.sebhoerl.avtaxi.schedule.AVTask;

public abstract class UniversalDispatcher extends AbstractDispatcher {
    private final List<AVVehicle> vehicles = new ArrayList<>();
    private final Set<AVRequest> pendingRequests = new HashSet<>();
    private Set<AVRequest> matchedRequests = new HashSet<>();

    // ---
    public UniversalDispatcher(EventsManager eventsManager, SingleRideAppender appender) {
        super(eventsManager, appender);
    }

    private double private_now = -1;

    private Collection<AVVehicle> getFunctioningVehicles() {
        if (vehicles.isEmpty() || !vehicles.get(0).getSchedule().getStatus().equals(Schedule.ScheduleStatus.STARTED))
            return Collections.emptyList();
        // for (AVVehicle avVehicle : vehicles) {
        // Schedule<AbstractTask> schedule = (Schedule<AbstractTask>) avVehicle.getSchedule();
        // if (schedule.getStatus().equals(Schedule.ScheduleStatus.STARTED))
        // collection.add(avVehicle);
        // }
        return Collections.unmodifiableList(vehicles);
    }

    protected Collection<AVRequest> getAVRequests() {
        pendingRequests.removeAll(matchedRequests);
        matchedRequests.clear();
        return Collections.unmodifiableCollection(pendingRequests);
    }

    protected Map<Link, Queue<AVVehicle>> getStayVehicles() {
        Map<Link, Queue<AVVehicle>> map = new HashMap<>();
        for (AVVehicle avVehicle : getFunctioningVehicles()) {
            Schedule<AbstractTask> schedule = (Schedule<AbstractTask>) avVehicle.getSchedule();
            AbstractTask abstractTask = Schedules.getLastTask(schedule);
            if (abstractTask.getStatus().equals(Task.TaskStatus.STARTED)) {
                AVTask avTask = (AVTask) abstractTask;
                if (avTask.getAVTaskType().equals(AVTask.AVTaskType.STAY)) {
                    new AVTaskAdapter(abstractTask) {
                        public void handle(AVStayTask avStayTask) {
                            Link link = avStayTask.getLink();
                            if (!map.containsKey(link))
                                map.put(link, new LinkedList<>());
                            map.get(link).add(avVehicle);
                        }
                    };
                }
            }
        }
        return Collections.unmodifiableMap(map);
    }

    protected void setAcceptRequest(AVVehicle avVehicle, AVRequest avRequest) {
        matchedRequests.add(avRequest);

        System.out.println(private_now + " @ " + avVehicle.getId() + " picksup " + avRequest.getPassenger().getId());
        AVTimingParameters timing = appender.config.getParent().getTimingParameters();
        Schedule<AbstractTask> schedule = (Schedule<AbstractTask>) avVehicle.getSchedule();

        AVStayTask stayTask = (AVStayTask) Schedules.getLastTask(schedule);
        final double scheduleEndTime = schedule.getEndTime();
        stayTask.setEndTime(private_now);

        AVPickupTask pickupTask = new AVPickupTask(private_now, private_now + timing.getPickupDurationPerStop(), avRequest.getFromLink(), Arrays.asList(avRequest));
        schedule.addTask(pickupTask);

        SimpleBlockingRouter simpleBlockingRouter = new SimpleBlockingRouter(appender.router, appender.travelTime);
        VrpPathWithTravelData dropoffPath = simpleBlockingRouter.getRoute(avRequest.getFromLink(), avRequest.getToLink(), pickupTask.getEndTime());
        AVDriveTask dropoffDriveTask = new AVDriveTask(dropoffPath, Arrays.asList(avRequest));
        schedule.addTask(dropoffDriveTask);

        AVDropoffTask dropoffTask = new AVDropoffTask( //
                dropoffPath.getArrivalTime(), dropoffPath.getArrivalTime() + timing.getDropoffDurationPerStop(), avRequest.getToLink(), Arrays.asList(avRequest));
        schedule.addTask(dropoffTask);

        // jan: following computation is mandatory for the internal scoring function
        double distance = 0.0;
        for (Link link : dropoffPath)
            distance += link.getLength();
        // for (int i = 0; i < dropoffPath.getLinkCount(); i++) {
        // distance += dropoffPath.getLink(i).getLength();
        // }
        avRequest.getRoute().setDistance(distance);

        if (dropoffTask.getEndTime() < scheduleEndTime)
            schedule.addTask(new AVStayTask(dropoffTask.getEndTime(), scheduleEndTime, dropoffTask.getLink()));
    }

    protected Collection<VehicleLinkPair> getDivertableVehicles() {
        Collection<VehicleLinkPair> collection = new LinkedList<>();
        for (AVVehicle avVehicle : getFunctioningVehicles()) {
            Schedule<AbstractTask> schedule = (Schedule<AbstractTask>) avVehicle.getSchedule();
            AbstractTask abstractTask = schedule.getCurrentTask();
            new AVTaskAdapter(abstractTask) {
                @Override
                public void handle(AVDriveTask avDriveTask) {
                    // for empty cars the drive task is second to last task
                    if (Schedules.isNextToLastTask(abstractTask)) {
                        TaskTracker taskTracker = avDriveTask.getTaskTracker();
                        OnlineDriveTaskTracker onlineDriveTaskTracker = (OnlineDriveTaskTracker) taskTracker;
                        collection.add(new VehicleLinkPair(avVehicle, onlineDriveTaskTracker.getDiversionPoint()));
                    }
                }

                @Override
                public void handle(AVStayTask avStayTask) {
                    // for empty vehicles the current task has to be the last task
                    if (Schedules.isLastTask(abstractTask))
                        if (avStayTask.getBeginTime() + 5 < private_now) { // TODO magic const
                            LinkTimePair linkTimePair = new LinkTimePair(avStayTask.getLink(), private_now);
                            collection.add(new VehicleLinkPair(avVehicle, linkTimePair));
                        }
                }
            };
        }
        return collection;
    }

    protected void setVehicleDiversion(final VehicleLinkPair vehicleLinkPair, final Link dest) {
        Schedule<AbstractTask> schedule = (Schedule<AbstractTask>) vehicleLinkPair.avVehicle.getSchedule();
        AbstractTask abstractTask = schedule.getCurrentTask();
        new AVTaskAdapter(abstractTask) {
            @Override
            public void handle(AVDriveTask avDriveTask) {
                if (!avDriveTask.getPath().getToLink().equals(dest)) {
                    System.out.println("REROUTING " + vehicleLinkPair.avVehicle.getId());
                    TaskTracker taskTracker = avDriveTask.getTaskTracker();
                    OnlineDriveTaskTracker onlineDriveTaskTracker = (OnlineDriveTaskTracker) taskTracker;

                    SimpleBlockingRouter simpleBlockingRouter = new SimpleBlockingRouter(appender.router, appender.travelTime);
                    VrpPathWithTravelData newSubPath = simpleBlockingRouter.getRoute( //
                            vehicleLinkPair.linkTimePair.link, dest, vehicleLinkPair.linkTimePair.time);
                    System.out.println(newSubPath.getFromLink().getId() + " =? " + vehicleLinkPair.linkTimePair.link.getId());
                    System.out.println(newSubPath.getDepartureTime() + " =? " + vehicleLinkPair.linkTimePair.time);

                    if (newSubPath.getFromLink().getId() == vehicleLinkPair.linkTimePair.link.getId())
                        onlineDriveTaskTracker.divertPath(newSubPath);
                    else {
                        new RuntimeException("links no good").printStackTrace();
                        System.out.println("SKIPPED BECAUSE OF MISMATCH!");
                    }
                }
            }

            @Override
            public void handle(AVStayTask avStayTask) {
                if (!avStayTask.getLink().equals(dest)) { // ignore request where location == target
                    final double scheduleEndTime = schedule.getEndTime(); // typically 108000.0
                    if (avStayTask.getStatus() == Task.TaskStatus.STARTED) {
                        avStayTask.setEndTime(vehicleLinkPair.linkTimePair.time);
                    } else {
                        schedule.removeLastTask();
                        System.out.println("The last task was removed for " + vehicleLinkPair.avVehicle.getId());
                    }
                    SimpleBlockingRouter simpleBlockingRouter = new SimpleBlockingRouter(appender.router, appender.travelTime);
                    VrpPathWithTravelData routePoints = simpleBlockingRouter.getRoute( //
                            vehicleLinkPair.linkTimePair.link, dest, vehicleLinkPair.linkTimePair.time);
                    final AVDriveTask avDriveTask = new AVDriveTask(routePoints);
                    schedule.addTask(avDriveTask);
                    schedule.addTask(new AVStayTask(avDriveTask.getEndTime(), scheduleEndTime, dest));

                    if (vehicleLinkPair.avVehicle.getId().toString().equals("av_av_op1_1")) {
                        System.out.println("schedule for vehicle id " + vehicleLinkPair.avVehicle.getId() + " MODIFIED");
                        for (AbstractTask task : schedule.getTasks())
                            System.out.println(" " + task);
                    }
                }

            }
        };
    }

    public final void onNextLinkEntered(AVVehicle avVehicle, DriveTask driveTask, LinkTimePair linkTimePair) {
        // for now, do nothing
    }

    @Override
    public final void onRequestSubmitted(AVRequest request) {
        pendingRequests.add(request);
    }

    // TODO this will not be necessary!!!
    @Override
    public final void onNextTaskStarted(AVTask task) {
        // intentionally empty
    }

    @Override
    public final void onNextTimestep(double now) {
        private_now = now;
        if (0 < appender.tasks.size())
            throw new RuntimeException("appender cannot have tasks!");
        appender.update();
        reoptimize(now);
    }

    public abstract void reoptimize(double now);

    @Override
    protected final void protected_registerVehicle(AVVehicle vehicle) {
        vehicles.add(vehicle);
    }

}
