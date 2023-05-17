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

	/**
	 * Calc the distance deceleration needs to start and the target speed.
	 */
	static double calcDeccelDistanceAndSpeed(RailLink currentLink, UpdateEvent event) {

		// TODO: ignores acceleration that happens

		TrainState state = event.state;

		if (state.speed == 0)
			return Double.POSITIVE_INFINITY;

		double assumedSpeed = state.speed;

		// Lookahead window
		double window = RailsimCalc.calcTraveledDist(assumedSpeed, assumedSpeed / state.train.deceleration(),
			-state.train.deceleration()) + currentLink.length;

		// Distance to the next speed change point (link)
		double dist = currentLink.length - state.headPosition;

		double deccelDist = Double.POSITIVE_INFINITY;
		double speed = 0;

		for (int i = state.routeIdx; i < state.route.size(); i++) {

			RailLink link = state.route.get(i);
			double allowed;
			// Last track where train comes to halt
			if (i == state.route.size() - 1)
				allowed = 0;
			else {
				allowed = link.getAllowedFreespeed(state.driver);
			}

			if (allowed < assumedSpeed) {
				double timeDeccel = (assumedSpeed - allowed) / state.train.deceleration();
				double newDeccelDist = RailsimCalc.calcTraveledDist(assumedSpeed, timeDeccel, -state.train.deceleration());

				if ((dist - newDeccelDist) < deccelDist) {
					deccelDist = dist - newDeccelDist;
					speed = allowed;
				}
			}

			dist += link.length;

			// don't need to look further than distance needed for full stop
			if (dist >= window)
				break;
		}

		event.newSpeed = speed;
		return deccelDist;
	}
}
