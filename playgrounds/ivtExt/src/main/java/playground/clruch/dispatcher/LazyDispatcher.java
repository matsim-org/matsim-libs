package playground.clruch.dispatcher;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.contrib.dvrp.schedule.AbstractTask;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedules;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatcher;
import playground.sebhoerl.avtaxi.dispatcher.AVVehicleAssignmentEvent;
import playground.sebhoerl.avtaxi.dispatcher.AbstractDispatcher;
import playground.sebhoerl.avtaxi.dispatcher.utils.SingleRideAppender;
import playground.sebhoerl.avtaxi.framework.AVModule;
import playground.sebhoerl.avtaxi.passenger.AVRequest;
import playground.sebhoerl.avtaxi.schedule.AVDriveTask;
import playground.sebhoerl.avtaxi.schedule.AVStayTask;
import playground.sebhoerl.avtaxi.schedule.AVTask;
import playground.sebhoerl.plcpc.LeastCostPathFuture;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

public class LazyDispatcher extends AbstractDispatcher {
    public static final String IDENTIFIER = "LazyDispatcher";
    
    final private Queue<AVVehicle> availableVehicles = new LinkedList<>();
    final private Queue<AVRequest> pendingRequests = new LinkedList<>();
    private Link[] destLinks = null;

    private boolean reoptimize = false;

    public LazyDispatcher(EventsManager eventsManager, SingleRideAppender appender, Link[] sendAVtoLink) {
        super(eventsManager, appender);
        this.destLinks = sendAVtoLink;
    }

    @Override
    public void onRequestSubmitted(AVRequest request) {
        pendingRequests.add(request);
        reoptimize = true;
    }

    @Override
    public void onNextTaskStarted(AVTask task) {
        if (task.getAVTaskType() == AVTask.AVTaskType.STAY) {
            availableVehicles.add((AVVehicle) task.getSchedule().getVehicle());
        }
    }

    @Override
    public void registerVehicle(AVVehicle vehicle) {
        availableVehicles.add(vehicle);
        eventsManager.processEvent(new AVVehicleAssignmentEvent(vehicle, 0));
    }

    private void reoptimize(double now) {
        System.out.println("lazy dispatcher is now reoptimizing. Pending requests.size(): " + pendingRequests.size() + "  availableVehicles.size()" + availableVehicles.size());
        Iterator<AVRequest> requestIterator = pendingRequests.iterator();
        // iterate over all pending requests and all available vehicles and assign a vehicle if it is on the same
        // link as the pending request
        while (requestIterator.hasNext()) {
            AVRequest request = requestIterator.next();
            Link custLocation = request.getFromLink();
            Iterator<AVVehicle> vehicleIterator = availableVehicles.iterator();
            while (vehicleIterator.hasNext()) {
                AVVehicle vehicle = vehicleIterator.next();
                Schedule<AbstractTask> schedule = (Schedule<AbstractTask>) vehicle.getSchedule();
                AVStayTask stayTask = (AVStayTask) Schedules.getLastTask(schedule);
                Link avLocation = stayTask.getLink();
                if (avLocation.equals(custLocation)) {
                    requestIterator.remove();
                    vehicleIterator.remove();
                    appender.schedule(request, vehicle, now);
                    System.out.println("matched AV and customer at link " + avLocation.getId().toString());
                    break;
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
                if (lastTask.getAVTaskType().equals(AVTask.AVTaskType.STAY)) {
                    double scheduleEndTime = schedule.getEndTime();
                    // remove the last stay task
                    AVStayTask stayTask = (AVStayTask) lastTask;
                    if (stayTask.getStatus() == Task.TaskStatus.STARTED) {
                        stayTask.setEndTime(now);
                    } else {
                        schedule.removeLastTask();
                    }

                    // add the drive task
                    double[] linkTTs = new double[]{20.3, 50.8};
                    //Link[] routePoints =new Link[] {stayTask.getLink(),destLinks[1]};
                    ParallelLeastCostPathCalculator router = appender.router;
                    LeastCostPathFuture drivepath = router.calcLeastCostPath(stayTask.getLink().getToNode(), destLinks[1].getFromNode(), now, null, null);
                    while(!drivepath.isDone()) {
                        try {
                            Thread.sleep(5);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    VrpPathWithTravelData routePoints = VrpPaths.createPath(stayTask.getLink(), destLinks[1], now,drivepath.get(), appender.travelTime);


                    //AVDriveTask rebalanceTask = new AVDriveTask(new VrpPathWithTravelDataImpl(now, 15.0, routePoints, linkTTs));
                    AVDriveTask rebalanceTask = new AVDriveTask(routePoints);
                    schedule.addTask(rebalanceTask);
                    System.out.println("sending AV " + vehicle.getId() + " to " + destLinks[1].getId());

                    // add additional stay task
                    // TODO what happens if scheduleEndTime is smaller than the end time of the previously added AV drive task
                    schedule.addTask(new AVStayTask(rebalanceTask.getEndTime(), scheduleEndTime, destLinks[1]));

                    // remove from available vehicles
                    vehicleIterator.remove();
                }
            }
        }
        reoptimize = false;
    }

    @Override
    public void onNextTimestep(double now) {
        appender.update();
        if (reoptimize) reoptimize(now);
    }

    static public class Factory implements AVDispatcherFactory {
        @Inject
        @Named(AVModule.AV_MODE)
        private ParallelLeastCostPathCalculator router;

        @Inject
        @Named(AVModule.AV_MODE)
        private TravelTime travelTime;

        @Inject
        private EventsManager eventsManager;

        @Override
        public AVDispatcher createDispatcher(AVDispatcherConfig config) {
            // load some random link from the network
            Id<Link> l1 = Id.createLinkId("9904005_1_rL2");
            Id<Link> l2 = Id.createLinkId("236193238_1_r");
            Link sendAVtoLink1 = playground.clruch.RunAVScenario.NETWORKINSTANCE.getLinks().get(l1);
            Link sendAVtoLink2 = playground.clruch.RunAVScenario.NETWORKINSTANCE.getLinks().get(l2);
            Link[] sendAVtoLinks = new Link[]{sendAVtoLink1, sendAVtoLink2};
            //sendAVtoLinks[0] = sendAVtoLink1;
            //sendAVtoLinks[1] = sendAVtoLink2;


            // put the link into the lazy dispatcher
            return new LazyDispatcher(eventsManager, new SingleRideAppender(config, router, travelTime), sendAVtoLinks);
        }
    }
}
