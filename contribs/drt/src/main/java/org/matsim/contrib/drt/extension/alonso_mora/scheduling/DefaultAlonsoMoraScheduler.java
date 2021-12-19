package org.matsim.contrib.drt.extension.alonso_mora.scheduling;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.extension.alonso_mora.algorithm.AlonsoMoraRequest;
import org.matsim.contrib.drt.extension.alonso_mora.algorithm.AlonsoMoraStop;
import org.matsim.contrib.drt.extension.alonso_mora.algorithm.AlonsoMoraVehicle;
import org.matsim.contrib.drt.extension.alonso_mora.algorithm.AlonsoMoraStop.StopType;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.schedule.DrtDriveTask;
import org.matsim.contrib.drt.schedule.DrtStayTask;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.drt.schedule.DrtTaskFactory;
import org.matsim.contrib.drt.schedule.DrtTaskType;
import org.matsim.contrib.drt.scheduler.EmptyVehicleRelocator;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.contrib.dvrp.schedule.DriveTask;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.ScheduleTimingUpdater.StayTaskEndTimeCalculator;
import org.matsim.contrib.dvrp.schedule.StayTask;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.schedule.Task.TaskStatus;
import org.matsim.contrib.dvrp.tracker.OnlineDriveTaskTracker;
import org.matsim.contrib.dvrp.util.LinkTimePair;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelTime;

import com.google.common.base.Verify;

/**
 * This class translates a sequence of pickups and dropoffs as proposed by the
 * Alonso-Mora algorithm into a DVRP schedule for a vehicle. Optionally,
 * operational tasks are extracted from the current vehicle plan and inserted
 * back in after reconstructing the pickups and dropoffs.
 * 
 * Note that RouteTracker pre-computes everything that is scheduled here exactly
 * by the second if free-flow conditions are imposed.
 * 
 * @author sebhoerl
 */
public class DefaultAlonsoMoraScheduler implements AlonsoMoraScheduler {
	private final DrtTaskFactory taskFactory;
	private final LeastCostPathCalculator router;
	private final TravelTime travelTime;

	private final double stopDuration;
	private final boolean checkDeterminsticTravelTimes;
	private final boolean reroutingDuringScheduling;

	private final OperationalVoter operationalVoter;

	private final StayTaskEndTimeCalculator endTimeCalculator;

	public DefaultAlonsoMoraScheduler(DrtTaskFactory taskFactory, double stopDuration,
			boolean checkDeterminsticTravelTimes, boolean reroutingDuringScheduling, TravelTime travelTime,
			Network network, StayTaskEndTimeCalculator endTimeCalculator, LeastCostPathCalculator router,
			OperationalVoter operationalVoter) {
		this.taskFactory = taskFactory;
		this.stopDuration = stopDuration;
		this.checkDeterminsticTravelTimes = checkDeterminsticTravelTimes;
		this.reroutingDuringScheduling = reroutingDuringScheduling;
		this.endTimeCalculator = endTimeCalculator;
		this.travelTime = travelTime;
		this.router = router;
		this.operationalVoter = operationalVoter;
	}

	/**
	 * This is an additional check that makes sure that the result of the travel
	 * function does include stops for requests that are currently on board of the
	 * vehicle but have not been dropped off yet.
	 */
	private void verifyOnboardRequestsAreDroppedOff(AlonsoMoraVehicle vehicle, List<AlonsoMoraStop> stops) {
		Set<AlonsoMoraRequest> onboardRequests = new HashSet<>(
				vehicle.getOnboardRequests().stream().filter(r -> !r.isDroppedOff()).collect(Collectors.toSet()));

		for (AlonsoMoraStop stop : stops) {
			switch (stop.getType()) {
			case Dropoff:
				onboardRequests.remove(stop.getRequest());
				break;
			case Pickup:
				Verify.verify(!vehicle.getOnboardRequests().contains(stop.getRequest()),
						"Cannot pick-up onboard request");
				break;
			case Relocation:
				break;
			default:
				throw new IllegalStateException();
			}
		}

		Verify.verify(onboardRequests.size() == 0, "Some onboard requests are not dropped off");
	}

