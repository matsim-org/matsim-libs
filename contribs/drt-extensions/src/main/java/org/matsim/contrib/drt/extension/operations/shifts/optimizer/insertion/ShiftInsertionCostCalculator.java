package org.matsim.contrib.drt.extension.operations.shifts.optimizer.insertion;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacilities;
import org.matsim.contrib.drt.extension.operations.shifts.fleet.ShiftDvrpVehicle;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.ShiftBreakTask;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.ShiftChangeOverTask;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.WaitForShiftTask;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShift;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShiftBreak;
import org.matsim.contrib.drt.optimizer.StopWaypoint;
import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.optimizer.Waypoint;
import org.matsim.contrib.drt.optimizer.insertion.InsertionCostCalculator;
import org.matsim.contrib.drt.optimizer.insertion.InsertionDetourTimeCalculator.DropoffDetourInfo;
import org.matsim.contrib.drt.optimizer.insertion.InsertionDetourTimeCalculator.PickupDetourInfo;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.schedule.DrtStayTask;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.zone.skims.TravelTimeMatrix;
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
	private final TravelTimeMatrix travelTimeMatrix;
	private final OperationFacilities opFas;
	private final Network network;

	public ShiftInsertionCostCalculator(
			MobsimTimer timer,
			InsertionCostCalculator delegate,
			OperationFacilities operationFacilities,
			Network network,
			TravelTimeMatrix travelTimeMatrix) {
		this.timer = timer;
		this.delegate = delegate;
		this.opFas = operationFacilities;
		this.network = network;
		this.travelTimeMatrix = travelTimeMatrix;
	}

	@Override
	public double calculate(DrtRequest drtRequest, Insertion insertion, DetourTimeInfo detourTimeInfo) {
		if (!checkShiftTimeConstraintsForScheduledRequests(insertion, detourTimeInfo)) {
			return INFEASIBLE_SOLUTION_COST;
		}
		return delegate.calculate(drtRequest, insertion, detourTimeInfo);
	}

	/*
	 * @Nico: I replaced the use of that info object with this function. 
	 * You assume zero dropoff duration here, I think. What you probably 
	 * would want to add is to take into account the duration of the stop. 
	 */
	private double calculateStopEnd(DropoffDetourInfo info) {
		return info.requestDropoffTime;
	}

	private double calculateStopEnd(PickupDetourInfo info) {
		return info.vehicleDepartureTime;
	}

	boolean checkShiftTimeConstraintsForScheduledRequests(Insertion insertion, DetourTimeInfo detourTimeInfo) {
		VehicleEntry vEntry = insertion.vehicleEntry;
		final int pickupIdx = insertion.pickup.index;
		final int dropoffIdx = insertion.dropoff.index;

		DrtShift currentShift = ((ShiftDvrpVehicle) vEntry.vehicle).getShifts().peek();
		double shiftEndTime = currentShift.getEndTime();
		if(shiftEndTime < calculateStopEnd(detourTimeInfo.dropoffDetourInfo)) {
			// fast fail which also captures requests that are prebooked for times outside of the shift.
			return false;
		}


		for (int s = 0; s < pickupIdx; s++) {
			StopWaypoint stop = vEntry.stops.get(s);
			if (stop.getTask() instanceof ShiftChangeOverTask) {
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
			StopWaypoint stop = vEntry.stops.get(s);
			DrtStopTask task = stop.getTask();
			if (task instanceof ShiftBreakTask) {
				final DrtShiftBreak shiftBreak = ((ShiftBreakTask) task).getShiftBreak();
				if (shiftBreak != null) {
					if (task.getBeginTime() + pickupDetourTimeLoss > shiftBreak.getScheduledLatestArrival()) {
						return false;
					}
				}
			} else if (task instanceof ShiftChangeOverTask) {
				//no stop _after_ shift change over
				return false;
			} else if(task instanceof WaitForShiftTask) {
				// there still is a wait for shift task that needs to finish before the insertion
				return false;
			}
		}

		double totalTimeLoss = detourTimeInfo.getTotalTimeLoss();
		boolean shiftEndScheduled = false;

		// all stops after the new (potential) dropoff are delayed by totalTimeLoss
		// check if this delay satisfies the time constraints at these stops
		for (int s = dropoffIdx; s < vEntry.stops.size(); s++) {
			StopWaypoint stop = vEntry.stops.get(s);
			DrtStopTask stopTask = stop.getTask();
			if (stopTask instanceof ShiftBreakTask) {
				final DrtShiftBreak shiftBreak = ((ShiftBreakTask) stopTask).getShiftBreak();
				if (shiftBreak != null) {
					final double beginTime = stopTask.getBeginTime();
					if (beginTime + totalTimeLoss > shiftBreak.getScheduledLatestArrival()) {
						return false;
					}
				}
			} else if (stopTask instanceof ShiftChangeOverTask) {
				shiftEndScheduled = true;
				final List<? extends Task> tasks = vEntry.vehicle.getSchedule().getTasks();
				final Task task = tasks.get(tasks.indexOf(stopTask) - 1);

				if (task instanceof DrtStayTask) {
					//check if stay slack is large enough
					// will not be necessary anymore once slack is correctly accounted for in waypoints
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

			if (stopTask.getBeginTime() + totalTimeLoss > stop.getLatestArrivalTime()
					|| stopTask.getEndTime() + totalTimeLoss > stop.getLatestDepartureTime()) {
				return false;
			}
		}


		// avoid shrinking break corridor too much (rather coarse for now)
		if(currentShift.getBreak().isPresent()) {
			DrtShiftBreak drtShiftBreak = currentShift.getBreak().get();
			if(!drtShiftBreak.isScheduled()) {

				if(calculateStopEnd(detourTimeInfo.dropoffDetourInfo) < drtShiftBreak.getEarliestBreakStartTime()) {
					// insertion finished before break corridor
					//ok
				} else if(calculateStopEnd(detourTimeInfo.pickupDetourInfo) > drtShiftBreak.getLatestBreakEndTime()) {
					// insertion start after break corridor
					//ok
				} else {
					double remainingTime = drtShiftBreak.getLatestBreakEndTime() - calculateStopEnd(detourTimeInfo.dropoffDetourInfo);
					if (remainingTime < drtShiftBreak.getDuration()) {
						// no meaningful break possible after insertion
						// (there could still be enough time before a prebooking though)
						return false;
					}
				}
			}
		}

		if(!shiftEndScheduled) {
			// shift end has not been scheduled yet, try to approximate whether the slack is large enough
			if(currentShift.getOperationFacilityId().isPresent()) {
				// we know where the shift will end

				Link lastLink = null;
				double potentialReturnToHubDepartureTime;
				if(dropoffIdx == vEntry.stops.size()) {
					// last stop is the new stop
					lastLink = insertion.dropoff.newWaypoint.getLink();
					potentialReturnToHubDepartureTime = calculateStopEnd(detourTimeInfo.dropoffDetourInfo);
				} else {
					// last stop is an existing stop
					lastLink = vEntry.stops.getLast().getLink();
					potentialReturnToHubDepartureTime = vEntry.stops.getLast().getDepartureTime() + totalTimeLoss;
				}

				Link hubLink = network.getLinks()
						.get(opFas.getDrtOperationFacilities()
								.get(currentShift.getOperationFacilityId().get())
								.getLinkId());
				int travelTimeEstimate = travelTimeMatrix.getTravelTime(lastLink.getToNode(), hubLink.getToNode(), potentialReturnToHubDepartureTime);

				if(potentialReturnToHubDepartureTime + travelTimeEstimate > currentShift.getEndTime()) {
					return false;
				}

			}
		}

		return true; //all time constraints of all stops are satisfied
	}
}
