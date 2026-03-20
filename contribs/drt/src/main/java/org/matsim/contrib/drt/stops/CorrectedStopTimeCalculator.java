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
	public Pickup initEndTimeForPickup(DvrpVehicle vehicle, double beginTime, DrtRequest request) {
		// pickup at the end of the stop
		return new Pickup(beginTime + stopDuration, beginTime + stopDuration);
	}

	@Override
	public Pickup updateEndTimeForPickup(DvrpVehicle vehicle, DrtStopTask stop, double insertionTime,
			DrtRequest request) {
		// an additional stop may extend the stop duration
		double beginTime = Math.max(stop.getBeginTime(), insertionTime);
		double endTime = beginTime + stopDuration;
		return new Pickup(endTime, endTime);
	}

	@Override
	public Dropoff initEndTimeForDropoff(DvrpVehicle vehicle, double beginTime, DrtRequest request) {
		// stop ends after stopDuration has elapsed (dropoff happens at beginning)
		double endTime = beginTime + stopDuration;
		return new Dropoff(endTime, beginTime);
	}

	@Override
	public Dropoff updateEndTimeForDropoff(DvrpVehicle vehicle, DrtStopTask stop, double insertionTime,
			DrtRequest request) {
		// adding a dropoff does not change the end time, but the whole task may be
		// shifted due to a previous pickup insertion
		double beginTime = Math.max(insertionTime, stop.getBeginTime());
		double endTime = beginTime + stopDuration;
		return new Dropoff(endTime, beginTime);
	}

	@Override
	public double shiftEndTime(DvrpVehicle vehicle, DrtStopTask stop, double beginTime) {
		// stop always has a fixed duration and ongoing tasks cannot be shifted (no need
		// to cover that case)
		return beginTime + stopDuration;
	}
}
