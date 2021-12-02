package org.matsim.contrib.drt.extension.alonso_mora.algorithm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.schedule.DriveTask;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.contrib.dvrp.schedule.StayTask;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.tracker.OnlineDriveTaskTracker;
import org.matsim.contrib.dvrp.util.LinkTimePair;

import com.google.common.base.Verify;

/**
 * Default implementation of a vehicle used in the algorithm by Alonso-Mora et
 * al. The important part is how to calculate the next possible diversion of the
 * vehicle.
 * 
 * @author sebhoerl
 */
public class DefaultAlonsoMoraVehicle implements AlonsoMoraVehicle {
	protected final DvrpVehicle vehicle;
	private final Set<AlonsoMoraRequest> onboardRequests = new HashSet<>();

	private List<AlonsoMoraStop> route = new ArrayList<>();

	public DefaultAlonsoMoraVehicle(DvrpVehicle vehicle) {
		this.vehicle = vehicle;
	}

	public DvrpVehicle getVehicle() {
		return vehicle;
	}

	@Override
	public Set<AlonsoMoraRequest> getOnboardRequests() {
		return onboardRequests;
	}

	@Override
	public void addOnboardRequest(AlonsoMoraRequest request) {
		Verify.verify(onboardRequests.add(request));
	}

	@Override
	public void removeOnboardRequest(AlonsoMoraRequest request) {
		Verify.verify(onboardRequests.remove(request));
	}

	@Override
	public void setRoute(List<AlonsoMoraStop> route) {
		this.route = route;
	}

	@Override
	public List<AlonsoMoraStop> getRoute() {
		return Collections.unmodifiableList(route);
	}

	@Override
	public LinkTimePair getNextDiversion(double now) {
		Schedule schedule = vehicle.getSchedule();

		if (schedule.getStatus().equals(ScheduleStatus.STARTED)) {
			Task task = schedule.getCurrentTask();

			if (task instanceof StayTask) {
				if (task instanceof DrtStopTask) {
					// Stops need to be finished, so we return the task end time
					return new LinkTimePair(((StayTask) task).getLink(), task.getEndTime());
				}

				// If it is not a stop, the vehicle is idle, so we can divert right away
				return new LinkTimePair(((StayTask) task).getLink(), now);
			} else {
				DriveTask driveTask = (DriveTask) task;

				// If the vehicle is driving, we check the next possible link and time to divert
				OnlineDriveTaskTracker tracker = (OnlineDriveTaskTracker) driveTask.getTaskTracker();
				return tracker.getDiversionPoint();
			}
		} else if (schedule.getStatus().equals(ScheduleStatus.PLANNED)) {
			// If schedule is not started yet, next possible diversion is at the schedule
			// start point
			return new LinkTimePair(vehicle.getStartLink(), schedule.getBeginTime());
		} else {
			return new LinkTimePair(vehicle.getStartLink(), Double.POSITIVE_INFINITY);
		}
	}
}
