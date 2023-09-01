package org.matsim.contrib.drt.stops;

import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;

/**
 * This calculator is a more flexible implementation of
 * CorrectedStopTimeCalculator. Instead of imposing a fixed stop duration, it
 * makes use of a StopDurationProvider. Adding a pickup or a dropoff may extend
 * the stop duration to accomodate for a request that needs more time to enter
 * the vehicle than others or induce a longer time after dropoff.
 */
public class ParallelStopTimeCalculator implements StopTimeCalculator {
	private final PassengerStopDurationProvider stopDurationProvider;

	public ParallelStopTimeCalculator(PassengerStopDurationProvider stopDurationProvider) {
		this.stopDurationProvider = stopDurationProvider;
	}

	@Override
	public double initEndTimeForPickup(DvrpVehicle vehicle, double beginTime, DrtRequest request) {
		// stop ends when pickup time has elapsed
		return beginTime + stopDurationProvider.calcPickupDuration(vehicle, request);
	}

	@Override
	public double updateEndTimeForPickup(DvrpVehicle vehicle, DrtStopTask stop, double insertionTime,
			DrtRequest request) {
		// an additional stop may extend the stop duration
		return Math.max(stop.getEndTime(), insertionTime + stopDurationProvider.calcPickupDuration(vehicle, request));
	}

	@Override
	public double initEndTimeForDropoff(DvrpVehicle vehicle, double beginTime, DrtRequest request) {
		// stop ends after stopDuration has elapsed (dropoff happens at beginning)
		return beginTime + stopDurationProvider.calcDropoffDuration(vehicle, request);
	}

	@Override
	public double updateEndTimeForDropoff(DvrpVehicle vehicle, DrtStopTask stop, double insertionTime,
			DrtRequest request) {
		// adding a dropoff may extend the stop duration
		return Math.max(stop.getEndTime(), insertionTime + stopDurationProvider.calcDropoffDuration(vehicle, request));
	}

	@Override
	public double shiftEndTime(DvrpVehicle vehicle, DrtStopTask stop, double beginTime) {
		// shifting the stop does not change the duration
		return beginTime + (stop.getEndTime() - stop.getBeginTime());
	}
}
