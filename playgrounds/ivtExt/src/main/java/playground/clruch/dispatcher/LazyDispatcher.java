package playground.clruch.dispatcher;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.schedule.AbstractTask;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedules;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.util.TravelTime;
import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatcher;
import playground.sebhoerl.avtaxi.dispatcher.AVVehicleAssignmentEvent;
import playground.sebhoerl.avtaxi.dispatcher.utils.SingleRideAppender;
import playground.sebhoerl.avtaxi.framework.AVModule;
import playground.sebhoerl.avtaxi.passenger.AVRequest;
import playground.sebhoerl.avtaxi.schedule.AVStayTask;
import playground.sebhoerl.avtaxi.schedule.AVTask;
import playground.sebhoerl.plcpc.LeastCostPathFuture;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

public class LazyDispatcher implements AVDispatcher {
    final private SingleRideAppender appender;
    final private Queue<AVVehicle> availableVehicles = new LinkedList<>();
    final private Queue<AVRequest> pendingRequests = new LinkedList<>();
    public static final String IDENTIFIER = "LazyDispatcher";

    final private EventsManager eventsManager;

    private boolean reoptimize = false;

    public LazyDispatcher(EventsManager eventsManager, SingleRideAppender appender) {
        this.appender = appender;
        this.eventsManager = eventsManager;
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
    public void addVehicle(AVVehicle vehicle) {
        availableVehicles.add(vehicle);
        eventsManager.processEvent(new AVVehicleAssignmentEvent(vehicle, 0));
    }

    private void reoptimize(double now) {
        System.out.println("lazy dispatcher is now reoptimizing. Pending requests.size(): " + pendingRequests.size() + "  availableVehicles.size()" + availableVehicles.size());
        Iterator<AVRequest> iterator = pendingRequests.iterator();

        while (iterator.hasNext()) {
            AVRequest request = iterator.next();
            Link custLocation = request.getFromLink();
            Iterator<AVVehicle> vehicleIterator = availableVehicles.iterator();
            while(vehicleIterator.hasNext()){
                AVVehicle vehicle = vehicleIterator.next();
                Schedule<AbstractTask> schedule = (Schedule<AbstractTask>) vehicle.getSchedule();
                AVStayTask stayTask = (AVStayTask) Schedules.getLastTask(schedule);
                Link avLocation = stayTask.getLink();
                if (avLocation.equals(custLocation)) {
                    iterator.remove();
                    vehicleIterator.remove();
                    appender.schedule(request,vehicle,now);
                    System.out.println("matched AV and customer at link " + avLocation.getId().toString());
                    break;
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
            return new LazyDispatcher(eventsManager, new SingleRideAppender(config, router, travelTime));
        }
    }
}
