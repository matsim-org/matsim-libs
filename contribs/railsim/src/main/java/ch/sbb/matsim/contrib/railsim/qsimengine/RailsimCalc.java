package ch.sbb.matsim.contrib.railsim.qsimengine;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class holding static calculation methods related to state (updates).
 */
public class RailsimCalc {

	private RailsimCalc() {
	}

	/**
	 * Calculate traveled distance given initial speed and constant acceleration.
	 */
	static double calcTraveledDist(double speed, double elapsedTime, double acceleration) {
		return speed * elapsedTime + (elapsedTime * elapsedTime * acceleration / 2);
	}

	/**
	 * Inverse of {@link #calcTraveledDist(double, double, double)}, solves for distance, returns needed time.
	 */
	static double solveTraveledDist(double speed, double dist, double acceleration) {
		if (acceleration == 0)
			return dist / speed;

		return (Math.sqrt(2 * acceleration * dist + speed * speed) - speed) / acceleration;
	}

	/**
	 * Calculate time needed to advance distance {@code dist}. Depending on acceleration and max speed.
	 */
	static double calcRequiredTime(TrainState state, double dist) {

		if (FuzzyUtils.equals(dist, 0))
			return 0;

		if (state.acceleration == 0)
			return state.speed == 0 ? Double.POSITIVE_INFINITY : dist / state.speed;

		if (state.acceleration > 0) {

			double accelTime = (state.targetSpeed - state.speed) / state.acceleration;

			double d = calcTraveledDist(state.speed, accelTime, state.acceleration);

			// The required distance is reached during acceleration
			if (d > dist) {
				return solveTraveledDist(state.speed, dist, state.acceleration);

			} else
				// Time for accel plus remaining dist at max speed
				return accelTime + (dist - d) / state.targetSpeed;

		} else {

			double decelTime = -(state.speed - state.targetSpeed) / state.acceleration;

			// max distance that can be reached
			double max = calcTraveledDist(state.speed, decelTime, state.acceleration);

			if (FuzzyUtils.equals(dist, max)) {
				return decelTime;
			} else if (dist <= max) {
				return solveTraveledDist(state.speed, dist, state.acceleration);
			} else
				return Double.POSITIVE_INFINITY;
		}
	}

	/**
	 * Calculate the maximum speed that can be reached under the condition that speed must be reduced to {@code allowedSpeed}
	 * again after traveled {@code dist}.
	 */
	static SpeedTarget calcTargetSpeed(double dist, double acceleration, double deceleration,
									   double currentSpeed, double allowedSpeed, double finalSpeed) {

		// Calculation is simplified if target is the same
		if (FuzzyUtils.equals(allowedSpeed, finalSpeed)) {
			return new SpeedTarget(finalSpeed, Double.POSITIVE_INFINITY);
		}

		double timeDecel = (allowedSpeed - finalSpeed) / deceleration;
		double distDecel = calcTraveledDist(allowedSpeed, timeDecel, -deceleration);

		// No further acceleration needed
		if (FuzzyUtils.equals(currentSpeed, allowedSpeed)) {
			double decelDist = dist - distDecel;

			// Start to stop now
			if (FuzzyUtils.equals(decelDist, 0)) {
				return new SpeedTarget(finalSpeed, 0);
			}

			// Decelerate later
			return new SpeedTarget(allowedSpeed, decelDist);
		}


		assert FuzzyUtils.greaterEqualThan(allowedSpeed, currentSpeed) : "Current speed must be lower than allowed";
		assert FuzzyUtils.greaterEqualThan(allowedSpeed, finalSpeed) : "Final speed must be smaller than target";

		double timeAccel = (allowedSpeed - currentSpeed) / acceleration;
		double distAccel = calcTraveledDist(currentSpeed, timeAccel, acceleration);

		// there is enough distance to accelerate to the target speed
		if (FuzzyUtils.lessThan(distAccel + distDecel, dist)) {
			return new SpeedTarget(allowedSpeed, dist - distDecel);
		}

		double nom = 2 * acceleration * deceleration * dist
			+ acceleration * finalSpeed * finalSpeed
			+ deceleration * currentSpeed * currentSpeed;

		double v = Math.sqrt(nom / (acceleration + deceleration));

		timeDecel = (v - finalSpeed) / deceleration;
		distDecel = calcTraveledDist(v, timeDecel, -deceleration);

		return new SpeedTarget(v, dist - distDecel);
	}


