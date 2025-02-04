package org.matsim.contrib.drt.stops;

import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;

/**
 * Attention: This is a simple stop time calculator with quick calculations for
 * *immediate* requests. The calculations in case of prebooking become more
 * complicated, especially in the case of updating end times when dropoffs are
 * added: Because the previously inserted pickup generates a time penalty in
 * InsertionGenerator, not only the newly added dropoff needs to be taken into
 * account here, but also the general shifting of the task. Then we need to
 * evaluate the earliestDepartureTime of all assigned pickups. For that reason,
 * we have a separate implementation that is also valid for prebooking requests,
 * but we keep this one here as it requires less calculations.
 */
public class DefaultStopTimeCalculator implements StopTimeCalculator {
	private final double stopDuration;

	public DefaultStopTimeCalculator(double stopDuration) {
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
		// an additional stop does not change the end time
		return stop.getEndTime();
	}

	@Override
	public double initEndTimeForDropoff(DvrpVehicle vehicle, double beginTime, DrtRequest request) {
		// stop ends after stopDuration has elapsed (dropoff happens at beginning)
		return beginTime + stopDuration;
	}

	@Override
	public double updateEndTimeForDropoff(DvrpVehicle vehicle, DrtStopTask stop, double insertionTime,
			DrtRequest request) {
		// adding a dropoff does not change the end time, but the whole task may be
		// shifted due to a previous pickup insertion
		return Math.max(stop.getEndTime(), insertionTime + stopDuration);
	}

	@Override
	public double shiftEndTime(DvrpVehicle vehicle, DrtStopTask stop, double beginTime) {
		// When shifting, we make sure the new duration is the same as the previous one
		double stopDuration  = stop.getEndTime() - stop.getBeginTime();
		return beginTime + stopDuration;
	}
}
