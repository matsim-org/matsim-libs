package ch.sbb.matsim.contrib.railsim.qsimengine.deadlocks;

import ch.sbb.matsim.contrib.railsim.qsimengine.TrainPosition;
import ch.sbb.matsim.contrib.railsim.qsimengine.resources.RailResource;
import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;

import java.util.Map;

/**
 * Interface for deadlock prevention strategies.
 * Strategies should aim to prevent deadlocks, but is not required to guarantee it.
 */
public interface DeadlockAvoidance {

	/**
	 * Let the strategy known about the resources that are available.
	 */
	default void initResources(Map<Id<RailResource>, RailResource> resources) {
	}

	/**
	 * Check if this resource is allowed to be reserved by the given train.
	 */
	boolean check(double time, RailResource resource, TrainPosition position);

	/**
	 * Called when a resource was reserved.
	 */
	void onReserve(double time, RailResource resource, TrainPosition position);

	/**
	 * Called when a resource was released.
	 */
	void onRelease(double time, RailResource resource, MobsimDriverAgent driver);
}
