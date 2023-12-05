package ch.sbb.matsim.contrib.railsim.qsimengine.deadlocks;

import ch.sbb.matsim.contrib.railsim.qsimengine.TrainPosition;
import ch.sbb.matsim.contrib.railsim.qsimengine.resources.RailResource;
import ch.sbb.matsim.contrib.railsim.qsimengine.resources.RailResourceManager;
import jakarta.inject.Inject;

/**
 * TODO
 */
public class SimpleDeadlockAvoidance implements DeadlockAvoidance {

	@Inject
	public SimpleDeadlockAvoidance(RailResourceManager res) {
	}

	@Override
	public boolean check(double time, RailResource resource, TrainPosition position) {
		return false;
	}

}
