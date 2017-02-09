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
    public Queue<AVVehicle> availableVehicles = new LinkedList<>(); // TODO remove
    @Deprecated
    final private Queue<AVRequest> pendingRequests = new LinkedList<>(); // TODO remove

    // ---
    public UniversalDispatcher(EventsManager eventsManager, SingleRideAppender appender) {
        super(eventsManager, appender);
    }

    private double private_now_time = -1; // TODO not good design

    protected Collection<VehicleLinkPair> getDivertableVehicles() {
        Collection<VehicleLinkPair> collection = new LinkedList<>();
        for (AVVehicle avVehicle : vehicles) {
            Schedule<AbstractTask> schedule = (Schedule<AbstractTask>) avVehicle.getSchedule();
            List<AbstractTask> tasks = schedule.getTasks();
            if (!tasks.isEmpty() && schedule.getStatus().equals(Schedule.ScheduleStatus.STARTED)) {
                AbstractTask abstractTask = schedule.getCurrentTask();
                AVTask avTask = (AVTask) abstractTask;
                switch (avTask.getAVTaskType()) {
                case DRIVE: {
                    AVDriveTask avDriveTask = (AVDriveTask) avTask;
                    TaskTracker taskTracker = avDriveTask.getTaskTracker();
                    OnlineDriveTaskTracker onlineDriveTaskTracker = (OnlineDriveTaskTracker) taskTracker;
                    collection.add(new VehicleLinkPair(avVehicle, onlineDriveTaskTracker.getDiversionPoint()));
                    break;
                }
                case STAY: {
                    AVStayTask avStayTask = (AVStayTask) avTask;
                    LinkTimePair linkTimePair = new LinkTimePair(avStayTask.getLink(), private_now_time);
                    collection.add(new VehicleLinkPair(avVehicle, linkTimePair));
                    break;
                }
                default:
                    break;
                }
            }
        }
        return collection;
    }

    protected void divertVehicle(VehicleLinkPair vehicleLinkPair, Link dest) {
        Schedule<AbstractTask> schedule = (Schedule<AbstractTask>) vehicleLinkPair.avVehicle.getSchedule();
        List<AbstractTask> tasks = schedule.getTasks();
        if (!tasks.isEmpty() && schedule.getStatus().equals(Schedule.ScheduleStatus.STARTED)) {
            AbstractTask abstractTask = schedule.getCurrentTask();
            AVTask avTask = (AVTask) abstractTask;
            switch (avTask.getAVTaskType()) {
            // TODO STAY task
            case DRIVE: {
                AVDriveTask avDriveTask = (AVDriveTask) avTask;
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
                break;
            }
            default:
                break;
            }
        }

    }

    public final void onNextLinkEntered(AVVehicle avVehicle, DriveTask driveTask, LinkTimePair linkTimePair) {
        // for now, do nothing
    }

    @Override
    public final void onRequestSubmitted(AVRequest request) {
        pendingRequests.add(request);
    }

    // TODO this will not be necessary!!!
    // @Override
    // public void onNextTaskStarted(AVTask task) {
    // }

    @Override
    public final void onNextTimestep(double now) {
        private_now_time = now;
        appender.update();
        reoptimize(now);

    }

    public abstract void reoptimize(double now);

    @Override
    protected final void protected_registerVehicle(AVVehicle vehicle) {
        vehicles.add(vehicle);
        availableVehicles.add(vehicle);

    }

}
