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
	public double initEndTimeForPickup(DvrpVehicle vehicle, double beginTime, DrtRequest request) {
		return Math.max(beginTime + minimumStopDuration, delegate.initEndTimeForPickup(vehicle, beginTime, request));
	}

	@Override
	public double updateEndTimeForPickup(DvrpVehicle vehicle, DrtStopTask stop, double insertionTime,
			DrtRequest request) {
		return Math.max(stop.getBeginTime() + minimumStopDuration,
				delegate.updateEndTimeForPickup(vehicle, stop, insertionTime, request));
	}

	@Override
	public double initEndTimeForDropoff(DvrpVehicle vehicle, double beginTime, DrtRequest request) {
		return Math.max(beginTime + minimumStopDuration, delegate.initEndTimeForDropoff(vehicle, beginTime, request));
	}

	@Override
	public double updateEndTimeForDropoff(DvrpVehicle vehicle, DrtStopTask stop, double insertionTime,
			DrtRequest request) {
		return Math.max(insertionTime + minimumStopDuration,
				delegate.updateEndTimeForDropoff(vehicle, stop, insertionTime, request));
	}

	@Override
	public double shiftEndTime(DvrpVehicle vehicle, DrtStopTask stop, double beginTime) {
		return Math.max(beginTime + minimumStopDuration, delegate.shiftEndTime(vehicle, stop, beginTime));
	}
}
