package playground.clruch.dispatcher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.schedule.AbstractTask;
import org.matsim.contrib.dvrp.schedule.DriveTask;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.tracker.OnlineDriveTaskTracker;
import org.matsim.contrib.dvrp.tracker.TaskTracker;
import org.matsim.contrib.dvrp.util.LinkTimePair;
import org.matsim.core.api.experimental.events.EventsManager;

import playground.clruch.router.SimpleBlockingRouter;
import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.dispatcher.AbstractDispatcher;
import playground.sebhoerl.avtaxi.dispatcher.utils.SingleRideAppender;
import playground.sebhoerl.avtaxi.passenger.AVRequest;
import playground.sebhoerl.avtaxi.schedule.AVDriveTask;
import playground.sebhoerl.avtaxi.schedule.AVStayTask;
import playground.sebhoerl.avtaxi.schedule.AVTask;

public abstract class UniversalDispatcher extends AbstractDispatcher {

    public final List<AVVehicle> vehicles = new ArrayList<>();
    @Deprecated
    final private Queue<AVRequest> pendingRequests = new LinkedList<>(); // TODO replace with something better!

    // ---
    public UniversalDispatcher(EventsManager eventsManager, SingleRideAppender appender) {
        super(eventsManager, appender);
    }

    private double private_now = -1; // TODO not good design

    private Collection<AVVehicle> getFunctioningVehicles() {
        Collection<AVVehicle> collection = new LinkedList<>();
        for (AVVehicle avVehicle : vehicles) {
            Schedule<AbstractTask> schedule = (Schedule<AbstractTask>) avVehicle.getSchedule();
            // List<AbstractTask> tasks = schedule.getTasks();
            // if (schedule.getStatus().equals(Schedule.ScheduleStatus.STARTED) && tasks.isEmpty())
            // throw new RuntimeException("nonono");
            if (schedule.getStatus().equals(Schedule.ScheduleStatus.STARTED))
                collection.add(avVehicle);
        }
        return collection;
    }

    protected Collection<VehicleLinkPair> getDivertableVehicles() {
        Collection<VehicleLinkPair> collection = new LinkedList<>();
        for (AVVehicle avVehicle : getFunctioningVehicles()) {
            Schedule<AbstractTask> schedule = (Schedule<AbstractTask>) avVehicle.getSchedule();
            AbstractTask abstractTask = schedule.getCurrentTask();
            new AVTaskAdapter(abstractTask) {
                @Override
                public void handle(AVDriveTask avDriveTask) {
                    TaskTracker taskTracker = avDriveTask.getTaskTracker();
                    OnlineDriveTaskTracker onlineDriveTaskTracker = (OnlineDriveTaskTracker) taskTracker;
                    collection.add(new VehicleLinkPair(avVehicle, onlineDriveTaskTracker.getDiversionPoint()));
                }

                @Override
                public void handle(AVStayTask avStayTask) {
                    LinkTimePair linkTimePair = new LinkTimePair(avStayTask.getLink(), private_now);
                    collection.add(new VehicleLinkPair(avVehicle, linkTimePair));
                }
            };
        }
        return collection;
    }

    protected void setVehicleDiversion(VehicleLinkPair vehicleLinkPair, Link dest) {
        Schedule<AbstractTask> schedule = (Schedule<AbstractTask>) vehicleLinkPair.avVehicle.getSchedule();
        // List<AbstractTask> tasks = schedule.getTasks();
        // TODO this check is obsolete!
        if (!schedule.getStatus().equals(Schedule.ScheduleStatus.STARTED))
            throw new RuntimeException("abuse of API");
        
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
                        {
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
        appender.update();
        reoptimize(now);

    }

    public abstract void reoptimize(double now);

    @Override
    protected final void protected_registerVehicle(AVVehicle vehicle) {
        vehicles.add(vehicle);
    }

}
