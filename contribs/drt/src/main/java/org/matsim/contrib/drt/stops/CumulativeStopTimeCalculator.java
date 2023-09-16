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
	public double initEndTimeForPickup(DvrpVehicle vehicle, double beginTime, DrtRequest request) {
		// pickup takes the indicated duration
		return beginTime + provider.calcPickupDuration(vehicle, request);
	}

	@Override
	public double updateEndTimeForPickup(DvrpVehicle vehicle, DrtStopTask stop, double insertionTime,
			DrtRequest request) {
		// pickup extends the stop duration
		return stop.getEndTime() + provider.calcPickupDuration(vehicle, request);
	}

	@Override
	public double initEndTimeForDropoff(DvrpVehicle vehicle, double beginTime, DrtRequest request) {
		// dropoff takes the indicated duration
		return beginTime + provider.calcDropoffDuration(vehicle, request);
	}

	@Override
	public double updateEndTimeForDropoff(DvrpVehicle vehicle, DrtStopTask stop, double insertionTime,
			DrtRequest request) {
		double stopDuration = provider.calcDropoffDuration(vehicle, request);

		if (insertionTime > stop.getBeginTime()) {
			// insertion has shifted the stop
			double initialDuration = stop.getEndTime() - stop.getBeginTime();
			return insertionTime + initialDuration + stopDuration;
		} else {
			// no shift, we simply add the new duration
			return stop.getEndTime() + stopDuration;
		}
	}

	@Override
	public double shiftEndTime(DvrpVehicle vehicle, DrtStopTask stop, double beginTime) {
		// shifting the stop does not change the duration
		return beginTime + (stop.getEndTime() - stop.getBeginTime());
	}
}
