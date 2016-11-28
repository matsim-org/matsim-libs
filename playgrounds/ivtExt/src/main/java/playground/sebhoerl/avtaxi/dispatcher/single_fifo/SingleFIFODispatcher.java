package playground.sebhoerl.avtaxi.dispatcher.single_fifo;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.contrib.dvrp.schedule.AbstractTask;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedules;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelTime;
import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.config.AVTimingParameters;
import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatcher;
import playground.sebhoerl.avtaxi.dispatcher.utils.SingleRideAppender;
import playground.sebhoerl.avtaxi.framework.AVModule;
import playground.sebhoerl.avtaxi.passenger.AVRequest;
import playground.sebhoerl.avtaxi.schedule.*;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

public class SingleFIFODispatcher implements AVDispatcher {
    final private SingleRideAppender appender;
    final private Queue<AVVehicle> availableVehicles = new LinkedList<>();
    final private Queue<AVRequest> pendingRequests = new LinkedList<>();

    private boolean reoptimize = false;

    public SingleFIFODispatcher(SingleRideAppender appender) {
        this.appender = appender;
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
    }

    private void reoptimize(double now) {
        while (availableVehicles.size() > 0 && pendingRequests.size() > 0) {
            AVVehicle vehicle = availableVehicles.poll();
            AVRequest request = pendingRequests.poll();
            appender.schedule(request, vehicle, now);
        }

        reoptimize = false;
    }

    @Override
    public void onNextTimestep(double now) {
        if (reoptimize) reoptimize(now);
    }

    static public class Factory implements AVDispatcherFactory {
        @Inject @Named(AVModule.AV_MODE)
        private LeastCostPathCalculator router;

        @Inject @Named(AVModule.AV_MODE)
        private TravelTime travelTime;

        @Override
        public AVDispatcher createDispatcher(AVDispatcherConfig config) {
            return new SingleFIFODispatcher(new SingleRideAppender(config, router, travelTime));
        }
    }
}
