package org.matsim.contrib.drt.analysis.afterSimAnalysis;

import org.apache.commons.lang3.mutable.MutableInt;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.schedule.DrtTaskBaseType;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.passenger.PassengerDroppedOffEvent;
import org.matsim.contrib.dvrp.passenger.PassengerDroppedOffEventHandler;
import org.matsim.contrib.dvrp.passenger.PassengerPickedUpEvent;
import org.matsim.contrib.dvrp.passenger.PassengerPickedUpEventHandler;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.vrpagent.TaskEndedEvent;
import org.matsim.contrib.dvrp.vrpagent.TaskEndedEventHandler;
import org.matsim.contrib.dvrp.vrpagent.TaskStartedEvent;
import org.matsim.contrib.dvrp.vrpagent.TaskStartedEventHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StoppingTaskRecorder implements TaskEndedEventHandler, TaskStartedEventHandler,
		PassengerPickedUpEventHandler, PassengerDroppedOffEventHandler {
	Map<Id<DvrpVehicle>, MutableInt> vehicleOccupancyTracker = new HashMap<>();
	Map<Id<DvrpVehicle>, DrtTaskInformation> startedTasks = new HashMap<>();
	List<DrtTaskInformation> drtTasksEntries = new ArrayList<>();
	List<Task.TaskType> extraTaskTypesToAnalyze = new ArrayList<>();

	public static class DrtTaskInformation {
		private final String taskName;
		private final Id<Link> linkId;
		private final double startTime;
		private double endTime;
		private final Id<DvrpVehicle> vehicleId;
		private final int occupancy;

		private DrtTaskInformation(String taskName, Id<Link> linkId, double startTime, Id<DvrpVehicle> vehicleId, int occupancy) {
			this.taskName = taskName;
			this.linkId = linkId;
			this.startTime = startTime;
			this.vehicleId = vehicleId;
			this.occupancy = occupancy;
		}

		void setEndTime(double endTime) {
			this.endTime = endTime;
		}

		public String getTaskName() {
			return taskName;
		}

		public Id<Link> getLinkId() {
			return linkId;
		}

		public double getStartTime() {
			return startTime;
		}

		public double getEndTime() {
			return endTime;
		}

		public Id<DvrpVehicle> getVehicleId() {
			return vehicleId;
		}

		public int getOccupancy() {
			return occupancy;
		}
	}

	public void addExtraTaskTypeToAnalyze(Task.TaskType customizedTaskType) {
		extraTaskTypesToAnalyze.add(customizedTaskType);
	}

	@Override
	public void handleEvent(TaskStartedEvent taskStartedEvent) {
		Task.TaskType taskType = taskStartedEvent.getTaskType();
		if (DrtTaskBaseType.STAY.isBaseTypeOf(taskType) || DrtTaskBaseType.STOP.isBaseTypeOf(taskType) ||
				extraTaskTypesToAnalyze.contains(taskType)) {
			Id<DvrpVehicle> vehicleId = taskStartedEvent.getDvrpVehicleId();
			Id<Link> linkId = taskStartedEvent.getLinkId();
			double time = taskStartedEvent.getTime();
			int occupancy = vehicleOccupancyTracker.computeIfAbsent(vehicleId, v -> new MutableInt()).intValue();

			startedTasks.put(vehicleId, new DrtTaskInformation(taskType.name(), linkId, time, vehicleId, occupancy));
		}
	}

	@Override
	public void handleEvent(TaskEndedEvent taskEndedEvent) {
		Task.TaskType taskType = taskEndedEvent.getTaskType();
		if (DrtTaskBaseType.STAY.isBaseTypeOf(taskType) || DrtTaskBaseType.STOP.isBaseTypeOf(taskType) ||
				extraTaskTypesToAnalyze.contains(taskType)) {
			Id<DvrpVehicle> vehicleId = taskEndedEvent.getDvrpVehicleId();
			DrtTaskInformation drtTaskInformation = startedTasks.get(vehicleId);
			drtTaskInformation.setEndTime(taskEndedEvent.getTime());
			drtTasksEntries.add(drtTaskInformation);
			startedTasks.remove(vehicleId);
		}
	}

	@Override
	public void handleEvent(PassengerPickedUpEvent passengerPickedUpEvent) {
		Id<DvrpVehicle> vehicleId = passengerPickedUpEvent.getVehicleId();
		vehicleOccupancyTracker.get(vehicleId).increment();
	}

	@Override
	public void handleEvent(PassengerDroppedOffEvent passengerDroppedOffEvent) {
		Id<DvrpVehicle> vehicleId = passengerDroppedOffEvent.getVehicleId();
		vehicleOccupancyTracker.get(vehicleId).decrement();
	}

	@Override
	public void reset(int iteration) {
		vehicleOccupancyTracker.clear();
		startedTasks.clear();
		drtTasksEntries.clear();
	}

	public List<DrtTaskInformation> getDrtTasksEntries() {
		return drtTasksEntries;
	}
}
