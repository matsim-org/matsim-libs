package org.matsim.contrib.drt.extension.shifts.optimizer.insertion;

import java.util.List;
import java.util.function.ToDoubleFunction;

import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.optimizer.Waypoint;
import org.matsim.contrib.drt.optimizer.insertion.CostCalculationStrategy;
import org.matsim.contrib.drt.optimizer.insertion.DefaultInsertionCostCalculator;
import org.matsim.contrib.drt.optimizer.insertion.DetourTimeEstimator;
import org.matsim.contrib.drt.optimizer.insertion.InsertionCostCalculator;
import org.matsim.contrib.drt.optimizer.insertion.InsertionDetourTimeCalculator;
import org.matsim.contrib.drt.optimizer.insertion.InsertionGenerator;
import org.matsim.contrib.drt.optimizer.insertion.InsertionWithDetourData;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.schedule.DrtStayTask;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.drt.extension.shifts.schedule.ShiftBreakTask;
import org.matsim.contrib.drt.extension.shifts.schedule.ShiftChangeOverTask;
import org.matsim.contrib.drt.extension.shifts.shift.DrtShiftBreak;
import org.matsim.core.mobsim.framework.MobsimTimer;

/**
 * @author nkuehnel / MOIA
 */
public class ShiftInsertionCostCalculator<D> implements InsertionCostCalculator<D> {

	public static InsertionCostCalculatorFactory createFactory(DrtConfigGroup drtCfg, MobsimTimer timer,
			CostCalculationStrategy costCalculationStrategy) {
		return new InsertionCostCalculatorFactory() {
			@Override
			public <D> InsertionCostCalculator<D> create(ToDoubleFunction<D> detourTime,
					DetourTimeEstimator replacedDriveTimeEstimator) {
				return new ShiftInsertionCostCalculator<>(drtCfg, timer, costCalculationStrategy, detourTime,
						replacedDriveTimeEstimator);
			}
		};
	}

	private final InsertionCostCalculator<D> defaultInsertionCostCalculator;
	private final InsertionDetourTimeCalculator<D> detourTimeCalculator;
	private final MobsimTimer timer;

	public ShiftInsertionCostCalculator(DrtConfigGroup drtConfig, MobsimTimer timer,
			CostCalculationStrategy costCalculationStrategy, ToDoubleFunction<D> detourTime,
			DetourTimeEstimator replacedDriveTimeEstimator) {
		this.timer = timer;
		defaultInsertionCostCalculator = new DefaultInsertionCostCalculator<>(drtConfig, timer, costCalculationStrategy,
				detourTime, replacedDriveTimeEstimator);
		detourTimeCalculator = new InsertionDetourTimeCalculator<>(drtConfig.getStopDuration(), detourTime,
				replacedDriveTimeEstimator);
	}

	@Override
	public double calculate(DrtRequest drtRequest, InsertionWithDetourData<D> insertion) {
		//TODO precompute time slacks for each stop to filter out even more infeasible insertions ???????????
		var detourTimeInfo = detourTimeCalculator.calculateDetourTimeInfo(insertion);
		if (!checkShiftTimeConstraintsForScheduledRequests(insertion.getInsertion(), detourTimeInfo.pickupTimeLoss,
				detourTimeInfo.getTotalTimeLoss())) {
			return INFEASIBLE_SOLUTION_COST;
		}
		return defaultInsertionCostCalculator.calculate(drtRequest, insertion);
	}

	boolean checkShiftTimeConstraintsForScheduledRequests(InsertionGenerator.Insertion insertion,
			double pickupDetourTimeLoss, double totalTimeLoss) {
		VehicleEntry vEntry = insertion.vehicleEntry;
		final int pickupIdx = insertion.pickup.index;
		final int dropoffIdx = insertion.dropoff.index;

		for (int s = 0; s < pickupIdx; s++) {
			Waypoint.Stop stop = vEntry.stops.get(s);
			if (stop.task instanceof ShiftChangeOverTask) {
				//no stop _after_ shift change over
				return false;
			}
		}

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
		return true; //all time constraints of all stops are satisfied
	}
}