	/**
	 * This is an additional check that makes sure that pick-ups and drop-offs which
	 * are currently already in progress (task is started) are not added again as a
	 * stop. This can happen if the travel function is not implemented properly.
	 */
	private void verifyActivePickupAndDropoff(AlonsoMoraVehicle vehicle, List<AlonsoMoraStop> stops) {
		Task currentTask = vehicle.getVehicle().getSchedule().getCurrentTask();

		if (currentTask instanceof DrtStopTask) {
			DrtStopTask stopTask = (DrtStopTask) currentTask;

			for (AlonsoMoraStop stop : stops) {
				switch (stop.getType()) {
				case Dropoff:
					// This means that the stop list contains a stop with a request that is
					// currently being dropped off in the current task. As we don't want to add
					// another task with this request to the vehicle's schedule, this should not be
					// here. Most likely, this is an error in the TravelFunction.

					for (DrtRequest drtRequest : stop.getRequest().getDrtRequests()) {
						Verify.verify(!stopTask.getDropoffRequests().containsKey(drtRequest.getId()));
					}

					break;
				case Pickup:
					for (DrtRequest drtRequest : stop.getRequest().getDrtRequests()) {
						Verify.verify(!stopTask.getPickupRequests().containsKey(drtRequest.getId()));
					}

					break;
				case Relocation:
					break;
				default:
					throw new IllegalStateException();
				}
			}
		}
	}

	private void verifyRelocationIsLast(List<AlonsoMoraStop> stops) {
		for (AlonsoMoraStop stop : stops) {
			if (stop.getType().equals(StopType.Relocation)) {
				Verify.verify(stops.get(stops.size() - 1) == stop, "Relocation must be the last!");
			}
		}
	}

