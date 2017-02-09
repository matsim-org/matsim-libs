package playground.clruch.dispatcher;

import java.util.Iterator;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
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

import com.google.inject.Inject;
import com.google.inject.name.Named;

import playground.clruch.router.SimpleBlockingRouter;
import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatcher;
import playground.sebhoerl.avtaxi.dispatcher.utils.SingleRideAppender;
import playground.sebhoerl.avtaxi.framework.AVModule;
import playground.sebhoerl.avtaxi.schedule.AVDriveTask;
import playground.sebhoerl.avtaxi.schedule.AVStayTask;
import playground.sebhoerl.avtaxi.schedule.AVTask;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

public class LazyDispatcher extends UniversalDispatcher {
    public static final String IDENTIFIER = "LazyDispatcher";

    private Link[] destLinks = null;

    public LazyDispatcher(EventsManager eventsManager, SingleRideAppender appender, Link[] sendAVtoLink) {
        super(eventsManager, appender);
        this.destLinks = sendAVtoLink;
    }

    @Deprecated
    @Override
    public void onNextTaskStarted(AVTask task) {
        // System.out.println("The task type is " + task.getAVTaskType().toString());
        if (task.getAVTaskType() == AVTask.AVTaskType.STAY) {
            // FIXME! maybe add too many!
            availableVehicles.add((AVVehicle) task.getSchedule().getVehicle());
        }
    }

    // @Deprecated
    // Map<AVVehicle, LinkTimePair> diversionPoints = new ConcurrentHashMap<>();

    @Deprecated
    @Override
    public void onNextLinkEntered(AVVehicle avVehicle, DriveTask driveTask, LinkTimePair linkTimePair) {
        // System.out.println("nextLinkEntered: " + avVehicle.getId() + " " + driveTask.toString() + " next diversion at:" + linkTimePair.link.getId() + " time
        // " + linkTimePair.time);
        // diversionPoints.put(avVehicle, linkTimePair);

    }

