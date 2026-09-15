package org.matsim.contrib.dvrp.schedule;

import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.contrib.dvrp.tracker.OnlineDriveTaskTracker;
import org.matsim.contrib.dvrp.util.LinkTimePair;
import org.matsim.core.router.util.TravelTime;

public abstract class AbstractRoutingDriveTaskUpdater implements DriveTaskUpdater {
	private final ThreadLocalLeastCostPathCalculatorProvider lcpcProvider;
	protected final TravelTime travelTime;

	protected AbstractRoutingDriveTaskUpdater(ThreadLocalLeastCostPathCalculatorProvider lcpcProvider, TravelTime travelTime) {
		this.lcpcProvider = lcpcProvider;
		this.travelTime = travelTime;
	}

	@Override
	public void updateCurrentDriveTask(DvrpVehicle vehicle, DriveTask task) {
		if (task.getTaskTracker() instanceof OnlineDriveTaskTracker tracker) {
			LinkTimePair diversionPoint = tracker.getDiversionPoint();
			if (diversionPoint != null) {
				VrpPathWithTravelData path = VrpPaths.calcAndCreatePathForDiversion(
					diversionPoint,
					task.getPath().getToLink(),
					lcpcProvider.get(),
					travelTime
				);
				tracker.divertPath(path);
			}
		}
	}

	@Override
	public void updatePlannedDriveTask(DvrpVehicle vehicle, DriveTask task, double beginTime) {
		VrpPathWithTravelData path = VrpPaths.calcAndCreatePath(
			task.getPath().getFromLink(),
			task.getPath().getToLink(),
			beginTime,
			lcpcProvider.get(),
			travelTime
		);

		DriveTask updatedTask = createDriveTask(vehicle, path);
		Schedule schedule = vehicle.getSchedule();
		schedule.removeTask(task);
		schedule.addTask(task.getTaskIdx(), updatedTask);
	}

	protected abstract DriveTask createDriveTask(DvrpVehicle vehicle, VrpPathWithTravelData path);
}
