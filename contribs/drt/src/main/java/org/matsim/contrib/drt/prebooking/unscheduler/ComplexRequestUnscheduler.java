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
		VehicleEntry vEntry = vehicleEntryFactory.create(vehicle, now);

		Waypoint pickupStop = null;
		Waypoint.Stop dropoffStop = null;

		DrtStopTask pickupStopTask = null;
		DrtStopTask dropoffStopTask = null;

		for (Waypoint.Stop stop : vEntry.stops) {
			if (stop.task.getPickupRequests().containsKey(requestId)) {
				Verify.verify(pickupStop == null);
				Verify.verify(pickupStopTask == null);

				pickupStop = stop;
				pickupStopTask = stop.task;
			}

			if (stop.task.getDropoffRequests().containsKey(requestId)) {
				Verify.verify(dropoffStop == null);
				Verify.verify(dropoffStopTask == null);

				dropoffStop = stop;
				dropoffStopTask = stop.task;
			}
		}

		if(pickupStopTask == null) {
			if(vEntry.start.task.orElseThrow() instanceof DrtStopTask stopTask) {
				if(stopTask.getPickupRequests().containsKey(requestId)) {
					pickupStopTask = stopTask;
					pickupStop = vEntry.start;
				}
			}
		}

		Verify.verifyNotNull(pickupStopTask, "Could not find request that I'm supposed to unschedule");
		Verify.verifyNotNull(dropoffStopTask, "Could not find request that I'm supposed to unschedule");
		Verify.verifyNotNull(pickupStop);
		Verify.verifyNotNull(dropoffStop);

		// remove request from stop, this we do in any case
		pickupStopTask.removePickupRequest(requestId);
		dropoffStopTask.removeDropoffRequest(requestId);

		// remove pickup
		// - either we didn't find a stop (because task is running), then we have
		// removed the pickup and the StopAction will handle the situation
		// - or we found a stop, then it is not started yet and we can remove it

		boolean removePickup = pickupStopTask.getPickupRequests().isEmpty()
				&& pickupStopTask.getDropoffRequests().isEmpty()
				&& pickupStop instanceof Waypoint.Stop;
		boolean removeDropoff = dropoffStopTask.getPickupRequests().isEmpty()
				&& dropoffStopTask.getDropoffRequests().isEmpty();

		Replacement pickupReplacement = removePickup ? findReplacement(vEntry, (Waypoint.Stop) pickupStop) : null;
		Replacement dropoffReplacement = removeDropoff ? findReplacement(vEntry, dropoffStop) : null;

		if (pickupReplacement != null && dropoffReplacement != null) {
			if (pickupReplacement.endTask.getTaskIdx() >= dropoffReplacement.startTask.getTaskIdx()) {
				// we have an overlap
				pickupReplacement = new Replacement(pickupReplacement.startTask, dropoffReplacement.endTask,
						vehicle.getSchedule());
				dropoffReplacement = null;
			}
		}

		if (pickupReplacement != null) {
			unschedule(now, vEntry, pickupReplacement);
		}

		if (dropoffReplacement != null) {
			unschedule(now, vEntry, dropoffReplacement);
		}
	}

	private Replacement findReplacement(VehicleEntry vEntry, Waypoint.Stop stop) {
		int stopIndex = vEntry.stops.indexOf(stop);

		final Task startTask;
		if (stopIndex == 0) {
			startTask = vEntry.vehicle.getSchedule().getCurrentTask();
		} else {
			startTask = vEntry.stops.get(stopIndex - 1).task;
		}

		final Task endTask;
		if (stopIndex == vEntry.stops.size() - 1) {
			endTask = Schedules.getLastTask(vEntry.vehicle.getSchedule());
		} else {
			endTask = vEntry.stops.get(stopIndex + 1).task;
		}

		return new Replacement(startTask, endTask, vEntry.vehicle.getSchedule());
	}

	private void unschedule(double now, VehicleEntry vEntry, Replacement replacement) {
		Schedule schedule = vEntry.vehicle.getSchedule();

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

			schedule.addTask(taskFactory.createStayTask(vEntry.vehicle, replacement.startTask.getEndTime(),
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
				lastInsertedTask = insertDriveWithWait(vEntry.vehicle, replacement.startTask, vrpPath, endArrivalTime);
			} else {
				tracker.divertPath(vrpPath);
				lastInsertedTask = insertWait(vEntry.vehicle, replacement.startTask, endArrivalTime);
			}
		} else { // normal case
			StayTask startStayTask = (StayTask) replacement.startTask;
			Link startLink = startStayTask.getLink();

			if (startLink == endLink) { // no need to move, maybe just wait
				if (startStayTask.getEndTime() < endArrivalTime) {
					lastInsertedTask = insertWait(vEntry.vehicle, startStayTask, endArrivalTime);
				} else {
					lastInsertedTask = startStayTask; // nothing inserted
				}
			} else {
				VrpPathWithTravelData vrpPath = VrpPaths.calcAndCreatePath(startLink, endLink,
						startStayTask.getEndTime(), router, travelTime);

				lastInsertedTask = insertDriveWithWait(vEntry.vehicle, startStayTask, vrpPath, endArrivalTime);
			}
		}

		timingUpdater.updateTimingsStartingFromTaskIdx(vEntry.vehicle, lastInsertedTask.getTaskIdx() + 1,
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
