package playground.sebhoerl.avtaxi.dispatcher.single_fifo;

import java.util.LinkedList;
import java.util.Queue;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.config.AVGeneratorConfig;
import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatcher;
import playground.sebhoerl.avtaxi.dispatcher.AbstractDispatcher;
import playground.sebhoerl.avtaxi.dispatcher.utils.SingleRideAppender;
import playground.sebhoerl.avtaxi.framework.AVModule;
import playground.sebhoerl.avtaxi.passenger.AVRequest;
import playground.sebhoerl.avtaxi.schedule.AVTask;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

public class SingleFIFODispatcher extends AbstractDispatcher {
    final private Queue<AVVehicle> availableVehicles = new LinkedList<>();
    final private Queue<AVRequest> pendingRequests = new LinkedList<>();

    private boolean reoptimize = false;

    public SingleFIFODispatcher(EventsManager eventsManager, SingleRideAppender appender) {
        super(eventsManager, appender);
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
    public void protected_registerVehicle(AVVehicle vehicle) {
        availableVehicles.add(vehicle);
    }

    private void reoptimize(double now) {
        while (availableVehicles.size() > 0 && pendingRequests.size() > 0) {
            System.out.println("single FIFO heuristic is now reoptimizing. Pending requests.size(): " //
                    + pendingRequests.size() + "  availableVehicles.size()" + availableVehicles.size());
            AVVehicle vehicle = availableVehicles.poll();
            AVRequest request = pendingRequests.poll();
            appender.schedule(request, vehicle, now);
        }

        reoptimize = false;
    }

    @Override
    public void onNextTimestep(double now) {
        appender.update();
        if (reoptimize)
            reoptimize(now);
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
        public AVDispatcher createDispatcher(AVDispatcherConfig config, AVGeneratorConfig generatorConfig) {
            return new SingleFIFODispatcher(eventsManager, new SingleRideAppender(config, router, travelTime));
        }
    }
}
