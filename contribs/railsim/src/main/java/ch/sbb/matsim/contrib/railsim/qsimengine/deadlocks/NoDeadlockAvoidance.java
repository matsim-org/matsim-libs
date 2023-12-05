package ch.sbb.matsim.contrib.railsim.qsimengine.deadlocks;

import ch.sbb.matsim.contrib.railsim.qsimengine.TrainPosition;
import ch.sbb.matsim.contrib.railsim.qsimengine.resources.RailResource;

/**
 * Does not prevent deadlocks and permits every request.
 */
public class NoDeadlockAvoidance implements DeadlockAvoidance {

	@Override
	public boolean check(double time, RailResource resource, TrainPosition position) {
		return true;
	}
}
