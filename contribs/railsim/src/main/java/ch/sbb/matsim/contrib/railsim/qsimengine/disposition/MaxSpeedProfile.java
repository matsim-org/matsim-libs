package ch.sbb.matsim.contrib.railsim.qsimengine.disposition;

import ch.sbb.matsim.contrib.railsim.qsimengine.TrainPosition;

/**
 * Always accelerate to the permissible max speed.
 */
public class MaxSpeedProfile implements SpeedProfile {
	@Override
	public PlannedArrival getNextArrival(double time, TrainPosition position) {
		return PlannedArrival.UNDEFINED;
	}

	@Override
	public double getTargetSpeed(double time, TrainPosition position, PlannedArrival nextArrival) {
		return Double.POSITIVE_INFINITY;
	}
}