    @Override
    public void reoptimize(double now) {
        // System.out.println("lazy dispatcher is now reoptimizing. Pending requests.size(): " + pendingRequests.size() + " availableVehicles.size()" +
        // availableVehicles.size());
        // Iterator<AVRequest> requestIterator = pendingRequests.iterator();
        // iterate over all pending requests and all available vehicles and assign a vehicle if it is on the same
        // link as the pending request

        /*
         * while (requestIterator.hasNext()) {
         * 
         * AVRequest request = requestIterator.next();
         * Link custLocation = request.getFromLink();
         * Iterator<AVVehicle> vehicleIterator = availableVehicles.iterator();
         * while (vehicleIterator.hasNext()) {
         * AVVehicle vehicle = vehicleIterator.next();
         * Schedule<AbstractTask> schedule = (Schedule<AbstractTask>) vehicle.getSchedule();
         * AVStayTask stayTask = (AVStayTask) Schedules.getLastTask(schedule);
         * Link avLocation = stayTask.getLink();
         * if (avLocation.equals(custLocation)) {
         * requestIterator.remove();
         * vehicleIterator.remove();
         * appender.schedule(request, vehicle, now);
         * //System.out.println("matched AV and customer at link " + avLocation.getId().toString());
         * break;
         * }
         * }
         * 
         * }
         */

        for (AVVehicle vehicle : vehicles) {
            // if task 00 drivetask
            // if current index == 3
            // ersety path mit neuem dest

            Schedule<AbstractTask> schedule = (Schedule<AbstractTask>) vehicle.getSchedule();
            List<AbstractTask> tasks = schedule.getTasks();
            if (!tasks.isEmpty() && schedule.getStatus().equals(Schedule.ScheduleStatus.STARTED)) {

                AbstractTask abstractTask = schedule.getCurrentTask();
                AVTask avTask = (AVTask) abstractTask;
                if (avTask.getAVTaskType().equals(AVTask.AVTaskType.DRIVE)) {
                    // if (diversionPoints.containsKey(vehicle))
                    {
                        AVDriveTask avDriveTask = (AVDriveTask) avTask;
                        if (!avDriveTask.getPath().getToLink().equals(destLinks[2])) {
                            System.out.println("REROUTING " + vehicle.getId());
                            // avDriveTask.getPath()
                            TaskTracker taskTracker = avDriveTask.getTaskTracker();
                            OnlineDriveTaskTracker onlineDriveTaskTracker = (OnlineDriveTaskTracker) taskTracker;
                            final LinkTimePair linkTimePair = onlineDriveTaskTracker.getDiversionPoint();
                            // diversionPoints.get(vehicle);

                            final LinkTimePair diversionPoint = linkTimePair;
                            // linkTimePair.link
                            // VrpPathWithTravelData newSubPath = null;
                            Link divLink = linkTimePair.link;
                            double startTime = linkTimePair.time;

                            SimpleBlockingRouter simpleBlockingRouter = new SimpleBlockingRouter(appender.router, appender.travelTime);
                            VrpPathWithTravelData newSubPath = simpleBlockingRouter.getRoute(divLink, destLinks[2], startTime);
                            System.out.println(newSubPath.getFromLink().getId() + " =? " + diversionPoint.link.getId());
                            System.out.println(newSubPath.getDepartureTime() + " =? " + diversionPoint.time);

                            if (newSubPath.getFromLink().getId() == diversionPoint.link.getId())
                                onlineDriveTaskTracker.divertPath(newSubPath);
                            else
                                System.out.println("SKIPPED BECAUSE OF MISMATCH!");
                        }
                    }
                }
            }
        }
        // send all available vehicles which are in a stay task towards a certain link
        Iterator<AVVehicle> vehicleIterator = availableVehicles.iterator();
        while (vehicleIterator.hasNext()) {
            AVVehicle vehicle = vehicleIterator.next();
            Schedule<AbstractTask> schedule = (Schedule<AbstractTask>) vehicle.getSchedule();
            List<AbstractTask> tasks = schedule.getTasks();
            if (!tasks.isEmpty()) {

                AVTask lastTask = (AVTask) Schedules.getLastTask(schedule);

                {
                    // System.out.println("Task from time " + lastTask.getBeginTime() + " to " + lastTask.getEndTime());
                    // System.out.println("Number of tasks: " + schedule.getTasks().size());
                }

                // if so, change end to +1 seconds, append ride to link and stay to simEndTime
                // TODO: change end time from hard-coded 108000 to appropriate value
                if (lastTask.getAVTaskType().equals(AVTask.AVTaskType.STAY)) {
                    double scheduleEndTime = schedule.getEndTime();
                    AVStayTask stayTask = (AVStayTask) lastTask;
                    if (!stayTask.getLink().equals(destLinks[1])) {

                        {
                            System.out.println("schedule for vehicle id " + vehicle.getId() + " time now = " + now); // TODO
                            for (AbstractTask task : tasks)
                                System.out.println(" " + task);
                        }
                        // finish or remove the last stay task

                        if (stayTask.getStatus() == Task.TaskStatus.STARTED) {
                            stayTask.setEndTime(now);
                        } else {
                            schedule.removeLastTask();
                            System.out.println("The last task was removed for " + vehicle.getId());
                        }

                        SimpleBlockingRouter simpleBlockingRouter = new SimpleBlockingRouter(appender.router, appender.travelTime);
                        VrpPathWithTravelData routePoints = simpleBlockingRouter.getRoute(stayTask.getLink(), destLinks[1], now);

                        // AVDriveTask rebalanceTask = new AVDriveTask(new VrpPathWithTravelDataImpl(now, 15.0, routePoints, linkTTs));
                        AVDriveTask rebalanceTask = new AVDriveTask(routePoints);
                        schedule.addTask(rebalanceTask);
                        System.out.println("sending AV " + vehicle.getId() + " to " + destLinks[1].getId());

                        // add additional stay task
                        // TODO what happens if scheduleEndTime is smaller than the end time of the previously added AV drive task
                        schedule.addTask(new AVStayTask(rebalanceTask.getEndTime(), scheduleEndTime, destLinks[1]));
                        // remove from available vehicles
                        vehicleIterator.remove();
                        {
                            System.out.println("schedule for vehicle id " + vehicle.getId() + " MODIFIED");
                            for (AbstractTask task : schedule.getTasks())
                                System.out.println(" " + task);
                        }
                    }

                }
            }
        }
    }

    // @Override
    // public void onNextTimestep(double now) {
    // }

    static public class Factory implements AVDispatcherFactory {
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
        public AVDispatcher createDispatcher(AVDispatcherConfig config) {
            // load some random link from the network
            Id<Link> l1 = Id.createLinkId("9904005_1_rL2");
            Id<Link> l2 = Id.createLinkId("236193238_1_r");
            Id<Link> l3 = Id.createLinkId("9904005_1_rL2");
            Link sendAVtoLink1 = network.getLinks().get(l1);
            Link sendAVtoLink2 = network.getLinks().get(l2);
            Link sendAVtoLink3 = network.getLinks().get(l3);
            Link[] sendAVtoLinks = new Link[] { sendAVtoLink1, sendAVtoLink2, sendAVtoLink3 };
            // put the link into the lazy dispatcher
            return new LazyDispatcher(eventsManager, new SingleRideAppender(config, router, travelTime), sendAVtoLinks);
        }
    }
}
