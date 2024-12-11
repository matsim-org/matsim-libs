package org.matsim.contrib.drt.taas.capacities;

import com.google.common.base.Verify;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.schedule.*;
import org.matsim.contrib.dvrp.fleet.dvrp_load.DvrpLoad;
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

/**
 * This {@link MobsimEngine} allows to plan the capacity change activities of DRT fleets at the beginning of each iteration.
 * The logic of the planning itself is handled in a {@link CapacityReconfigurationLogic}. Here we just retrieve the capacity changes to be inserted in the vehicles' schedules and perform the insertion.
 * The desired capacity changes determined by the logic might be scheduled with a later time if the vehicle cannot be there on time.
 * @author Tarek Chouaki (tkchouaki)
 */
public class CapacityChangeSchedulerEngine implements MobsimEngine {
	private final Fleet fleet;
	private final LeastCostPathCalculator router;
	private final TravelTime travelTime;
	private static final DrtTaskType taskType = new DrtTaskType("TO_CAPACITY_CHANGE", DrtTaskBaseType.DRIVE);
	private final CapacityReconfigurationLogic capacityReconfigurationLogic;
	private final Network network;
	private final double capacityChangeTaskDuration;

	public CapacityChangeSchedulerEngine(Fleet fleet, Network network, TravelDisutility travelDisutility, TravelTime travelTime, CapacityReconfigurationLogic capacityReconfigurationLogic, double capacityChangeTaskDuration) {
		this.fleet = fleet;
		this.travelTime = travelTime;
		this.router = new SpeedyALTFactory().createPathCalculator(network, travelDisutility, travelTime);
		this.capacityReconfigurationLogic = capacityReconfigurationLogic;
		this.network = network;
		this.capacityChangeTaskDuration = capacityChangeTaskDuration;
	}

	@Override
	public void doSimStep(double time) {

	}

	private void insertCapacityChangeTasks(DvrpVehicle vehicle) {
		List<DefaultCapacityReconfigurationLogic.CapacityChangeItem> capacityChangeItems = this.capacityReconfigurationLogic.getPreScheduledCapacityChanges(vehicle);
		if(capacityChangeItems.isEmpty()) {
			return;
		}

		Schedule schedule = vehicle.getSchedule();

		Verify.verify(schedule.getTasks().size() == 1);
		Verify.verify(schedule.getStatus().equals(Schedule.ScheduleStatus.PLANNED));
		Verify.verify(schedule.getTasks().getFirst() instanceof DrtStayTask);

		// We retrieve the first initial task to store its end time to conclude the schedule at the end of the process
		DrtStayTask initialStayTask = (DrtStayTask) schedule.getTasks().getFirst();

		schedule.removeTask(initialStayTask);

		double currentTime = initialStayTask.getBeginTime();
		Link previousLink = initialStayTask.getLink();
		for (DefaultCapacityReconfigurationLogic.CapacityChangeItem capacityChangeItem : capacityChangeItems) {
			Link link = Objects.requireNonNull(network.getLinks().get(capacityChangeItem.linkId()));

			VrpPathWithTravelData driveToCapacityChangeData = VrpPaths.calcAndCreatePath(previousLink, link, currentTime, router, travelTime);
			DrtDriveTask drtDriveTask = new DrtDriveTask(driveToCapacityChangeData, taskType);

			double capacityChangeBeginTime = Math.max(capacityChangeItem.time(), driveToCapacityChangeData.getArrivalTime());

			Task stayBeforeCapacityChangeTask = null;
			if (driveToCapacityChangeData.getArrivalTime() < capacityChangeItem.time()) {
				// We are already arriving before, we need to insert a stay before the capacity change
				stayBeforeCapacityChangeTask = new DrtStayTask(driveToCapacityChangeData.getArrivalTime(), capacityChangeItem.time(), link);
			}

			//Then we insert a capacity change with a duration of one minute
			Task capacityChangeTask = new DefaultDrtStopTaskWithVehicleCapacityChange(capacityChangeBeginTime, capacityChangeBeginTime + this.capacityChangeTaskDuration, link, capacityChangeItem.nextCapacity());

			schedule.addTask(drtDriveTask);
			if (stayBeforeCapacityChangeTask != null) {
				schedule.addTask(stayBeforeCapacityChangeTask);
			}
			schedule.addTask(capacityChangeTask);
			currentTime = capacityChangeTask.getEndTime();
			previousLink = link;
		}
		DrtStayTask finalStayTask = new DrtStayTask(currentTime, initialStayTask.getEndTime(), previousLink);
		schedule.addTask(finalStayTask);
	}

	@Override
	public void onPrepareSim() {
		IdMap<DvrpVehicle, DvrpLoad> startingCapacities = this.capacityReconfigurationLogic.getOverriddenStartingCapacities();
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
