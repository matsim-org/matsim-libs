package ch.sbb.matsim.contrib.railsim.qsimengine.deadlocks;

import ch.sbb.matsim.contrib.railsim.qsimengine.TrainPosition;
import ch.sbb.matsim.contrib.railsim.qsimengine.resources.RailLink;
import ch.sbb.matsim.contrib.railsim.qsimengine.resources.RailResource;
import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;

import javax.annotation.Nullable;
import java.util.List;
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
	 * Called when a resource was reserved.
	 */
	void onReserve(double time, RailResource resource, TrainPosition position);

	/**
	 * Check if reserving a segment of links may produce a deadlock.
	 * @return the first link that should not be reserved in order to avoid deadlock, or null if no deadlock is detected.
	 */
	@Nullable
	RailLink check(double time, List<RailLink> segment, TrainPosition position);

	/**
	 * Called when a resource was released.
	 */
	void onRelease(double time, RailResource resource, MobsimDriverAgent driver);
}
