package org.matsim.contrib.drt.stops;

import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;

public class PrebookingStopTimeCalculator implements StopTimeCalculator {
	private final PassengerStopDurationProvider provider;

	public PrebookingStopTimeCalculator(PassengerStopDurationProvider provider) {
		this.provider = provider;
	}

	@Override
	public double initEndTimeForPickup(DvrpVehicle vehicle, double beginTime, DrtRequest request) {
		double stopDuration = provider.calcPickupDuration(vehicle, request);

		// pickup duration starts either at the earliest departure time or when stop
		// beginning is planned
		return Math.max(request.getEarliestStartTime(), beginTime) + stopDuration;
	}

	@Override
	public double updateEndTimeForPickup(DvrpVehicle vehicle, DrtStopTask stop, double insertionTime,
			DrtRequest request) {
		double stopDuration = provider.calcPickupDuration(vehicle, request);

		// the pickup starts at the latest of the following events
		// - the begin time of the existing stop (immediate request)
		// - the given insertionTime (immediate request merged to an ongoing stop)
		// - the requests's earliest departure time (prebooking)

		double earliestStartTime = stop.getBeginTime();
		earliestStartTime = Math.max(earliestStartTime, insertionTime);
		earliestStartTime = Math.max(earliestStartTime, request.getEarliestStartTime());

		// from that point on, we add the stop duration, and we never shrink the stop to
		// account for other assigned requests
		return Math.max(stop.getEndTime(), earliestStartTime + stopDuration);
	}

	@Override
	public double initEndTimeForDropoff(DvrpVehicle vehicle, double beginTime, DrtRequest request) {
		double stopDuration = provider.calcDropoffDuration(vehicle, request);

		// dropoff duration starts at the planned stop begin time
		return beginTime + stopDuration;
	}

	@Override
	public double updateEndTimeForDropoff(DvrpVehicle vehicle, DrtStopTask stop, double insertionTime,
			DrtRequest request) {
		double stopDuration = provider.calcDropoffDuration(vehicle, request);

		// first, check when we can start with the dropoff
		double earliestStartTime = stop.getBeginTime();
		earliestStartTime = Math.max(earliestStartTime, insertionTime);
		
		// second, we add the dropoff
		return Math.max(stop.getEndTime(), earliestStartTime + stopDuration);
	}

	/**
	 * Calculate the expected end time of a stop when begin time is updated
	 */
	@Override
	public double shiftEndTime(DvrpVehicle vehicle, DrtStopTask stop, double beginTime) {
		double latestPickupEndTime = stop.getPickupRequests().values().stream().mapToDouble(request -> {
			double departureTime = Math.max(beginTime, request.getEarliestStartTime());
			return departureTime + provider.calcPickupDuration(vehicle, request.getRequest());
		}).max().orElse(beginTime);

		double latestDropoffEndTime = stop.getDropoffRequests().values().stream().mapToDouble(request -> {
			return beginTime + provider.calcDropoffDuration(vehicle, request.getRequest());
		}).max().orElse(beginTime);

		return Math.max(latestPickupEndTime, latestDropoffEndTime);
	}
}
