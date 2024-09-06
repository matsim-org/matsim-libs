package org.matsim.contrib.drt.stops;

import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;

/**
 * This calculator extend the simple logic of DefaultStopTimeCalculator by
 * allowing tasks to be extended. This happens when an ongoing task is assigned
 * a new pickup.
 */
public class CorrectedStopTimeCalculator implements StopTimeCalculator {
	private final double stopDuration;

	public CorrectedStopTimeCalculator(double stopDuration) {
		this.stopDuration = stopDuration;
	}

	@Override
	public double initEndTimeForPickup(DvrpVehicle vehicle, double beginTime, DrtRequest request) {
		// stop ends when pickup time has elapsed
		return beginTime + stopDuration;
	}

	@Override
	public double updateEndTimeForPickup(DvrpVehicle vehicle, DrtStopTask stop, double insertionTime,
			DrtRequest request) {
		// an additional stop may extend the stop duration
		return Math.max(stop.getEndTime(), insertionTime + stopDuration);
	}

	@Override
	public double initEndTimeForDropoff(DvrpVehicle vehicle, double beginTime, DrtRequest request) {
		// stop ends after stopDuration has elapsed (dropoff happens at beginning)
		return beginTime + stopDuration;
	}

	@Override
	public double updateEndTimeForDropoff(DvrpVehicle vehicle, DrtStopTask stop, double insertionTime,
			DrtRequest request) {
		// adding a dropoff may extend the stop duration
		return Math.max(stop.getEndTime(), insertionTime + stopDuration);
	}

	@Override
	public double shiftEndTime(DvrpVehicle vehicle, DrtStopTask stop, double beginTime) {
		// stop always has a fixed duration and ongoing tasks cannot be shifted (no need
		// to cover that case)
		return beginTime + stopDuration;
	}
}
