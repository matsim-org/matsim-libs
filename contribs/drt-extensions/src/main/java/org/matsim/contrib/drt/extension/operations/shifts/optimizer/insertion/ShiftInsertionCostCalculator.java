package org.matsim.contrib.drt.extension.operations.shifts.optimizer.insertion;

import org.matsim.contrib.drt.extension.operations.shifts.fleet.ShiftDvrpVehicle;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.ShiftBreakTask;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.ShiftChangeOverTask;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShift;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShiftBreak;
import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.optimizer.Waypoint;
import org.matsim.contrib.drt.optimizer.insertion.InsertionCostCalculator;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.schedule.DrtStayTask;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.core.mobsim.framework.MobsimTimer;

import java.util.List;

import static org.matsim.contrib.drt.optimizer.insertion.InsertionDetourTimeCalculator.DetourTimeInfo;
import static org.matsim.contrib.drt.optimizer.insertion.InsertionGenerator.Insertion;

/**
 * @author nkuehnel / MOIA
 */
public class ShiftInsertionCostCalculator implements InsertionCostCalculator {

	private final InsertionCostCalculator delegate;
	private final MobsimTimer timer;

	public ShiftInsertionCostCalculator(MobsimTimer timer,
			InsertionCostCalculator delegate) {
		this.timer = timer;
		this.delegate = delegate;
	}

	@Override
	public double calculate(DrtRequest drtRequest, Insertion insertion, DetourTimeInfo detourTimeInfo) {
		if (!checkShiftTimeConstraintsForScheduledRequests(insertion, detourTimeInfo)) {
			return INFEASIBLE_SOLUTION_COST;
		}
		return delegate.calculate(drtRequest, insertion, detourTimeInfo);
	}

	boolean checkShiftTimeConstraintsForScheduledRequests(Insertion insertion, DetourTimeInfo detourTimeInfo) {
		VehicleEntry vEntry = insertion.vehicleEntry;
		final int pickupIdx = insertion.pickup.index;
		final int dropoffIdx = insertion.dropoff.index;

		DrtShift currentShift = ((ShiftDvrpVehicle) vEntry.vehicle).getShifts().peek();
		double shiftEndTime = currentShift.getEndTime();
		if(shiftEndTime < detourTimeInfo.dropoffDetourInfo.arrivalTime) {
			// fast fail which also captures requests that are prebooked for times outside of the shift.
			return false;
		}

		for (int s = 0; s < pickupIdx; s++) {
			Waypoint.Stop stop = vEntry.stops.get(s);
			if (stop.task instanceof ShiftChangeOverTask) {
				//no stop _after_ shift change over
				return false;
			}
		}

		double pickupDetourTimeLoss = detourTimeInfo.pickupDetourInfo.pickupTimeLoss;

		// each existing stop has 2 time constraints: latestArrivalTime and latestDepartureTime (see: Waypoint.Stop)
		// we are looking only at the time constraints of the scheduled requests (the new request is checked separately)

		// all stops after the new (potential) pickup but before the new dropoff are delayed by pickupDetourTimeLoss
		// check if this delay satisfies the time constraints at these stops
		for (int s = pickupIdx; s < dropoffIdx; s++) {
			Waypoint.Stop stop = vEntry.stops.get(s);
			if (stop.task instanceof ShiftBreakTask) {
				final DrtShiftBreak shiftBreak = ((ShiftBreakTask)stop.task).getShiftBreak();
				if (shiftBreak != null) {
					if (stop.task.getBeginTime() + pickupDetourTimeLoss > shiftBreak.getScheduledLatestArrival()) {
						return false;
					}
				}
			} else if (stop.task instanceof ShiftChangeOverTask) {
				//no stop _after_ shift change over
				return false;
			}
		}

		double totalTimeLoss = detourTimeInfo.getTotalTimeLoss();

		// all stops after the new (potential) dropoff are delayed by totalTimeLoss
		// check if this delay satisfies the time constraints at these stops
		for (int s = dropoffIdx; s < vEntry.stops.size(); s++) {
			Waypoint.Stop stop = vEntry.stops.get(s);
			if (stop.task instanceof ShiftBreakTask) {
				final DrtShiftBreak shiftBreak = ((ShiftBreakTask)stop.task).getShiftBreak();
				if (shiftBreak != null) {
					final double beginTime = stop.task.getBeginTime();
					if (beginTime + totalTimeLoss > shiftBreak.getScheduledLatestArrival()) {
						return false;
					}
				}
			} else if (stop.task instanceof ShiftChangeOverTask) {
				final List<? extends Task> tasks = vEntry.vehicle.getSchedule().getTasks();
				final Task task = tasks.get(tasks.indexOf(stop.task) - 1);

				if (task instanceof DrtStayTask) {
					final double refTime;
					if (task.getStatus().equals(Task.TaskStatus.STARTED)) {
						refTime = this.timer.getTimeOfDay();
					} else {
						refTime = task.getBeginTime();
					}
					if (task.getEndTime() - refTime < totalTimeLoss) {
						return false;
					}
				} else {
					return false;
				}
			}

			if (stop.task.getBeginTime() + totalTimeLoss > stop.latestArrivalTime
					|| stop.task.getEndTime() + totalTimeLoss > stop.latestDepartureTime) {
				return false;
			}
		}


		// avoid shrinking break corridor too much (rather coarse for now)
		if(currentShift.getBreak().isPresent()) {
			DrtShiftBreak drtShiftBreak = currentShift.getBreak().get();
			if(!drtShiftBreak.isScheduled()) {


				if(detourTimeInfo.dropoffDetourInfo.arrivalTime < drtShiftBreak.getEarliestBreakStartTime()) {
					// insertion finished before break corridor
					//ok
				} else if(detourTimeInfo.pickupDetourInfo.departureTime > drtShiftBreak.getLatestBreakEndTime()) {
					// insertion start after break corridor
					//ok
				} else {
					double remainingTime = drtShiftBreak.getLatestBreakEndTime() - detourTimeInfo.dropoffDetourInfo.arrivalTime;
					if (remainingTime < drtShiftBreak.getDuration()) {
						// no meaningful break possible after insertion
						// (there could still be enough time before a prebooking though)
						return false;
					}
				}
			}
		}

		return true; //all time constraints of all stops are satisfied
	}
}
