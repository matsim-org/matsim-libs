package ch.sbb.matsim.contrib.railsim.qsimengine.deadlocks;

import ch.sbb.matsim.contrib.railsim.qsimengine.TrainPosition;
import ch.sbb.matsim.contrib.railsim.qsimengine.resources.RailLink;
import ch.sbb.matsim.contrib.railsim.qsimengine.resources.RailResource;
import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;

import javax.annotation.Nullable;
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
	 * Check if reserving this link may produce a deadlock.
	 * @return true if the link can be reserved, false otherwise.
	 */
	boolean checkLink(double time, RailLink link, TrainPosition position);

	/**
	 * Called when a resource was released.
	 */
	void onRelease(double time, RailResource resource, MobsimDriverAgent driver);

	/**
	 * Called when a link was released.
	 */
	default void onReleaseLink(double time, RailLink link, MobsimDriverAgent driver) {
	}

}
