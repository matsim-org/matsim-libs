package ch.sbb.matsim.contrib.railsim.qsimengine.disposition;

import ch.sbb.matsim.contrib.railsim.qsimengine.TrainPosition;
import ch.sbb.matsim.contrib.railsim.qsimengine.resources.RailLink;

/**
 * Speed profile that reduces the speed if ahead of schedule.
 */
public class AdaptiveSpeedProfile implements SpeedProfile {

	/**
	 * Buffer time always subtracted from the planned arrival time
	 */
	static final double BUFFER = 3 * 60;

	@Override
	public double getTargetSpeed(double time, TrainPosition position, PlannedArrival nextArrival) {

		if (nextArrival == PlannedArrival.UNDEFINED || nextArrival.route().isEmpty())
			return Double.POSITIVE_INFINITY;

		double arrivalTime = nextArrival.time() - BUFFER;

		double totalLength = 0;
		double maxSpeed = 0;
		for (RailLink link : nextArrival.route()) {
			double allowedSpeed = link.getAllowedFreespeed(position.getDriver());

			totalLength += link.getLength();
			maxSpeed += allowedSpeed * link.getLength();
		}

		// Normalize the speed to the total length of the route
		maxSpeed /= totalLength;

		// Calculate remaining time
		double remainingTime = arrivalTime - time;

		// If we have no time left or negative time, return maximum speed
		if (remainingTime <= 0 || totalLength <= 0) {
			return Double.POSITIVE_INFINITY;
		}

		// Calculate required speed to reach destination in remaining time
		double requiredSpeed = totalLength / remainingTime;

		double f = requiredSpeed / maxSpeed;

		double target = f * nextArrival.route().getFirst().getAllowedFreespeed(position.getDriver());

		// Return the required speed, but at least 10 m/s
		return Math.max(10, target);
	}

}
