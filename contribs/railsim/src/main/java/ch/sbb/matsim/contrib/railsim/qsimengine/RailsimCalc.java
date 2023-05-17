package ch.sbb.matsim.contrib.railsim.qsimengine;

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
	 * Inverse of {@link #calcTraveledDist(double, double, double)}, solves for distance.
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

			double deccelTime = -(state.speed - state.targetSpeed) / state.acceleration;

			// max distance that can be reached
			double max = calcTraveledDist(state.speed, deccelTime, state.acceleration);

			if (dist < max) {
				return solveTraveledDist(state.speed, dist, state.acceleration);
			} else
				return deccelTime;
		}
	}
}
