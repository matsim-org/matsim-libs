package org.matsim.contrib.drt.extension.alonso_mora.shifts;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.extension.alonso_mora.algorithm.AlonsoMoraRequest;
import org.matsim.contrib.drt.extension.alonso_mora.algorithm.AlonsoMoraStop;
import org.matsim.contrib.drt.extension.alonso_mora.algorithm.AlonsoMoraVehicle;
import org.matsim.contrib.drt.extension.alonso_mora.algorithm.function.DefaultAlonsoMoraFunction.Constraint;
import org.matsim.contrib.drt.extension.alonso_mora.algorithm.function.RouteTracker;
import org.matsim.contrib.drt.extension.alonso_mora.travel_time.TravelTimeEstimator;
import org.matsim.contrib.drt.extension.shifts.schedule.OperationalStop;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.StayTask;
import org.matsim.contrib.dvrp.schedule.Task;

/**
 * Constraint that makes the Alonso-Mora dispatcher take into account shifts and
 * breaks.
 * 
 * @author sebhoerl
 */
public class AlonsoMoraShiftConstraint implements Constraint {
	private final TravelTimeEstimator travelTimeEstimator;

	public AlonsoMoraShiftConstraint(TravelTimeEstimator travelTimeEstimator) {
		this.travelTimeEstimator = travelTimeEstimator;
	}

	@Override
	public double checkAssignment(AlonsoMoraVehicle vehicle, Collection<AlonsoMoraRequest> requests,
			List<AlonsoMoraStop> stops, double now, boolean isComplete, RouteTracker tracker) {
		Optional<StayTask> nextOperationalTask = getNextOperationalTask(vehicle);

		if (nextOperationalTask.isPresent()) {
			StayTask operationalTask = nextOperationalTask.get();

			double lastStopDepartureTime = tracker.getDepartureTime(stops.size() - 1);
			double requiredArrivalTime = operationalTask.getBeginTime();

			if (!isComplete) {
				// Here we may have a delay already even without driving to the facility

				return Math.max(0.0, lastStopDepartureTime - requiredArrivalTime);
			} else {
				// We add the route to the operational facility

				Link lastStopLink = stops.get(stops.size() - 1).getLink();
				double travelTime = travelTimeEstimator.estimateTravelTime(lastStopLink, operationalTask.getLink(),
						lastStopDepartureTime, requiredArrivalTime);
				double estimatedArrivalTime = lastStopDepartureTime + travelTime;

				return Math.max(0.0, estimatedArrivalTime - requiredArrivalTime);
			}
		}

		return 0.0;
	}

	@Override
	public boolean checkRelocation(AlonsoMoraVehicle vehicle, Link destination, double now) {
		Task currentTask = vehicle.getVehicle().getSchedule().getCurrentTask();

		if (currentTask instanceof OperationalStop) {
			return false;
		}

		if (getNextOperationalTask(vehicle) != null) {
			return false;
		}

		return true;
	}

	/**
	 * Find the next operational task in the schedule.
	 */
	private Optional<StayTask> getNextOperationalTask(AlonsoMoraVehicle vehicle) {
		Schedule schedule = vehicle.getVehicle().getSchedule();

		for (int i = schedule.getCurrentTask().getTaskIdx() + 1; i < schedule.getTaskCount(); i++) {
			Task task = schedule.getTasks().get(i);

			if (task instanceof OperationalStop) {
				return Optional.of((StayTask) task);
			}
		}

		return Optional.empty();
	}
}
