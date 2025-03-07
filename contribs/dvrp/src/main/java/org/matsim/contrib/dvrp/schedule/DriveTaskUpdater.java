package org.matsim.contrib.dvrp.schedule;

import org.matsim.contrib.dvrp.fleet.DvrpVehicle;

/**
 * This interface is supposed to update drive tasks during the simulation that
 * are already present in vehicle schedules. This way, rerouting in response to
 * congestion or other phenomena can be performed by DVRP.
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public interface DriveTaskUpdater {
    void updateCurrentDriveTask(DvrpVehicle vehicle, DriveTask task);

    void updatePlannedDriveTask(DvrpVehicle vehicle, DriveTask task, double beginTime);

    static DriveTaskUpdater NOOP = new DriveTaskUpdater() {
        @Override
        public void updateCurrentDriveTask(DvrpVehicle vehicle, DriveTask task) {
            // do nothing
        }

        @Override
        public void updatePlannedDriveTask(DvrpVehicle vehicle, DriveTask task, double beginTime) {
            // do nothing
        }
    };
}