	public void schedule(AlonsoMoraVehicle vehicle, double now) {
		List<AlonsoMoraStop> stops = vehicle.getRoute();

		verifyOnboardRequestsAreDroppedOff(vehicle, stops);
		verifyActivePickupAndDropoff(vehicle, stops);
		verifyRelocationIsLast(stops);

		DvrpVehicle dvrpVehicle = vehicle.getVehicle();
		Schedule schedule = dvrpVehicle.getSchedule();

		Task currentTask = schedule.getCurrentTask();

		// Verify that we understand what is in the schedule
		for (int index = currentTask.getTaskIdx(); index < schedule.getTaskCount(); index++) {
			Task task = schedule.getTasks().get(index);

			boolean isStayTask = task instanceof DrtStayTask;
			boolean isStopTask = task instanceof DrtStopTask;
			boolean isDriveTask = task instanceof DrtDriveTask;
			boolean isWaitForStopTask = task instanceof WaitForStopTask;
			boolean isOperationalTask = operationalVoter.isOperationalTask(task);

			Verify.verify(isStayTask || isStopTask || isDriveTask || isWaitForStopTask || isOperationalTask,
					"Don't know what to do with this task");
		}

		// Collect operational tasks that need to be re-added at the end
		List<Task> operationalTasks = new LinkedList<>();

		for (int i = schedule.getCurrentTask().getTaskIdx() + 1; i < schedule.getTaskCount(); i++) {
			Task task = schedule.getTasks().get(i);

			if (operationalVoter.isOperationalTask(task)) {
				operationalTasks.add(task);
			}
		}

		// Clean up the task chain to reconstruct it
		while (schedule.getTasks().get(schedule.getTaskCount() - 1).getStatus().equals(TaskStatus.PLANNED)) {
			schedule.removeLastTask();
		}

		// Start rebuilding the schedule

		Link currentLink = null;

		if (operationalVoter.isOperationalTask(currentTask)) {
			currentLink = ((StayTask) currentTask).getLink();
		} else if (currentTask instanceof StayTask) {
			currentLink = ((StayTask) currentTask).getLink();

			if (currentTask instanceof DrtStayTask || currentTask instanceof WaitForStopTask) {
				// If we are currently staying somewhere, end the stay task now
				currentTask.setEndTime(now);
			}
		} else if (currentTask instanceof DriveTask) {
			// Will be handled individually further below
		} else {
			throw new IllegalStateException();
		}

		for (int index = 0; index < stops.size(); index++) {
			AlonsoMoraStop stop = stops.get(index);

			if (stop.getType().equals(StopType.Pickup) || stop.getType().equals(StopType.Dropoff)) {
				// We want to add a pickup or dropoff

				if (index == 0 && currentTask instanceof DriveTask) {
					// Vehicle is driving, so we may need to divert it

					DriveTask driveTask = (DriveTask) currentTask;

					if (!driveTask.getTaskType().equals(DrtDriveTask.TYPE)) {
						// Not a standard drive task, so we have to abort it

						OnlineDriveTaskTracker tracker = (OnlineDriveTaskTracker) driveTask.getTaskTracker();
						LinkTimePair diversionPoint = tracker.getDiversionPoint();
						VrpPathWithTravelData diversionPath = VrpPaths.createZeroLengthPathForDiversion(diversionPoint);
						tracker.divertPath(diversionPath);

						currentLink = diversionPoint.link;
					} else {
						// We have a standard drive task, so we can simply divert it

						if (driveTask.getPath().getToLink() != stop.getLink() || reroutingDuringScheduling) {
							OnlineDriveTaskTracker tracker = (OnlineDriveTaskTracker) driveTask.getTaskTracker();
							LinkTimePair diversionPoint = tracker.getDiversionPoint();
							VrpPathWithTravelData diversionPath = VrpPaths.calcAndCreatePathForDiversion(diversionPoint,
									stop.getLink(), router, travelTime);
							tracker.divertPath(diversionPath);
						}

						currentLink = stop.getLink();
					}
				}

				if (currentLink != stop.getLink()) {
					// Add a conventional drive (e.g. after a previous stop)

					VrpPathWithTravelData drivePath = VrpPaths.calcAndCreatePath(currentLink, stop.getLink(),
							currentTask.getEndTime(), router, travelTime);

					currentTask = taskFactory.createDriveTask(dvrpVehicle, drivePath, DrtDriveTask.TYPE);
					schedule.addTask(currentTask);

					currentLink = stop.getLink();
				}

				// For pre-booked requests, we may need to wait for the customer
				if (stop.getType().equals(StopType.Pickup)) {
					double expectedStartTime = stop.getRequest().getEarliestPickupTime() - stopDuration;

					if (expectedStartTime > currentTask.getEndTime()) {
						currentTask = new WaitForStopTask(currentTask.getEndTime(), expectedStartTime, currentLink);
						schedule.addTask(currentTask);
					}
				}

				// Now, retrieve or create the stop task

				DrtStopTask stopTask = null;

				if (currentTask instanceof DrtStopTask && index > 0) {
					// We're at a stop task and the stop task is not already started, so we can add
					// the requests to this stop

					stopTask = (DrtStopTask) currentTask;
				} else {
					// Create a new stop task as we are not at a previously created stop
					stopTask = taskFactory.createStopTask(dvrpVehicle, currentTask.getEndTime(),
							currentTask.getEndTime() + stopDuration, stop.getLink());

					schedule.addTask(stopTask);
					currentTask = stopTask;
				}

				// Add requests to the stop task

				if (stop.getType().equals(StopType.Pickup)) {
					stop.getRequest().getDrtRequests().forEach(stopTask::addPickupRequest);
					stop.getRequest().setPickupTask(vehicle, stopTask);

					if (checkDeterminsticTravelTimes) {
						Verify.verify(stop.getTime() == stopTask.getEndTime(),
								"Checking for determinstic travel times and found mismatch between expected stop time and scheduled stop time.");
						Verify.verify(stop.getTime() <= stop.getRequest().getPlannedPickupTime(),
								"Checking for determinstic travel times and found mismatch between expected stop time and planned stop time.");
					}
				} else if (stop.getType().equals(StopType.Dropoff)) {
					stop.getRequest().getDrtRequests().forEach(stopTask::addDropoffRequest);
					stop.getRequest().setDropoffTask(vehicle, stopTask);

					if (checkDeterminsticTravelTimes) {
						Verify.verify(stop.getTime() == stopTask.getBeginTime(),
								"Checking for determinstic travel times and found mismatch between expected stop time and scheduled stop time.");
					}
				} else {
					throw new IllegalStateException();
				}
			} else if (stop.getType().equals(StopType.Relocation)) {
				// We want to add a relocation to the schedule

				if (index == 0 && currentTask instanceof DriveTask) {
					// Vehicle is driving, so we may need to divert it

					DriveTask driveTask = (DriveTask) currentTask;

					if (!driveTask.getTaskType().equals(EmptyVehicleRelocator.RELOCATE_VEHICLE_TASK_TYPE)) {
						// Not a relocation drive task, so we have to abort it

						OnlineDriveTaskTracker tracker = (OnlineDriveTaskTracker) driveTask.getTaskTracker();
						LinkTimePair diversionPoint = tracker.getDiversionPoint();
						VrpPathWithTravelData diversionPath = VrpPaths.createZeroLengthPathForDiversion(diversionPoint);
						tracker.divertPath(diversionPath);

						currentLink = diversionPoint.link;
					} else {
						// We have a relocation drive task, so we can simply divert it

						if (driveTask.getPath().getToLink() != stop.getLink() || reroutingDuringScheduling) {
							OnlineDriveTaskTracker tracker = (OnlineDriveTaskTracker) driveTask.getTaskTracker();
							LinkTimePair diversionPoint = tracker.getDiversionPoint();
							VrpPathWithTravelData diversionPath = VrpPaths.calcAndCreatePathForDiversion(diversionPoint,
									stop.getLink(), router, travelTime);
							tracker.divertPath(diversionPath);
						}

						currentLink = stop.getLink();
					}
				}

				if (currentLink != stop.getLink()) {
					// Add a relocation drive (e.g. after a stop or aborting another drive)

					VrpPathWithTravelData drivePath = VrpPaths.calcAndCreatePath(currentLink, stop.getLink(),
							currentTask.getEndTime(), router, travelTime);
					currentTask = taskFactory.createDriveTask(dvrpVehicle, drivePath,
							EmptyVehicleRelocator.RELOCATE_VEHICLE_TASK_TYPE);
					schedule.addTask(currentTask);

					currentLink = stop.getLink();
				}
			} else {
				throw new IllegalStateException();
			}
		}

		if (stops.size() == 0 && operationalTasks.size() == 0) {
			if (currentTask instanceof DriveTask) {
				// We have neither stops nor relocation -> stop the drive

				DriveTask driveTask = (DriveTask) currentTask;

				OnlineDriveTaskTracker tracker = (OnlineDriveTaskTracker) driveTask.getTaskTracker();
				LinkTimePair diversionPoint = tracker.getDiversionPoint();
				VrpPathWithTravelData diversionPath = VrpPaths.createZeroLengthPathForDiversion(diversionPoint);
				tracker.divertPath(diversionPath);

				currentLink = diversionPoint.link;
			}
		}

		// Add operational tasks

		if (operationalTasks.size() > 0) {
			for (int index = 0; index < operationalTasks.size(); index++) {
				Task operationalTask = operationalTasks.get(index);
				Link operationalLink = ((StayTask) operationalTask).getLink();

				DrtTaskType driveTaskType = operationalVoter.getDriveTaskType(operationalTask);

				if (index == 0 && stops.size() == 0 && currentTask instanceof DriveTask) {
					// Vehicle is driving, so we may need to divert it

					DriveTask driveTask = (DriveTask) currentTask;

					if (!driveTask.getTaskType().equals(driveTaskType)) {
						// Not a standard drive task, so we have to abort it

						OnlineDriveTaskTracker tracker = (OnlineDriveTaskTracker) driveTask.getTaskTracker();
						LinkTimePair diversionPoint = tracker.getDiversionPoint();
						VrpPathWithTravelData diversionPath = VrpPaths.createZeroLengthPathForDiversion(diversionPoint);
						tracker.divertPath(diversionPath);

						currentLink = diversionPoint.link;
					} else {
						// We have a fitting drive task, so we can simply divert it

						if (driveTask.getPath().getToLink() != operationalLink || reroutingDuringScheduling) {
							OnlineDriveTaskTracker tracker = (OnlineDriveTaskTracker) driveTask.getTaskTracker();
							LinkTimePair diversionPoint = tracker.getDiversionPoint();
							VrpPathWithTravelData diversionPath = VrpPaths.calcAndCreatePathForDiversion(diversionPoint,
									operationalLink, router, travelTime);
							tracker.divertPath(diversionPath);
						}

						currentLink = operationalLink;
					}
				}

				if (currentLink != operationalLink) {
					// Add a drive

					VrpPathWithTravelData drivePath = VrpPaths.calcAndCreatePath(currentLink, operationalLink,
							currentTask.getEndTime(), router, travelTime);

					currentTask = taskFactory.createDriveTask(dvrpVehicle, drivePath, driveTaskType);
					schedule.addTask(currentTask);

					currentLink = operationalLink;
				}

				if (operationalTask.getBeginTime() > currentTask.getEndTime()) {
					// We need to fill the gap with a stay task

					currentTask = taskFactory.createStayTask(dvrpVehicle, currentTask.getEndTime(),
							operationalTask.getBeginTime(), operationalLink);
					schedule.addTask(currentTask);
				}

				double endTime = endTimeCalculator.calcNewEndTime(dvrpVehicle, (StayTask) operationalTask,
						currentTask.getEndTime());
				operationalTask.setBeginTime(currentTask.getEndTime());
				operationalTask.setEndTime(endTime);

				currentTask = operationalTask;
				schedule.addTask(currentTask);
			}
		}

		// Finally, add stay task until the end of the schedule

		if (currentTask instanceof DrtStayTask) {
			currentTask.setEndTime(Math.max(currentTask.getEndTime(), vehicle.getVehicle().getServiceEndTime()));
		} else {
			StayTask stayTask = taskFactory.createStayTask(dvrpVehicle, currentTask.getEndTime(),
					Math.max(currentTask.getEndTime(), vehicle.getVehicle().getServiceEndTime()), currentLink);
			schedule.addTask(stayTask);
		}
	}

	static public interface OperationalVoter {
		boolean isOperationalTask(Task task);

		DrtTaskType getDriveTaskType(Task task);
	}

	static public class NoopOperationalVoter implements OperationalVoter {
		@Override
		public boolean isOperationalTask(Task task) {
			return false;
		}

		@Override
		public DrtTaskType getDriveTaskType(Task task) {
			return DrtDriveTask.TYPE;
		}
	}
}
