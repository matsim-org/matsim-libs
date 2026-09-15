package org.matsim.contrib.drt.schedule;

import com.google.inject.Provider;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.schedule.AbstractRoutingDriveTaskUpdater;
import org.matsim.contrib.dvrp.schedule.DriveTask;
import org.matsim.contrib.dvrp.schedule.ThreadLocalLeastCostPathCalculatorProvider;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelTime;

/**
 * @author Sebatian Hörl (sebhoerl), IRT SystemX
 */
public class DrtRoutingDriveTaskUpdater extends AbstractRoutingDriveTaskUpdater {
    private final DrtTaskFactory taskFactory;

    public DrtRoutingDriveTaskUpdater(DrtTaskFactory taskFactory, Provider<LeastCostPathCalculator> lcpc, TravelTime travelTime) {
        super(new ThreadLocalLeastCostPathCalculatorProvider(lcpc), travelTime);
        this.taskFactory = taskFactory;
    }

    @Override
    protected DriveTask createDriveTask(DvrpVehicle vehicle, VrpPathWithTravelData path) {
        return taskFactory.createDriveTask(vehicle, path, DrtDriveTask.TYPE);
    }
}
