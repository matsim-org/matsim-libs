package org.matsim.contrib.drt.extension.operations.shifts.optimizer.insertion;

import org.matsim.contrib.drt.extension.operations.shifts.fleet.ShiftDvrpVehicle;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.ShiftChangeOverTask;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShift;
import org.matsim.contrib.drt.optimizer.StopWaypoint;
import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.optimizer.insertion.InsertionCostCalculator;
import org.matsim.contrib.drt.optimizer.insertion.InsertionDetourTimeCalculator.DropoffDetourInfo;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.schedule.DrtStopTask;

import static org.matsim.contrib.drt.optimizer.insertion.InsertionDetourTimeCalculator.DetourTimeInfo;
import static org.matsim.contrib.drt.optimizer.insertion.InsertionGenerator.Insertion;

/**
 * @author nkuehnel / MOIA
 */
public class ShiftInsertionCostCalculator implements InsertionCostCalculator {

	private final InsertionCostCalculator delegate;

	public ShiftInsertionCostCalculator(
			InsertionCostCalculator delegate) {
		this.delegate = delegate;
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


	boolean checkShiftTimeConstraintsForScheduledRequests(Insertion insertion, DetourTimeInfo detourTimeInfo) {
		VehicleEntry vEntry = insertion.vehicleEntry;

        DrtShift currentShift = ((ShiftDvrpVehicle) vEntry.vehicle).getShifts().peek();
        if(currentShift.getEndTime() < calculateStopEnd(detourTimeInfo.dropoffDetourInfo)) {
			// fast fail which also captures requests that are prebooked for times outside the shift.
			return false;
		}

		for (int s = 0; s < insertion.dropoff.index; s++) {
			StopWaypoint stop = vEntry.stops.get(s);
			DrtStopTask task = stop.getTask();
			if (task instanceof ShiftChangeOverTask) {
				//no stop _after_ shift change over
				return false;
			}
		}
		return true;
	}
}
