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
	public Pickup initEndTimeForPickup(DvrpVehicle vehicle, double beginTime, DrtRequest request) {
		// stop ends when pickup time has elapsed
		double endTime = beginTime + stopDurationProvider.calcPickupDuration(vehicle, request);
		return new Pickup(endTime, endTime);
	}

	@Override
	public Pickup updateEndTimeForPickup(DvrpVehicle vehicle, DrtStopTask stop, double insertionTime,
			DrtRequest request) {
		// an additional stop may extend the stop duration
		double beginTime = Math.max(stop.getBeginTime(), insertionTime);
		double pickupTime = beginTime + stopDurationProvider.calcPickupDuration(vehicle, request);
		double endTime = Math.max(stop.getEndTime(), pickupTime);

		return new Pickup(endTime, pickupTime);
	}

	@Override
	public Dropoff initEndTimeForDropoff(DvrpVehicle vehicle, double beginTime, DrtRequest request) {
		// stop ends after dropoff duration
		double endTime = beginTime + stopDurationProvider.calcDropoffDuration(vehicle, request);
		return new Dropoff(endTime, endTime);
	}

	@Override
	public Dropoff updateEndTimeForDropoff(DvrpVehicle vehicle, DrtStopTask stop, double insertionTime,
			DrtRequest request) {
		// update end time
		double initialDuration = stop.getEndTime() - stop.getBeginTime();
		double endTime = Math.max(stop.getEndTime(), initialDuration + insertionTime);

		// add the dropoff
		double dropoffTime = insertionTime + stopDurationProvider.calcDropoffDuration(vehicle, request);
		endTime = Math.max(endTime, dropoffTime);

		return new Dropoff(endTime, dropoffTime);
	}

	@Override
	public double shiftEndTime(DvrpVehicle vehicle, DrtStopTask stop, double beginTime) {
		// shifting the stop does not change the duration
		return beginTime + (stop.getEndTime() - stop.getBeginTime());
	}
}
