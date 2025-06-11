package org.matsim.contrib.drt.stops;

import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;

public class MinimumStopDurationAdapter implements StopTimeCalculator {
	private final StopTimeCalculator delegate;
	private final double minimumStopDuration;

	public MinimumStopDurationAdapter(StopTimeCalculator delegate, double minimumStopDuration) {
		this.delegate = delegate;
		this.minimumStopDuration = minimumStopDuration;
	}

	@Override
	public Pickup initEndTimeForPickup(DvrpVehicle vehicle, double beginTime, DrtRequest request) {
		Pickup delegate = this.delegate.initEndTimeForPickup(vehicle, beginTime, request);
		double endTime = Math.max(beginTime + minimumStopDuration, delegate.endTime());

		return new Pickup(endTime, delegate.pickupTime());
	}

	@Override
	public Pickup updateEndTimeForPickup(DvrpVehicle vehicle, DrtStopTask stop, double insertionTime,
			DrtRequest request) {
		Pickup delegate = this.delegate.updateEndTimeForPickup(vehicle, stop, insertionTime, request);
		double endTime = Math.max(stop.getBeginTime() + minimumStopDuration, delegate.endTime());

		return new Pickup(endTime, delegate.pickupTime());
	}

	@Override
	public Dropoff initEndTimeForDropoff(DvrpVehicle vehicle, double beginTime, DrtRequest request) {
		Dropoff delegate = this.delegate.initEndTimeForDropoff(vehicle, beginTime, request);
		double endTime = Math.max(beginTime + minimumStopDuration, delegate.endTime());

		return new Dropoff(endTime, delegate.dropoffTime());
	}

	@Override
	public Dropoff updateEndTimeForDropoff(DvrpVehicle vehicle, DrtStopTask stop, double insertionTime,
			DrtRequest request) {
		Dropoff delegate = this.delegate.updateEndTimeForDropoff(vehicle, stop, insertionTime, request);
		double endTime = Math.max(stop.getBeginTime() + minimumStopDuration, delegate.endTime());
		
		return new Dropoff(endTime, delegate.dropoffTime());
	}

	@Override
	public double shiftEndTime(DvrpVehicle vehicle, DrtStopTask stop, double beginTime) {
		return Math.max(beginTime + minimumStopDuration, delegate.shiftEndTime(vehicle, stop, beginTime));
	}
}
