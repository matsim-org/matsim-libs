package org.matsim.contrib.drt.stops;

import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;

public class CumulativeStopTimeCalculator implements StopTimeCalculator {
	private final PassengerStopDurationProvider provider;

	public CumulativeStopTimeCalculator(PassengerStopDurationProvider provider) {
		this.provider = provider;
	}

	@Override
	public Pickup initEndTimeForPickup(DvrpVehicle vehicle, double beginTime, DrtRequest request) {
		// pickup takes the indicated duration
		double endTime = beginTime + provider.calcPickupDuration(vehicle, request);
		return new Pickup(endTime, endTime);
	}

	@Override
	public Pickup updateEndTimeForPickup(DvrpVehicle vehicle, DrtStopTask stop, double insertionTime,
			DrtRequest request) {
		// pickup extends the stop duration
		insertionTime = Math.max(stop.getEndTime(), insertionTime);
		double endTime = insertionTime + provider.calcPickupDuration(vehicle, request);
		return new Pickup(endTime, endTime);
	}

	@Override
	public Dropoff initEndTimeForDropoff(DvrpVehicle vehicle, double beginTime, DrtRequest request) {
		// dropoff takes the indicated duration
		double endTime = beginTime + provider.calcDropoffDuration(vehicle, request);
		return new Dropoff(endTime, endTime);
	}

	@Override
	public Dropoff updateEndTimeForDropoff(DvrpVehicle vehicle, DrtStopTask stop, double insertionTime,
			DrtRequest request) {
		// stop may have been shifted
		double initialDuration = stop.getEndTime() - stop.getBeginTime();
		double endTime = Math.max(stop.getEndTime(), insertionTime + initialDuration);

		// add dropoff
		double stopDuration = provider.calcDropoffDuration(vehicle, request);
		endTime += stopDuration;

		return new Dropoff(endTime, endTime);
	}

	@Override
	public double shiftEndTime(DvrpVehicle vehicle, DrtStopTask stop, double beginTime) {
		// shifting the stop does not change the duration
		return beginTime + (stop.getEndTime() - stop.getBeginTime());
	}
}
