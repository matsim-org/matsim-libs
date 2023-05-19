package ch.sbb.matsim.contrib.railsim.qsimengine;

import java.util.ArrayList;
import java.util.List;

/**
 * Reserve tracks latest as possible.
 */
public class ReservationAtLatestChance implements TrackReservationStrategy {
	@Override
	public double nextUpdate(RailLink currentLink, TrainState state) {

		// time needed for full stop
		double stopTime = state.allowedMaxSpeed / state.train.deceleration();

		assert stopTime > 0 : "Stop time can not be negative";

		// Distance for full stop
		double safetyDist = RailsimCalc.calcTraveledDist(state.allowedMaxSpeed, stopTime, -state.train.deceleration());

		double dist = -state.headPosition - safetyDist;
		int idx = state.routeIdx;
		do {
			RailLink nextLink = state.route.get(idx++);
			dist += nextLink.length;

			if (nextLink.isReserved(state.driver))
				continue;

			return dist;
		} while (dist <= safetyDist && idx < state.route.size());

		// No need to reserve yet
		return Double.POSITIVE_INFINITY;
	}

	@Override
	public List<RailLink> retrieveLinksToReserve(double time, UpdateEvent.Type type, int idx, TrainState state) {

		List<RailLink> result = new ArrayList<>();

		double assumedSpeed = state.train.maxVelocity();

		double stopTime = assumedSpeed / state.train.deceleration();
		// safety distance
		double safety = RailsimCalc.calcTraveledDist(assumedSpeed, stopTime, -state.train.deceleration()) + state.headPosition;

		double reserved = 0;

		do {
			RailLink nextLink = state.route.get(idx++);
			result.add(nextLink);
			reserved += nextLink.length;

		} while (reserved < safety && idx < state.route.size());

		return result;
	}
}
