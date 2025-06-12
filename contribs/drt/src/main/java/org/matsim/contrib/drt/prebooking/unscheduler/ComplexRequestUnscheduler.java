package org.matsim.contrib.drt.prebooking.unscheduler;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.optimizer.Waypoint;
import org.matsim.contrib.drt.schedule.DrtDriveTask;
import org.matsim.contrib.drt.schedule.DrtStayTask;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.drt.schedule.DrtTaskBaseType;
import org.matsim.contrib.drt.schedule.DrtTaskFactory;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleLookup;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.contrib.dvrp.schedule.DriveTask;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.ScheduleTimingUpdater;
import org.matsim.contrib.dvrp.schedule.Schedules;
import org.matsim.contrib.dvrp.schedule.StayTask;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.schedule.Task.TaskStatus;
import org.matsim.contrib.dvrp.tracker.OnlineDriveTaskTracker;
import org.matsim.contrib.dvrp.util.LinkTimePair;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelTime;

import com.google.common.base.Verify;
import org.matsim.vehicles.Vehicle;

import java.util.List;

import static org.matsim.contrib.drt.schedule.DrtTaskBaseType.STAY;
import static org.matsim.contrib.drt.schedule.DrtTaskBaseType.STOP;

/**
 * This RequestUnscheduler searches for a request in a vehicle's schedule and
 * removes the request from the relevant stop tasks. Furthermore, the stops are
 * removed if they don't carry any other pickups or dropoffs. Accordingly, the
 * schedule will also be rerouted.
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class ComplexRequestUnscheduler implements RequestUnscheduler {
	private final DvrpVehicleLookup vehicleLookup;
	private final VehicleEntry.EntryFactory vehicleEntryFactory;

	private final DrtTaskFactory taskFactory;

	private final LeastCostPathCalculator router;
	private final TravelTime travelTime;
	private final ScheduleTimingUpdater timingUpdater;

	private final boolean scheduleWaitBeforeDrive;

	public ComplexRequestUnscheduler(DvrpVehicleLookup vehicleLookup, VehicleEntry.EntryFactory vehicleEntryFactory,
			DrtTaskFactory taskFactory, LeastCostPathCalculator router, TravelTime travelTime,
			ScheduleTimingUpdater timingUpdater, boolean scheduleWaitBeforeDrive) {
		this.vehicleLookup = vehicleLookup;
		this.vehicleEntryFactory = vehicleEntryFactory;
		this.taskFactory = taskFactory;
		this.travelTime = travelTime;
		this.router = router;
		this.timingUpdater = timingUpdater;
		this.scheduleWaitBeforeDrive = scheduleWaitBeforeDrive;
	}

	@Override
	public void unscheduleRequest(double now, Id<DvrpVehicle> vehicleId, Id<Request> requestId) {
		DvrpVehicle vehicle = vehicleLookup.lookupVehicle(vehicleId);

		DrtStopTask pickupStopTask = null;
		DrtStopTask dropoffStopTask = null;

		Schedule schedule = vehicle.getSchedule();
		List<? extends Task> tasks = schedule.getTasks();

		for (int i = schedule.getCurrentTask().getTaskIdx(); i < tasks.size(); i++) {
			Task task = tasks.get(i);
			if(task instanceof DrtStopTask stopTask) {
				if (stopTask.getPickupRequests().containsKey(requestId)) {
					Verify.verify(pickupStopTask == null);
					pickupStopTask = stopTask;
				}

				if (stopTask.getDropoffRequests().containsKey(requestId)) {
					Verify.verify(dropoffStopTask == null);
					dropoffStopTask = stopTask;
				}
			}
		}

		Verify.verifyNotNull(pickupStopTask, "Could not find request that I'm supposed to unschedule");
		Verify.verifyNotNull(dropoffStopTask, "Could not find request that I'm supposed to unschedule");

		// remove request from stop, this we do in any case
		pickupStopTask.removePickupRequest(requestId);
		dropoffStopTask.removeDropoffRequest(requestId);

		// remove pickup
		// - either we didn't find a stop (because task is running), then we have
		// removed the pickup and the StopAction will handle the situation
		// - or we found a stop, then it is not started yet and we can remove it

		boolean removePickup = pickupStopTask.getPickupRequests().isEmpty()
				&& pickupStopTask.getDropoffRequests().isEmpty();
		boolean removeDropoff = dropoffStopTask.getPickupRequests().isEmpty()
				&& dropoffStopTask.getDropoffRequests().isEmpty();

		Replacement pickupReplacement = removePickup ? findReplacement(vehicle,  pickupStopTask) : null;
		Replacement dropoffReplacement = removeDropoff ? findReplacement(vehicle, dropoffStopTask) : null;

		if (pickupReplacement != null && dropoffReplacement != null) {
			if (pickupReplacement.endTask.getTaskIdx() >= dropoffReplacement.startTask.getTaskIdx()) {
				// we have an overlap
				pickupReplacement = new Replacement(pickupReplacement.startTask, dropoffReplacement.endTask,
						vehicle.getSchedule());
				dropoffReplacement = null;
			}
		}

		if (pickupReplacement != null) {
			unschedule(now, vehicle, pickupReplacement);
		}

		if (dropoffReplacement != null) {
			unschedule(now, vehicle, dropoffReplacement);
		}
	}

	private Replacement findReplacement(DvrpVehicle vehicle, DrtStopTask stopTask) {
		// replace from current or previous task
		Task startTask = vehicle.getSchedule().getCurrentTask();
		for (Task task : Schedules.getTasksBetween(startTask.getTaskIdx(), stopTask.getTaskIdx(), vehicle.getSchedule())) {
			if (STOP.isBaseTypeOf(task)) {
				startTask = task;
			}
		}

		if(vehicle.getSchedule().getTaskCount() > startTask.getTaskIdx() + 1) {
			for (Task task : Schedules.getTasksUntilLast(stopTask.getTaskIdx() + 1, vehicle.getSchedule())) {
				if (STOP.isBaseTypeOf(task)) {
					return new Replacement(startTask, task, vehicle.getSchedule());
				}
			}
		}
		return new Replacement(startTask, Schedules.getLastTask(vehicle.getSchedule()), vehicle.getSchedule());
	}

	private void unschedule(double now, DvrpVehicle vehicle, Replacement replacement) {
		Schedule schedule = vehicle.getSchedule();

		if (replacement.startTask instanceof DrtStayTask) {
			replacement.startTask.setEndTime(now);
		}

		// special case: we remove everything until the end (and replace the stay task)
		boolean removeUntilEnd = replacement.endTask == Schedules.getLastTask(schedule);
		if (removeUntilEnd) {
			Verify.verify(replacement.endTask instanceof DrtStayTask);
			final Link stayLink;

			if (replacement.startTask instanceof StayTask) {
				stayLink = ((StayTask) replacement.startTask).getLink();
			} else {
				Verify.verify(replacement.startTask.getStatus().equals(TaskStatus.STARTED));
				DriveTask driveTask = (DriveTask) replacement.startTask;

				OnlineDriveTaskTracker tracker = (OnlineDriveTaskTracker) driveTask.getTaskTracker();
				tracker.divertPath(VrpPaths.createZeroLengthPathForDiversion(tracker.getDiversionPoint()));

				stayLink = driveTask.getPath().getToLink();
			}

			double initialEndTime = replacement.endTask.getEndTime();

			while (!(replacement.startTask == Schedules.getLastTask(schedule))) {
				schedule.removeLastTask();
			}

			schedule.addTask(taskFactory.createStayTask(vehicle, replacement.startTask.getEndTime(),
					Math.max(replacement.startTask.getEndTime(), initialEndTime), stayLink));

			return; // done
		}

		// remove everything between the two indicated tasks
		while (replacement.startTask.getTaskIdx() + 1 != replacement.endTask.getTaskIdx()) {
			Task removeTask = schedule.getTasks().get(replacement.startTask.getTaskIdx() + 1);
			schedule.removeTask(removeTask);
		}

		// if destination is not the schedule end, it must be another stop
		Verify.verify(replacement.endTask instanceof DrtStopTask);
		Link endLink = ((StayTask) replacement.endTask).getLink();
		double endArrivalTime = replacement.endTask.getBeginTime();

		final Task lastInsertedTask;
		if (replacement.startTask instanceof DriveTask) { // special case: start task is driving
			Verify.verify(replacement.startTask.getStatus().equals(TaskStatus.STARTED));

			DriveTask driveTask = (DriveTask) replacement.startTask;
			OnlineDriveTaskTracker tracker = (OnlineDriveTaskTracker) driveTask.getTaskTracker();
			LinkTimePair diversion = tracker.getDiversionPoint();

			VrpPathWithTravelData vrpPath = VrpPaths.calcAndCreatePathForDiversion(diversion, endLink, router,
					travelTime);

			if (vrpPath.getArrivalTime() < endArrivalTime && scheduleWaitBeforeDrive) {
				tracker.divertPath(VrpPaths.createZeroLengthPathForDiversion(diversion));
				lastInsertedTask = insertDriveWithWait(vehicle, replacement.startTask, vrpPath, endArrivalTime);
			} else {
				tracker.divertPath(vrpPath);
				lastInsertedTask = insertWait(vehicle, replacement.startTask, endArrivalTime);
			}
		} else { // normal case
			StayTask startStayTask = (StayTask) replacement.startTask;
			Link startLink = startStayTask.getLink();

			if (startLink == endLink) { // no need to move, maybe just wait
				if (startStayTask.getEndTime() < endArrivalTime) {
					lastInsertedTask = insertWait(vehicle, startStayTask, endArrivalTime);
				} else {
					lastInsertedTask = startStayTask; // nothing inserted
				}
			} else {
				VrpPathWithTravelData vrpPath = VrpPaths.calcAndCreatePath(startLink, endLink,
						startStayTask.getEndTime(), router, travelTime);

				lastInsertedTask = insertDriveWithWait(vehicle, startStayTask, vrpPath, endArrivalTime);
			}
		}

		timingUpdater.updateTimingsStartingFromTaskIdx(vehicle, lastInsertedTask.getTaskIdx() + 1,
				lastInsertedTask.getEndTime());
	}

	/*
	 * Copy & paste from DefaultRequestInsertionScheduler
	 */
	private Task insertWait(DvrpVehicle vehicle, Task departureTask, double earliestNextStartTime) {
		Schedule schedule = vehicle.getSchedule();

		final Link waitLink;
		if (departureTask instanceof StayTask) {
			waitLink = ((StayTask) departureTask).getLink();
		} else if (departureTask instanceof DriveTask) {
			waitLink = ((DriveTask) departureTask).getPath().getToLink();
		} else {
			throw new IllegalStateException();
		}

		if (departureTask.getEndTime() < earliestNextStartTime) {
			DrtStayTask waitTask = taskFactory.createStayTask(vehicle, departureTask.getEndTime(),
					earliestNextStartTime, waitLink);
			schedule.addTask(departureTask.getTaskIdx() + 1, waitTask);
			return waitTask;
		}

		return departureTask;
	}

	/*
	 * Copy & paste from DefaultRequestInsertionScheduler
	 */
	private Task insertDriveWithWait(DvrpVehicle vehicle, Task departureTask, VrpPathWithTravelData path,
			double latestArrivalTime) {
		Schedule schedule = vehicle.getSchedule();

		Task leadingTask = departureTask;

		if (scheduleWaitBeforeDrive) {
			double driveDepartureTime = latestArrivalTime - path.getTravelTime();

			if (driveDepartureTime > departureTask.getEndTime()) {
				// makes sense to insert a wait task before departure
				DrtStayTask waitTask = taskFactory.createStayTask(vehicle, departureTask.getEndTime(),
						driveDepartureTime, path.getFromLink());
				schedule.addTask(departureTask.getTaskIdx() + 1, waitTask);

				path = path.withDepartureTime(driveDepartureTime);
				leadingTask = waitTask;
			}
		}

		Task driveTask = taskFactory.createDriveTask(vehicle, path, DrtDriveTask.TYPE);
		schedule.addTask(leadingTask.getTaskIdx() + 1, driveTask);

		if (driveTask.getEndTime() < latestArrivalTime) {
			DrtStayTask waitTask = taskFactory.createStayTask(vehicle, driveTask.getEndTime(), latestArrivalTime,
					path.getToLink());
			schedule.addTask(driveTask.getTaskIdx() + 1, waitTask);
			return waitTask;
		} else {
			return driveTask;
		}
	}

	private class Replacement {
		final Task startTask;
		final Task endTask;

		Replacement(Task startTask, Task endTask, Schedule schedule) {
			boolean startIsOngoing = startTask.getStatus().equals(TaskStatus.STARTED);
			boolean startIsStopTask = DrtTaskBaseType.STOP.isBaseTypeOf(startTask);

			Verify.verify(startIsOngoing || startIsStopTask);
			this.startTask = startTask;

			boolean endIsLastStay = endTask instanceof DrtStayTask && Schedules.getLastTask(schedule) == endTask;
			boolean endIsStopTask = DrtTaskBaseType.STOP.isBaseTypeOf(endTask);

			Verify.verify(endIsLastStay || endIsStopTask);
			this.endTask = endTask;
		}
	}
}
