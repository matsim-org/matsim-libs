package org.matsim.contrib.drt.extension.shifts.schedule;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.extension.shifts.operationFacilities.OperationFacilities;
import org.matsim.contrib.drt.extension.shifts.operationFacilities.OperationFacility;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.facilities.Facility;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author nkuehnel / MOIA
 */
public class ShiftSchedules {

	@SuppressWarnings("unchecked")
	public static Optional<ShiftChangeOverTask> getNextShiftChangeover (Schedule schedule) {
		int taskIdx = schedule.getStatus() == Schedule.ScheduleStatus.PLANNED ? 0 : schedule.getCurrentTask().getTaskIdx() + 1;
		return ((Stream<ShiftChangeOverTask>) schedule.tasks()
				.filter(t -> t instanceof ShiftChangeOverTask))
				.filter(t -> t.getTaskIdx() > taskIdx)
				.min(Comparator.comparingDouble(Task::getBeginTime));
	}

	public static void initSchedules(OperationFacilities operationFacilities, Fleet fleet, ShiftDrtTaskFactory taskFactory) {
		final Map<Id<Link>, List<OperationFacility>> facilitiesByLink = operationFacilities.getDrtOperationFacilities().values().stream().collect(Collectors.groupingBy(Facility::getLinkId));
		for (DvrpVehicle veh : fleet.getVehicles().values()) {
			try {
				final OperationFacility operationFacility = facilitiesByLink.get(veh.getStartLink().getId()).stream().findFirst().orElseThrow((Supplier<Throwable>) () -> new RuntimeException("Vehicles must start at an operation facility!"));
				veh.getSchedule()
						.addTask(taskFactory.createWaitForShiftStayTask(veh, veh.getServiceBeginTime(), veh.getServiceEndTime(),
								veh.getStartLink(), operationFacility));
				boolean success = operationFacility.register(veh.getId());
				if(!success) {
					throw new RuntimeException(String.format("Cannot register vehicle %s at facility %s at start-up. Please check" +
							"facility capacity and initial fleet distribution.", veh.getId().toString(), operationFacility.getId().toString()));
				}			} catch (Throwable throwable) {
				throwable.printStackTrace();
			}
		}
	}
}
