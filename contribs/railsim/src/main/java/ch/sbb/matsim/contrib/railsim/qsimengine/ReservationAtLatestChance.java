package ch.sbb.matsim.contrib.railsim.qsimengine;

import ch.sbb.matsim.contrib.railsim.events.RailsimLinkStateChangeEvent;

import javax.measure.quantity.Pressure;
import java.util.ArrayList;
import java.util.List;

/**
 * Reserve tracks latest as possible.
 */
public class ReservationAtLatestChance implements TrackReservationStrategy {
	@Override
	public double nextUpdate(RailLink currentLink, TrainState state) {

		// TODO: only having the next link reserved might not be sufficient
		if (state.route.get(state.routeIdx).isReserved(state.driver))
			return Double.POSITIVE_INFINITY;

		// time needed for full stop
		double stopTime = state.allowedMaxSpeed / state.train.deceleration();

		assert stopTime > 0 : "Stop time can not be negative";

		// Distance for full stop
		double safetyDist = RailsimCalc.calcTraveledDist(state.allowedMaxSpeed, stopTime, -state.train.deceleration());

		return currentLink.length - safetyDist - state.headPosition;
	}

	@Override
	public List<RailLink> retrieveLinksToReserve(double time, int idx, TrainState state) {

		List<RailLink> result = new ArrayList<>();

		double stopTime = state.targetSpeed / state.train.deceleration();
		// safety distance
		double safety = RailsimCalc.calcTraveledDist(state.targetSpeed, stopTime, -state.train.deceleration());

		double reserved = 0;

		do {
			RailLink nextLink = state.route.get(idx++);
			result.add(nextLink);
			reserved += nextLink.length;

		} while (reserved < safety && idx < state.route.size());

		return result;
	}
}
