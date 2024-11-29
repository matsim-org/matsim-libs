package org.matsim.contrib.drt.taas.capacities;

import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.schedule.*;
import org.matsim.contrib.dvrp.fleet.DvrpLoad;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.router.speedy.SpeedyALTFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import java.util.List;
import java.util.Objects;

public class CapacityChangeSchedulerEngine implements MobsimEngine {
	private final Fleet fleet;
	private final LeastCostPathCalculator router;
	private final TravelTime travelTime;
	private static final DrtTaskType taskType = new DrtTaskType("TO_CAPACITY_CHANGE", DrtTaskBaseType.DRIVE);
	private final CapacityReconfigurationLogic defaultCapacitiesConfigurationLogic;
	private final Network network;

	public CapacityChangeSchedulerEngine(Fleet fleet, Network network, TravelDisutility travelDisutility, TravelTime travelTime, CapacityReconfigurationLogic defaultCapacitiesConfigurationLogic) {
		this.fleet = fleet;
		this.travelTime = travelTime;
		this.router = new SpeedyALTFactory().createPathCalculator(network, travelDisutility, travelTime);
		this.defaultCapacitiesConfigurationLogic = defaultCapacitiesConfigurationLogic;
		this.network = network;
	}

	@Override
	public void doSimStep(double time) {

	}

	private void insertCapacityChangeTasks(DvrpVehicle vehicle) {
		List<DefaultCapacityConfigurationLogic.CapacityChangeItem> capacityChangeItems = this.defaultCapacitiesConfigurationLogic.getPreScheduledCapacityChanges(vehicle);
		Schedule schedule = vehicle.getSchedule();

		assert schedule.getTasks().size() == 1;
		assert schedule.getStatus().equals(Schedule.ScheduleStatus.PLANNED);
		assert schedule.getTasks().get(0) instanceof DrtStayTask;

		// We leave the initial stay task at the middle of the initially allowed slot
		DrtStayTask drtStayTask = (DrtStayTask) schedule.getTasks().get(0);
		double initialEndTime = drtStayTask.getEndTime();

		double currentTime = drtStayTask.getBeginTime() + 60.0;
		for (DefaultCapacityConfigurationLogic.CapacityChangeItem capacityChangeItem : capacityChangeItems) {
			Link link = Objects.requireNonNull(network.getLinks().get(capacityChangeItem.linkId()));

			VrpPathWithTravelData driveToCapacityChangeData = VrpPaths.calcAndCreatePath(drtStayTask.getLink(), link, currentTime, router, travelTime);
			DrtDriveTask drtDriveTask = new DrtDriveTask(driveToCapacityChangeData, taskType);

			drtStayTask.setEndTime(currentTime);

			double capacityChangeBeginTime = Math.max(capacityChangeItem.time(), driveToCapacityChangeData.getArrivalTime());

			Task stayBeforeCapacityChangeTask = null;
			if (driveToCapacityChangeData.getArrivalTime() < capacityChangeItem.time()) {
				// We are already arriving before, we need to insert a stay before the capacity change
				stayBeforeCapacityChangeTask = new DrtStayTask(driveToCapacityChangeData.getArrivalTime(), capacityChangeItem.time(), link);
			}

			//Then we insert a capacity change with a duration of one minute
			Task capacityChangeTask = new DefaultDrtStopTaskWithVehicleCapacityChange(capacityChangeBeginTime, capacityChangeBeginTime + 60, link, capacityChangeItem.nextCapacity());

			//Then we insert a stay task there
			DrtStayTask stayAfterCapacityChangeTask = new DrtStayTask(capacityChangeTask.getEndTime(), Math.max(initialEndTime, capacityChangeTask.getEndTime() + 60), link);
			schedule.addTask(drtDriveTask);
			if (stayBeforeCapacityChangeTask != null) {
				schedule.addTask(stayBeforeCapacityChangeTask);
			}
			schedule.addTask(capacityChangeTask);
			schedule.addTask(stayAfterCapacityChangeTask);
			currentTime = stayAfterCapacityChangeTask.getBeginTime() + 60;
			drtStayTask = stayAfterCapacityChangeTask;
		}
	}

	@Override
	public void onPrepareSim() {
		IdMap<DvrpVehicle, DvrpLoad> startingCapacities = this.defaultCapacitiesConfigurationLogic.getOverriddenStartingCapacities();
		startingCapacities.forEach((id, load) -> Objects.requireNonNull(this.fleet.getVehicles().get(id)).setCapacity(load));
		this.fleet.getVehicles().values().forEach(this::insertCapacityChangeTasks);
	}

	@Override
	public void afterSim() {

	}

	@Override
	public void setInternalInterface(InternalInterface internalInterface) {

	}
}
