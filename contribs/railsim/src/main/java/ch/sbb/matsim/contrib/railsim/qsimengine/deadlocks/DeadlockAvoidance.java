package ch.sbb.matsim.contrib.railsim.qsimengine.deadlocks;

import ch.sbb.matsim.contrib.railsim.qsimengine.TrainPosition;
import ch.sbb.matsim.contrib.railsim.qsimengine.resources.RailResource;

/**
 * Interface for deadlock prevention strategies.
 * Strategies should aim to prevent deadlocks, but is not required to guarantee it.
 */
public interface DeadlockAvoidance {

	// TODO: needs to keep track of the trains that are currently using a resource

	/**
	 * Check if this resource is allowed to be reserved by the given train.
	 */
	boolean check(double time, RailResource resource, TrainPosition position);

}
