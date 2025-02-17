package org.matsim.contrib.dvrp.schedule;

import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.contrib.dvrp.tracker.OnlineDriveTaskTracker;
import org.matsim.contrib.dvrp.util.LinkTimePair;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelTime;

/**
 * @author Sebatian Hörl (sebhoerl), IRT SystemX
 */
public abstract class AbstractRoutingDriveTaskUpdater implements DriveTaskUpdater {
    protected final LeastCostPathCalculator lcpc;
    protected final TravelTime travelTime;

    protected AbstractRoutingDriveTaskUpdater(LeastCostPathCalculator lspc, TravelTime travelTime) {
        this.travelTime = travelTime;
        this.lcpc = lspc;
    }

    @Override
    public void updateCurrentDriveTask(DvrpVehicle vehicle, DriveTask task) {
        if (task.getTaskTracker() instanceof OnlineDriveTaskTracker tracker) {
            LinkTimePair diversionPoint = tracker.getDiversionPoint();
            if (diversionPoint != null) {
                // perform the diversion
                VrpPathWithTravelData path = VrpPaths.calcAndCreatePathForDiversion(tracker.getDiversionPoint(),
                        task.getPath().getToLink(), lcpc, travelTime);
                tracker.divertPath(path);
            }
        }
    }

    @Override
    public void updatePlannedDriveTask(DvrpVehicle vehicle, DriveTask task, double beginTime) {
        VrpPathWithTravelData path = VrpPaths.calcAndCreatePath(task.getPath().getFromLink(),
                task.getPath().getToLink(), beginTime, lcpc, travelTime);

        // replace the task
        DriveTask updatedTask = createDriveTask(vehicle, path);

        Schedule schedule = vehicle.getSchedule();
        schedule.removeTask(task);

        schedule.addTask(task.getTaskIdx(), updatedTask);
    }

    abstract protected DriveTask createDriveTask(DvrpVehicle vehicle, VrpPathWithTravelData path);
}
