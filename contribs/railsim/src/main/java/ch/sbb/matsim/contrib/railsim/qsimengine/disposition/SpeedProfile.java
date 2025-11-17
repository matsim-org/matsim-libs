package ch.sbb.matsim.contrib.railsim.qsimengine.disposition;

import ch.sbb.matsim.contrib.railsim.qsimengine.RailsimTransitDriverAgent;
import ch.sbb.matsim.contrib.railsim.qsimengine.TrainPosition;
import ch.sbb.matsim.contrib.railsim.qsimengine.resources.RailLink;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.List;

/**
 * Interface for calculating the target speed of a train, incorporating planned arrivals and current delay.
 */
public interface SpeedProfile {

	/**
	 * Calculate the next arrival of a train, depending on its current position and time.
	 */
	default PlannedArrival getNextArrival(double time, TrainPosition position) {

		RailsimTransitDriverAgent pt = position.getPt();
		// Only transit schedules are supported
		if (pt == null) {
			return PlannedArrival.UNDEFINED;
		}

		TransitStopFacility stop = position.getNextStop();
		if (stop == null) {
			return PlannedArrival.UNDEFINED;
		}

		TransitRoute tr = pt.getTransitRoute();

		int stopIdx = pt.getCurrentStopIndex();

		TransitRouteStop routeStop = tr.getStops().get(stopIdx);
		OptionalTime arrival = routeStop.getArrivalOffset();

		if (arrival.isUndefined())
			return PlannedArrival.UNDEFINED;

		Departure departure = pt.getDeparture();

		double nextArrivalTime = departure.getDepartureTime() + arrival.seconds();
		return new PlannedArrival(nextArrivalTime, position.getRouteUntilNextStop());
	}

	/**
	 * Calculate the target speed of a train based on its current position, time, and planned arrival.
	 * The method does *not* need to consider stopping, speed limits, etc. This will be done upstream in the engine.
	 *
	 * @param time        now
	 * @param position    current position of the train
	 * @param nextArrival next planned arrival of the train
	 * @return the target speed in m/s, or Double.POSITIVE_INFINITY if train should drive at full speed.
	 */
	double getTargetSpeed(double time, TrainPosition position, PlannedArrival nextArrival);

}