	/**
	 * Calculate the deceleration needed to arrive at {@code targetSpeed} exactly after {@code dist}.
	 *
	 * @return negative acceleration, always a negative number.
	 */
	static double calcTargetDecel(double dist, double targetSpeed, double currentSpeed) {
		return -(currentSpeed * currentSpeed - targetSpeed * targetSpeed) / (2 * dist);
	}

	/**
	 * Calculate the maximum speed that can be achieved if trains want to stop after dist.
	 */
	static double calcTargetSpeedForStop(double dist, double acceleration, double deceleration, double currentSpeed) {

		double nom = 2 * acceleration * deceleration * dist
			+ deceleration * currentSpeed * currentSpeed;

		return Math.sqrt(nom / (acceleration + deceleration));
	}

	/**
	 * Calculate when the reservation function should be triggered.
	 * Should return {@link Double#POSITIVE_INFINITY} if this distance is far in the future and can be checked at later point.
	 *
	 * @param state       current train state
	 * @param currentLink the link where the train head is on
	 * @return travel distance after which reservations should be updated.
	 */
	public static double nextLinkReservation(TrainState state, RailLink currentLink) {

		double assumedSpeed = calcPossibleMaxSpeed(state);

		// time needed for full stop
		double stopTime = assumedSpeed / state.train.deceleration();

		assert stopTime > 0 : "Stop time can not be negative";

		// safety distance
		double safety = RailsimCalc.calcTraveledDist(assumedSpeed, stopTime, -state.train.deceleration());

		int idx = state.routeIdx;
		double dist = currentLink.length - state.headPosition;

		RailLink nextLink = null;
		// need to check beyond safety distance
		while (FuzzyUtils.lessEqualThan(dist, safety * 2) && idx < state.route.size()) {
			nextLink = state.route.get(idx++);

			if (!nextLink.isBlockedBy(state.driver))
				return dist - safety;

			// No reservation beyond pt stop
			if (state.isStop(nextLink.getLinkId()))
				break;

			dist += nextLink.length;
		}

		// No reservation needed after the end
		if (idx == state.route.size() || (nextLink != null && state.isStop(nextLink.getLinkId())))
			return Double.POSITIVE_INFINITY;

		return dist - safety;
	}

	/**
	 * Links that need to be blocked or otherwise stop needs to be initiated.
	 */
	public static List<RailLink> calcLinksToBlock(TrainState state, RailLink currentLink) {

		List<RailLink> result = new ArrayList<>();

		double assumedSpeed = calcPossibleMaxSpeed(state);
		double stopTime = assumedSpeed / state.train.deceleration();

		// safety distance
		double safety = RailsimCalc.calcTraveledDist(assumedSpeed, stopTime, -state.train.deceleration());

		int idx = state.routeIdx;

		// dist to next
		double dist = currentLink.length - state.headPosition;

		while (FuzzyUtils.lessEqualThan(dist, safety) && idx < state.route.size()) {
			RailLink nextLink = state.route.get(idx++);
			result.add(nextLink);
			dist += nextLink.length;

			// Beyond pt stop links don't need to be reserved
			if (state.isStop(nextLink.getLinkId()))
				break;
		}

		return result;
	}

	private static double calcPossibleMaxSpeed(TrainState state) {

		// TODO better would be the maximum that is possible over the next upcoming links
		// taking safety distance into account

		return state.train.maxVelocity();
	}

	record SpeedTarget(double targetSpeed, double decelDist) implements Comparable<SpeedTarget> {

		@Override
		public int compareTo(SpeedTarget o) {
			return Double.compare(decelDist, o.decelDist);
		}
	}

}
