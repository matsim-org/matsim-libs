package org.matsim.contrib.drt.prebooking.unscheduler;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleLookup;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Task;

import com.google.common.base.Verify;

/**
 * This RequestUnscheduler searches for a request in a vehicle's schedule and
 * removes the request from the relevant stop tasks. No other changes (wrt to
 * rerouting the vehicle) are applied to the schedule.
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class SimpleRequestUnscheduler implements RequestUnscheduler {
	private final DvrpVehicleLookup vehicleLookup;

	public SimpleRequestUnscheduler(DvrpVehicleLookup vehicleLookup) {
		this.vehicleLookup = vehicleLookup;
	}

	@Override
	public void unscheduleRequest(double now, Id<DvrpVehicle> vehicleId, Id<Request> requestId) {
		DvrpVehicle vehicle = vehicleLookup.lookupVehicle(vehicleId);
		Schedule schedule = vehicle.getSchedule();

		DrtStopTask pickupTask = null;
		DrtStopTask dropoffTask = null;

		int currentIndex = schedule.getCurrentTask().getTaskIdx();
		for (; currentIndex < schedule.getTaskCount() && dropoffTask == null; currentIndex++) {
			Task currentTask = schedule.getTasks().get(currentIndex);

			if (currentTask instanceof DrtStopTask) {
				DrtStopTask stopTask = (DrtStopTask) currentTask;

				if (stopTask.getPickupRequests().keySet().contains(requestId)) {
					Verify.verify(pickupTask == null);
					pickupTask = stopTask;
				}

				if (stopTask.getDropoffRequests().keySet().contains(requestId)) {
					Verify.verify(dropoffTask == null);
					dropoffTask = stopTask;
				}
			}
		}

		Verify.verifyNotNull(pickupTask);
		Verify.verifyNotNull(dropoffTask);

		pickupTask.removePickupRequest(requestId);
		dropoffTask.removeDropoffRequest(requestId);
	}
}